# [KATALOG] scrapers.samehadaku.detail
import logging
from core.config import settings
from core.fetcher import fetch_page
from core.cache import cache
from core.errors import NotFoundError
from scrapers.samehadaku.selectors import selectors
from models.anime import AnimeDetail, EpisodeListItem

logger = logging.getLogger("scrapers.samehadaku.detail")

def extract_slug(url: str) -> str:
    if not url:
        return ""
    return url.rstrip("/").split("/")[-1]

async def scrape_anime_detail(slug: str) -> AnimeDetail:
    cache_key = f"anime_detail_{slug}"
    cached = cache.get(cache_key)
    if cached:
        return AnimeDetail(**cached)

    url = f"{settings.SAMEHADAKU_BASE_URL}/anime/{slug}/"
    doc = await fetch_page(url)

    title_el = doc.css_first(selectors.DETAIL_TITLE)
    if not title_el:
        logger.warning(f"Detail title not found for slug: {slug}")
        raise NotFoundError(f"Anime dengan slug '{slug}' tidak ditemukan")

    title = title_el.get_text(strip=True)
    thumb_el = doc.css_first(selectors.DETAIL_THUMB)
    poster = thumb_el.get("src") or thumb_el.get("data-src") if thumb_el else ""

    synopsis_el = doc.css_first(selectors.DETAIL_SYNOPSIS)
    synopsis = synopsis_el.get_text(strip=True) if synopsis_el else "Tidak ada sinopsis tersedia."

    score_el = doc.css_first(selectors.DETAIL_SCORE)
    score = score_el.get_text(strip=True) if score_el else "N/A"

    # Genres
    genre_els = doc.css(selectors.DETAIL_GENRES)
    genres = [g.get_text(strip=True) for g in genre_els if g.get_text(strip=True)]

    # Episode list
    episodes: list[EpisodeListItem] = []
    ep_elements = doc.css(selectors.DETAIL_EPISODES_CONTAINER)
    for el in ep_elements:
        ep_link = el.select_one(selectors.DETAIL_EPISODE_TITLE)
        ep_date = el.select_one(selectors.DETAIL_EPISODE_DATE)
        if ep_link:
            ep_title = ep_link.get_text(strip=True)
            ep_href = ep_link.get("href", "")
            date_str = ep_date.get_text(strip=True) if ep_date else None
            episodes.append(EpisodeListItem(
                title=ep_title,
                slug=extract_slug(ep_href),
                number=ep_title.replace("Episode", "").strip() or "1",
                uploaded_at=date_str
            ))

    detail = AnimeDetail(
        title=title,
        slug=slug,
        poster=poster,
        synopsis=synopsis,
        score=score,
        genres=genres,
        episodes=episodes
    )
    cache.set(cache_key, detail.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
    return detail
