from __future__ import annotations

import base64
import logging
import re
from urllib.parse import quote_plus, urljoin

from core.cache import cache
from core.config import settings
from core.errors import NotFoundError
from core.fetcher import ParsedPage, fetch_page
from models.anime import (
    AnimeCard, AnimeDetail, AnimeListResponse, EpisodeListItem,
    HomeBanner, HomeData, LatestEpisodeCard,
)
from models.common import PaginationMeta
from models.episode import DownloadLink, EpisodeStreamDetail, VideoServer
from models.schedule import ScheduleDay, ScheduleItem, ScheduleResponse
from scrapers.base import BaseSource
from scrapers.profiles import SiteProfile, merged_selectors, merged_urls

logger = logging.getLogger("scrapers.generic")

DAY_NAMES = {
    "senin": "Senin", "monday": "Senin", "mon": "Senin",
    "selasa": "Selasa", "tuesday": "Selasa", "tue": "Selasa",
    "rabu": "Rabu", "wednesday": "Rabu", "wed": "Rabu",
    "kamis": "Kamis", "thursday": "Kamis", "thu": "Kamis",
    "jumat": "Jumat", "jum'at": "Jumat", "friday": "Jumat", "fri": "Jumat",
    "sabtu": "Sabtu", "saturday": "Sabtu", "sat": "Sabtu",
    "minggu": "Minggu", "ahad": "Minggu", "sunday": "Minggu", "sun": "Minggu",
}
DAY_ORDER = ["Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"]

INFO_KEYS = {
    "japanese_title": ("japanese", "jepang", "judul alternatif", "synonym", "native"),
    "type": ("type", "tipe"),
    "status": ("status",),
    "score": ("skor", "score", "rating"),
    "total_episodes": ("total episode", "jumlah episode", "episodes", "episode"),
    "duration": ("durasi", "duration"),
    "season": ("season", "musim"),
    "studios": ("studio", "studios"),
    "released": ("rilis", "released", "tahun", "aired", "tayang"),
}


def slug_of(url: str) -> str:
    if not url:
        return ""
    path = url.split("?")[0].split("#")[0].rstrip("/")
    return path.split("/")[-1]


def episode_number(text: str) -> str:
    m = re.search(r"(?:episode|eps?)\s*(\d+(?:[.-]\d+)?)", text or "", re.I)
    if not m:
        nums = re.findall(r"\d+(?:\.\d+)?", text or "")
        return nums[-1] if nums else "1"
    return m.group(1).replace("-", ".")


def split_episode_title(title: str):
    m = re.match(r"^(.*?)\s*[-–:]?\s*(?:episode|eps?)\s*(\d+(?:[.-]\d+)?)", title or "", re.I)
    if m and m.group(1).strip():
        return m.group(1).strip(), m.group(2).replace("-", ".")
    return None, None


def detect_day(text: str):
    low = (text or "").lower()
    for key, label in DAY_NAMES.items():
        if len(key) > 3 and re.search(rf"\b{re.escape(key)}\b", low):
            return label
    return None


