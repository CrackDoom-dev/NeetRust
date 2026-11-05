import logging
import re
from core.config import settings
from core.fetcher import fetch_page
from core.cache import cache
from scrapers.samehadaku.selectors import selectors
from models.anime import HomeData, HomeBanner, LatestEpisodeCard, AnimeCard

logger = logging.getLogger("scrapers.samehadaku.home")

def extract_slug(url: str) -> str:
    if not url:
        return ""
    clean = url.rstrip("/").split("/")[-1]
    return clean

async def scrape_home() -> HomeData:
    cache_key = "home_data"
    cached = cache.get(cache_key)
    if cached:
        return HomeData(**cached)

    url = settings.SAMEHADAKU_BASE_URL
    page = await fetch_page(url)

    banners: list[HomeBanner] = []
    banner_elements = page.css(selectors.HOME_BANNER_CONTAINER)
    for el in banner_elements:
        title_el = el.select_one(selectors.HOME_BANNER_TITLE)
        img_el = el.select_one(selectors.HOME_BANNER_THUMB)
        link_el = el.select_one(selectors.HOME_BANNER_URL)
        genre_els = el.select(selectors.HOME_BANNER_GENRE)

        if title_el and link_el:
            title = title_el.get_text(strip=True)
            href = link_el.get("href", "")
            img_src = img_el.get("src", "") or img_el.get("data-src", "") if img_el else ""
            genres = [g.get_text(strip=True) for g in genre_els if g.get_text(strip=True)]
            banners.append(HomeBanner(
                title=title,
                slug=extract_slug(href),
                poster=img_src,
                genres=genres
            ))

    latest_episodes: list[LatestEpisodeCard] = []
    ep_elements = page.css(selectors.LATEST_EPISODES_CONTAINER)
    for el in ep_elements[:20]:
        title_el = el.select_one(selectors.LATEST_EPISODE_TITLE)
        img_el = el.select_one(selectors.LATEST_EPISODE_THUMB)
        ep_el = el.select_one(selectors.LATEST_EPISODE_NUMBER)
        date_el = el.select_one(selectors.LATEST_EPISODE_RELEASE_TIME)
        link_el = el.select_one(selectors.LATEST_EPISODE_URL)

        if title_el or link_el:
            raw_title = title_el.get_text(strip=True) if title_el else ""
            href = link_el.get("href", "") if link_el else ""
            img_src = ""
            if img_el:
                img_src = img_el.get("src") or img_el.get("data-src") or ""

            ep_num = ep_el.get_text(strip=True) if ep_el else "Ep ?"
            rel_time = date_el.get_text(strip=True) if date_el else "Baru saja"

            latest_episodes.append(LatestEpisodeCard(
                title=raw_title,
                episode_slug=extract_slug(href),
                poster=img_src,
                episode_number=ep_num,
                release_time=rel_time
            ))

    popular_anime: list[AnimeCard] = []
    pop_elements = page.css(selectors.POPULAR_CONTAINER)
    for el in pop_elements[:15]:
        title_el = el.select_one(selectors.POPULAR_TITLE)
        img_el = el.select_one(selectors.POPULAR_THUMB)
        score_el = el.select_one(selectors.POPULAR_RATING)
        link_el = el.select_one(selectors.POPULAR_URL)

        if title_el or link_el:
            title = title_el.get_text(strip=True) if title_el else ""
            href = link_el.get("href", "") if link_el else ""
            img_src = ""
            if img_el:
                img_src = img_el.get("src") or img_el.get("data-src") or ""
            score = score_el.get_text(strip=True) if score_el else "N/A"

            popular_anime.append(AnimeCard(
                title=title,
                slug=extract_slug(href),
                poster=img_src,
                score=score,
                status="Populer"
            ))

    data = HomeData(
        banners=banners,
        latest_episodes=latest_episodes,
        popular_anime=popular_anime
    )
    cache.set(cache_key, data.model_dump(), ttl=settings.CACHE_DEFAULT_TTL)
    return data
