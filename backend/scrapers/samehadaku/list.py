import logging
from core.config import settings
from core.fetcher import fetch_page
from core.cache import cache
from scrapers.samehadaku.selectors import selectors
from models.anime import AnimeCard, AnimeListResponse
from models.common import PaginationMeta

logger = logging.getLogger("scrapers.samehadaku.list")

def extract_slug(url: str) -> str:
    if not url:
        return ""
    return url.rstrip("/").split("/")[-1]

async def scrape_anime_list(page: int = 1, genre: str = None) -> AnimeListResponse:
    cache_key = f"anime_list_{page}_{genre or 'all'}"
    cached = cache.get(cache_key)
    if cached:
        return AnimeListResponse(**cached)

    if genre:
        url = f"{settings.SAMEHADAKU_BASE_URL}/genre/{genre}/page/{page}/" if page > 1 else f"{settings.SAMEHADAKU_BASE_URL}/genre/{genre}/"
    else:
        url = f"{settings.SAMEHADAKU_BASE_URL}/daftar-anime/page/{page}/" if page > 1 else f"{settings.SAMEHADAKU_BASE_URL}/daftar-anime/"

    doc = await fetch_page(url)
    items: list[AnimeCard] = []

    containers = doc.css(selectors.ANIME_LIST_CONTAINER)
    for el in containers:
        title_el = el.select_one(selectors.ANIME_LIST_TITLE)
        img_el = el.select_one(selectors.ANIME_LIST_THUMB)
        link_el = el.select_one(selectors.ANIME_LIST_URL)
        type_el = el.select_one(selectors.ANIME_LIST_TYPE)
        status_el = el.select_one(selectors.ANIME_LIST_STATUS)
        score_el = el.select_one(selectors.ANIME_LIST_SCORE)

        if title_el or link_el:
            title = title_el.get_text(strip=True) if title_el else ""
            href = link_el.get("href", "") if link_el else ""
            img_src = ""
            if img_el:
                img_src = img_el.get("src") or img_el.get("data-src") or ""

            items.append(AnimeCard(
                title=title,
                slug=extract_slug(href),
                poster=img_src,
                type=type_el.get_text(strip=True) if type_el else "TV",
                status=status_el.get_text(strip=True) if status_el else "Ongoing",
                score=score_el.get_text(strip=True) if score_el else "N/A"
            ))

    has_next = doc.css_first(selectors.PAGINATION_NEXT) is not None
    response_data = AnimeListResponse(
        items=items,
        pagination=PaginationMeta(
            current_page=page,
            has_next=has_next
        )
    )
    cache.set(cache_key, response_data.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
    return response_data
