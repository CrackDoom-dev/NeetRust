# [KATALOG] scrapers.samehadaku.episode
import logging
import base64
import re
from core.config import settings
from core.fetcher import fetch_page
from core.cache import cache
from core.errors import NotFoundError
from scrapers.samehadaku.selectors import selectors
from models.episode import EpisodeStreamDetail, VideoServer, DownloadLink

logger = logging.getLogger("scrapers.samehadaku.episode")

def extract_slug(url: str) -> str:
    if not url:
        return ""
    return url.rstrip("/").split("/")[-1]

async def scrape_episode_stream(slug: str) -> EpisodeStreamDetail:
    cache_key = f"episode_stream_{slug}"
    cached = cache.get(cache_key)
    if cached:
        return EpisodeStreamDetail(**cached)

    url = f"{settings.SAMEHADAKU_BASE_URL}/{slug}/"
    doc = await fetch_page(url)

    title_el = doc.css_first(selectors.EPISODE_TITLE_HEADER)
    title = title_el.get_text(strip=True) if title_el else slug

    # Iframe default stream URL
    iframe_el = doc.css_first(selectors.EPISODE_STREAM_IFRAME)
    default_stream_url = ""
    if iframe_el:
        default_stream_url = iframe_el.get("src") or iframe_el.get("data-src") or ""

    # Streaming servers
    servers: list[VideoServer] = []
    server_elements = doc.css(selectors.EPISODE_SERVER_LIST)
    idx = 1
    for s_el in server_elements:
        btn = s_el.select_one(selectors.EPISODE_SERVER_BUTTON) or s_el
        name = btn.get_text(strip=True) or f"Server {idx}"
        post_id = btn.get("data-post") or btn.get("data-nume") or str(idx)
        content_data = btn.get("data-content") or ""

        # Decode base64 data-content if present
        stream_url = default_stream_url
        if content_data:
            try:
                decoded = base64.b64decode(content_data).decode("utf-8")
                # find src="..."
                match = re.search(r'src=["\']([^"\']+)["\']', decoded)
                if match:
                    stream_url = match.group(1)
            except Exception:
                pass

        servers.append(VideoServer(
            server_id=f"srv_{idx}_{post_id}",
            server_name=name,
            stream_url=stream_url or default_stream_url,
            quality="720p",
            is_embed=True
        ))
        idx += 1

    if not servers and default_stream_url:
        servers.append(VideoServer(
            server_id="default_server",
            server_name="Default HD Player",
            stream_url=default_stream_url,
            quality="720p",
            is_embed=True
        ))

    # Navigation prev/next
    prev_el = doc.css_first(selectors.EPISODE_NAV_PREV)
    next_el = doc.css_first(selectors.EPISODE_NAV_NEXT)
    prev_slug = extract_slug(prev_el.get("href", "")) if prev_el else None
    next_slug = extract_slug(next_el.get("href", "")) if next_el else None

    # Download mirrors
    download_links: list[DownloadLink] = []
    dl_box = doc.css_first(selectors.EPISODE_DOWNLOAD_CONTAINER)
    if dl_box:
        for li in dl_box.select("li"):
            q_el = li.select_one("strong, b")
            quality = q_el.get_text(strip=True) if q_el else "720p"
            for a in li.select("a"):
                host = a.get_text(strip=True)
                href = a.get("href", "")
                if href and host:
                    download_links.append(DownloadLink(
                        quality=quality,
                        host_name=host,
                        url=href
                    ))

    detail = EpisodeStreamDetail(
        title=title,
        episode_slug=slug,
        anime_slug=slug.rsplit("-episode-", 1)[0] if "-episode-" in slug else slug,
        episode_number=slug.rsplit("-episode-", 1)[-1].replace("-", ".") if "-episode-" in slug else "1",
        default_stream_url=default_stream_url,
        servers=servers,
        download_links=download_links,
        prev_episode_slug=prev_slug,
        next_episode_slug=next_slug
    )
    cache.set(cache_key, detail.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
    return detail