class GenericSource(BaseSource):
    """Scraper berbasis profil: satu kelas untuk semua situs (selector + URL dari profil)."""

    def __init__(self, profile: SiteProfile):
        self.profile = profile
        self.id = profile.id
        self.name = profile.name
        self.base = profile.base_url.rstrip("/")
        self.sel = merged_selectors(profile)
        self.urls = merged_urls(profile)

    # ---------- util ----------
    def _url(self, key: str, **kw) -> str:
        tpl = self.urls.get(key)
        if not tpl:
            raise NotFoundError(f"Sumber {self.id} tidak mendukung '{key}'")
        safe = {k: (quote_plus(str(v)) if k == "q" else str(v)) for k, v in kw.items()}
        path = tpl.format(**safe)
        if path.startswith("http"):
            return path
        return self.base + "/" + path.lstrip("/")

    def _abs(self, link: str) -> str:
        return urljoin(self.base + "/", link) if link else ""

    def _s(self, key: str) -> str:
        return self.sel.get(key, "")

    def _one(self, root, key: str):
        sel = self._s(key)
        root = root.soup if isinstance(root, ParsedPage) else root
        if not sel or root is None:
            return None
        try:
            return root.select_one(sel)
        except Exception as e:
            logger.warning(f"[{self.id}] selector '{key}' tidak valid: {e}")
            return None

    def _all(self, root, key: str):
        sel = self._s(key)
        root = root.soup if isinstance(root, ParsedPage) else root
        if not sel or root is None:
            return []
        try:
            return root.select(sel)
        except Exception as e:
            logger.warning(f"[{self.id}] selector '{key}' tidak valid: {e}")
            return []

    @staticmethod
    def _text(el) -> str:
        return el.get_text(" ", strip=True) if el else ""

    def _img(self, el) -> str:
        if not el:
            return ""
        for attr in ("src", "data-src", "data-lazy-src", "data-original"):
            v = el.get(attr)
            if v and not v.startswith("data:"):
                return self._abs(v)
        srcset = el.get("srcset") or el.get("data-srcset") or ""
        if srcset:
            return self._abs(srcset.split(",")[0].strip().split(" ")[0])
        return ""

    def _ck(self, name: str) -> str:
        return f"{self.id}:{name}"

    # ---------- kartu anime (list / search / ongoing / movies) ----------
    def _cards(self, doc, prefix: str) -> list[AnimeCard]:
        cards: list[AnimeCard] = []
        seen: set[str] = set()
        for el in self._all(doc, f"{prefix}_container"):
            title_el = self._one(el, f"{prefix}_title")
            link_el = self._one(el, f"{prefix}_url")
            img_el = self._one(el, f"{prefix}_thumb")
            href = link_el.get("href", "") if link_el else ""
            title = self._text(title_el)
            if not title and link_el:
                title = link_el.get("title", "") or self._text(link_el)
            if not title and img_el:
                title = img_el.get("alt", "")
            slug = slug_of(href)
            if not slug or not title or slug in seen:
                continue
            seen.add(slug)
            type_el = self._one(el, f"{prefix}_type")
            status_el = self._one(el, f"{prefix}_status")
            score_el = self._one(el, f"{prefix}_score")
            cards.append(AnimeCard(
                title=title,
                slug=slug,
                poster=self._img(img_el),
                type=self._text(type_el) or "TV",
                status=self._text(status_el) or "Ongoing",
                score=self._text(score_el) or "N/A",
                source=self.id,
            ))
        return cards

    async def _list_page(self, key: str, page: int, genre: str | None, cache_name: str) -> AnimeListResponse:
        ck = self._ck(f"{cache_name}_{page}_{genre or 'all'}")
        cached = cache.get(ck)
        if cached:
            return AnimeListResponse(**cached)
        if genre:
            k = "genre" if page > 1 else "genre_first"
            url = self._url(k, genre=genre, page=page)
        else:
            k = key if page > 1 else f"{key}_first"
            url = self._url(k if k in self.urls else key, page=page)
        doc = await fetch_page(url)
        items = self._cards(doc, "anime_list")
        has_next = self._one(doc, "pagination_next") is not None
        resp = AnimeListResponse(
            items=items,
            pagination=PaginationMeta(current_page=page, has_next=has_next),
            source=self.id,
        )
        cache.set(ck, resp.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
        return resp

    async def list(self, page: int = 1, genre: str | None = None) -> AnimeListResponse:
        return await self._list_page("list", page, genre, "list")

    async def ongoing(self, page: int = 1) -> AnimeListResponse:
        return await self._list_page("ongoing", page, None, "ongoing")

    async def movies(self, page: int = 1) -> AnimeListResponse:
        return await self._list_page("movies", page, None, "movies")

    # ---------- pencarian ----------
    async def search(self, query: str) -> list[AnimeCard]:
        q = (query or "").strip()
        if not q:
            return []
        ck = self._ck(f"search_{q.lower()}")
        cached = cache.get(ck)
        if cached:
            return [AnimeCard(**c) for c in cached]
        doc = await fetch_page(self._url("search", q=q))
        results = self._cards(doc, "search")
        cache.set(ck, [r.model_dump() for r in results], ttl=settings.CACHE_DEFAULT_TTL)
        return results

    # ---------- beranda ----------
    async def home(self) -> HomeData:
        ck = self._ck("home")
        cached = cache.get(ck)
        if cached:
            return HomeData(**cached)
        doc = await fetch_page(self._url("home"))

        banners: list[HomeBanner] = []
        for el in self._all(doc, "home_banner_container"):
            title_el = self._one(el, "home_banner_title")
            link_el = self._one(el, "home_banner_url")
            if not (title_el and link_el):
                continue
            banners.append(HomeBanner(
                title=self._text(title_el),
                slug=slug_of(link_el.get("href", "")),
                poster=self._img(self._one(el, "home_banner_thumb")),
                genres=[self._text(g) for g in self._all(el, "home_banner_genre") if self._text(g)],
            ))

        latest: list[LatestEpisodeCard] = []
        for el in self._all(doc, "latest_episodes_container")[:20]:
            title_el = self._one(el, "latest_episode_title")
            link_el = self._one(el, "latest_episode_url")
            if not (title_el or link_el):
                continue
            href = link_el.get("href", "") if link_el else ""
            raw_title = self._text(title_el)
            ep_slug = slug_of(href)
            ep_el = self._one(el, "latest_episode_number")
            ep_num = self._text(ep_el) or episode_number(raw_title)
            latest.append(LatestEpisodeCard(
                title=raw_title,
                episode_slug=ep_slug,
                anime_slug=ep_slug.rsplit("-episode-", 1)[0] if "-episode-" in ep_slug else None,
                poster=self._img(self._one(el, "latest_episode_thumb")),
                episode_number=ep_num or "Ep ?",
                release_time=self._text(self._one(el, "latest_episode_release_time")) or "Baru saja",
            ))

        popular: list[AnimeCard] = []
        for el in self._all(doc, "popular_container")[:15]:
            title_el = self._one(el, "popular_title")
            link_el = self._one(el, "popular_url")
            if not (title_el or link_el):
                continue
            popular.append(AnimeCard(
                title=self._text(title_el),
                slug=slug_of(link_el.get("href", "")) if link_el else "",
                poster=self._img(self._one(el, "popular_thumb")),
                score=self._text(self._one(el, "popular_rating")) or "N/A",
                status="Populer",
                source=self.id,
            ))

        data = HomeData(banners=banners, latest_episodes=latest, popular_anime=popular, source=self.id)
        cache.set(ck, data.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
        return data

    # ---------- detail ----------
    def _parse_info(self, doc) -> dict:
        info: dict[str, str] = {}
        for span in self._all(doc, "detail_info_container"):
            raw = self._text(span)
            if not raw:
                continue
            b = span.find(["b", "strong"])
            if b and self._text(b):
                key = self._text(b).rstrip(":").strip().lower()
                value = raw.replace(self._text(b), "", 1).lstrip(": ").strip()
            elif ":" in raw:
                key, value = [p.strip() for p in raw.split(":", 1)]
                key = key.lower()
            else:
                continue
            for field, aliases in INFO_KEYS.items():
                if field not in info and any(key == a or key.startswith(a) for a in aliases) and value:
                    info[field] = value
                    break
        return info

    async def detail(self, slug: str) -> AnimeDetail:
        ck = self._ck(f"detail_{slug}")
        cached = cache.get(ck)
        if cached:
            return AnimeDetail(**cached)
        doc = await fetch_page(self._url("detail", slug=slug))

        title_el = self._one(doc, "detail_title")
        if not title_el:
            raise NotFoundError(f"Anime '{slug}' tidak ditemukan di {self.id}")

        info = self._parse_info(doc)
        score_el = self._one(doc, "detail_score")
        synopsis_el = self._one(doc, "detail_synopsis")

        episodes: list[EpisodeListItem] = []
        for el in self._all(doc, "detail_episodes_container"):
            link = self._one(el, "detail_episode_title")
            if not link or not link.get("href"):
                continue
            ep_title = self._text(link)
            episodes.append(EpisodeListItem(
                title=ep_title,
                slug=slug_of(link["href"]),
                number=episode_number(ep_title),
                uploaded_at=self._text(self._one(el, "detail_episode_date")) or None,
            ))

        detail = AnimeDetail(
            title=self._text(title_el),
            slug=slug,
            poster=self._img(self._one(doc, "detail_thumb")),
            synopsis=self._text(synopsis_el) or "Tidak ada sinopsis tersedia.",
            japanese_title=info.get("japanese_title"),
            type=info.get("type") or "TV",
            status=info.get("status") or "Ongoing",
            score=self._text(score_el) or info.get("score") or "N/A",
            total_episodes=info.get("total_episodes") or (str(len(episodes)) if episodes else "?"),
            duration=info.get("duration") or "24 min",
            season=info.get("season"),
            studios=info.get("studios"),
            released=info.get("released"),
            genres=[self._text(g) for g in self._all(doc, "detail_genres") if self._text(g)],
            episodes=episodes,
            source=self.id,
        )
        cache.set(ck, detail.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
        return detail

    # ---------- episode / video ----------
    async def episode(self, slug: str) -> EpisodeStreamDetail:
        ck = self._ck(f"episode_{slug}")
        cached = cache.get(ck)
        if cached:
            return EpisodeStreamDetail(**cached)
        doc = await fetch_page(self._url("episode", slug=slug))

        title_el = self._one(doc, "episode_title_header")
        title = self._text(title_el) or slug

        iframe_el = self._one(doc, "episode_stream_iframe")
        default_url = ""
        if iframe_el:
            default_url = iframe_el.get("src") or iframe_el.get("data-src") or ""
        if not default_url:
            for fr in doc.css("iframe[src]"):
                default_url = fr.get("src", "")
                if default_url:
                    break
        default_url = self._abs(default_url)

        servers: list[VideoServer] = []
        for idx, s_el in enumerate(self._all(doc, "episode_server_list"), start=1):
            btn = self._one(s_el, "episode_server_button") or s_el
            name = self._text(btn) or f"Server {idx}"
            post_id = btn.get("data-post") or btn.get("data-nume") or str(idx)
            stream_url = default_url
            content = btn.get("data-content") or ""
            if content:
                try:
                    decoded = base64.b64decode(content).decode("utf-8")
                    m = re.search(r'src=["\']([^"\']+)["\']', decoded)
                    if m:
                        stream_url = self._abs(m.group(1))
                except Exception:
                    pass
            servers.append(VideoServer(
                server_id=f"srv_{idx}_{post_id}",
                server_name=name,
                stream_url=stream_url or default_url,
                quality="720p",
                is_embed=True,
            ))
        if not servers and default_url:
            servers.append(VideoServer(
                server_id="default_server", server_name="Default HD Player",
                stream_url=default_url, quality="720p", is_embed=True,
            ))

        prev_el = self._one(doc, "episode_nav_prev")
        next_el = self._one(doc, "episode_nav_next")

        downloads: list[DownloadLink] = []
        box = self._one(doc, "episode_download_container")
        if box:
            for li in box.select("li"):
                q_el = li.select_one("strong, b")
                quality = self._text(q_el) or "720p"
                for a in li.select("a"):
                    host, href = self._text(a), a.get("href", "")
                    if host and href:
                        downloads.append(DownloadLink(quality=quality, host_name=host, url=href))

        anime_title, ep_num = split_episode_title(title)
        detail = EpisodeStreamDetail(
            title=title,
            episode_slug=slug,
            anime_slug=slug.rsplit("-episode-", 1)[0] if "-episode-" in slug else slug,
            anime_title=anime_title,
            episode_number=ep_num or episode_number(slug.replace("-episode-", " episode ")),
            default_stream_url=default_url,
            servers=servers,
            download_links=downloads,
            prev_episode_slug=slug_of(prev_el.get("href", "")) if prev_el else None,
            next_episode_slug=slug_of(next_el.get("href", "")) if next_el else None,
            source=self.id,
        )
        cache.set(ck, detail.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
        return detail

    # ---------- jadwal tayang ----------
    def _schedule_items(self, root) -> list[ScheduleItem]:
        items: list[ScheduleItem] = []
        seen: set[str] = set()
        nodes = self._all(root, "schedule_item")
        for el in nodes:
            link_el = self._one(el, "schedule_url") if el.name != "a" else el
            href = link_el.get("href", "") if link_el else ""
            slug = slug_of(href)
            title_el = self._one(el, "schedule_title")
            title = self._text(title_el) if title_el else ""
            if not title and link_el:
                title = link_el.get("title", "") or self._text(link_el)
            if not slug or not title or slug in seen:
                continue
            seen.add(slug)
            time_el = self._one(el, "schedule_time")
            time_txt = self._text(time_el)
            if not time_txt:
                m = re.search(r"\b\d{1,2}[:.]\d{2}\b", self._text(el))
                time_txt = m.group(0) if m else ""
            items.append(ScheduleItem(
                title=title,
                slug=slug,
                poster=self._img(self._one(el, "schedule_thumb")),
                time=time_txt or None,
            ))
        return items

    async def schedule(self) -> ScheduleResponse:
        ck = self._ck("schedule")
        cached = cache.get(ck)
        if cached:
            return ScheduleResponse(**cached)
        doc = await fetch_page(self._url("schedule"))

        days: dict[str, list[ScheduleItem]] = {}

        # Cara 1: kontainer per hari.
        for box in self._all(doc, "schedule_day"):
            title_el = self._one(box, "schedule_day_title")
            day = detect_day(self._text(title_el)) or detect_day(
                " ".join([box.get("id", ""), " ".join(box.get("class", [])), box.get("data-day", "")])
            )
            if not day:
                continue
            items = self._schedule_items(box)
            if items:
                bucket = days.setdefault(day, [])
                known = {i.slug for i in bucket}
                bucket.extend(i for i in items if i.slug not in known)

        # Cara 2 (cadangan): heading nama hari diikuti daftar anime sampai heading hari berikutnya.
        if not days:
            for head in doc.css("h1, h2, h3, h4, h5, strong, b"):
                day = detect_day(self._text(head))
                if not day or len(self._text(head)) > 20:
                    continue
                items: list[ScheduleItem] = []
                for sib in head.find_all_next(limit=200):
                    if sib is not head and sib.name in ("h1", "h2", "h3", "h4", "h5") and detect_day(self._text(sib)):
                        break
                    if sib.name == "a" and sib.get("href"):
                        title = sib.get("title", "") or self._text(sib)
                        slug = slug_of(sib["href"])
                        if title and slug and all(i.slug != slug for i in items):
                            items.append(ScheduleItem(title=title, slug=slug,
                                                      poster=self._img(sib.find("img"))))
                if items:
                    bucket = days.setdefault(day, [])
                    known = {i.slug for i in bucket}
                    bucket.extend(i for i in items if i.slug not in known)

        ordered = [ScheduleDay(day=d, items=days[d]) for d in DAY_ORDER if d in days]
        resp = ScheduleResponse(days=ordered, source=self.id)
        cache.set(ck, resp.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
        return resp
