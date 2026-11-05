# [KATALOG] scrapers.samehadaku.search
import logging
from core.config import settings
from core.fetcher import fetch_page
from core.cache import cache
from scrapers.samehadaku.selectors import selectors
from models.anime import AnimeCard

logger = logging.getLogger("scrapers.samehadaku.search")

def extract_slug(url: str) -> str:
    if not url:
        return ""
    return url.rstrip("/").split("/")[-1]

async def search_anime(query: str) -> list[AnimeCard]:
    if not query.strip():
        return []

    cache_key = f"search_{query.strip().lower()}"
    cached = cache.get(cache_key)
    if cached:
        return [AnimeCard(**item) for item in cached]

    url = f"{settings.SAMEHADAKU_BASE_URL}/?s={query.strip()}"
    doc = await fetch_page(url)

    results: list[AnimeCard] = []
    containers = doc.css(selectors.SEARCH_CONTAINER)

    for el in containers:
        title_el = el.select_one(selectors.SEARCH_TITLE)
        img_el = el.select_one(selectors.SEARCH_THUMB)
        link_el = el.select_one(selectors.SEARCH_URL)
        type_el = el.select_one(selectors.SEARCH_TYPE)
        status_el = el.select_one(selectors.SEARCH_STATUS)
        score_el = el.select_one(selectors.SEARCH_SCORE)

        if title_el or link_el:
            title = title_el.get_text(strip=True) if title_el else ""
            href = link_el.get("href", "") if link_el else ""
            img_src = ""
            if img_el:
                img_src = img_el.get("src") or img_el.get("data-src") or ""

            results.append(AnimeCard(
                title=title,
                slug=extract_slug(href),
                poster=img_src,
                type=type_el.get_text(strip=True) if type_el else "TV",
                status=status_el.get_text(strip=True) if status_el else "Ongoing",
                score=score_el.get_text(strip=True) if score_el else "N/A"
            ))

    cache.set(cache_key, [r.model_dump() for r in results], ttl=settings.CACHE_DEFAULT_TTL)
    return results
