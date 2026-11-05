from typing import Dict
from pydantic import BaseModel
from scrapers.samehadaku.selectors import selectors as _samehadaku_selectors

class SiteProfile(BaseModel):
    id: str
    name: str
    base_url: str
    preset: str = "wp-anime"
    urls: Dict[str, str] = {}
    selectors: Dict[str, str] = {}
    enabled: bool = True
    priority: int = 100
    builtin: bool = False

# Preset dasar diambil dari selectors.py Samehadaku (nama atribut jadi huruf kecil),
# lalu ditambah selector untuk jadwal tayang.
_WP_ANIME_SELECTORS: Dict[str, str] = {
    name.lower(): getattr(_samehadaku_selectors, name)
    for name in dir(_samehadaku_selectors)
    if name.isupper()
}
_WP_ANIME_SELECTORS.update({
    "schedule_day": ".schedule-day, .day-container, .jadwal-hari, .sch-day, .tab-pane, .tabcontent, .scheduleday",
    "schedule_day_title": "h2, h3, h4, .day, .title-day, .hari",
    "schedule_item": ".animepost, .schedule-item, article, li",
    "schedule_title": ".title, h2, h3, .entry-title, a",
    "schedule_thumb": "img",
    "schedule_url": "a",
    "schedule_time": ".time, .jam, .release-time, .waktu",
})

# Template path per operasi. {q} {page} {genre} {slug} akan diisi otomatis.
_WP_ANIME_URLS: Dict[str, str] = {
    "home": "/",
    "search": "/?s={q}",
    "list_first": "/daftar-anime/",
    "list": "/daftar-anime/page/{page}/",
    "genre_first": "/genre/{genre}/",
    "genre": "/genre/{genre}/page/{page}/",
    "detail": "/anime/{slug}/",
    "episode": "/{slug}/",
    "schedule": "/jadwal-rilis/",
    "ongoing_first": "/daftar-anime/?status=Currently+Airing&order=update",
    "ongoing": "/daftar-anime/page/{page}/?status=Currently+Airing&order=update",
    "movies_first": "/daftar-anime/?type=Movie&order=update",
    "movies": "/daftar-anime/page/{page}/?type=Movie&order=update",
}

PRESETS: Dict[str, Dict[str, Dict[str, str]]] = {
    "wp-anime": {"selectors": _WP_ANIME_SELECTORS, "urls": _WP_ANIME_URLS},
}

def merged_selectors(profile: SiteProfile) -> Dict[str, str]:
    base = dict(PRESETS.get(profile.preset, PRESETS["wp-anime"])["selectors"])
    base.update({k: v for k, v in profile.selectors.items() if v})
    return base

def merged_urls(profile: SiteProfile) -> Dict[str, str]:
    base = dict(PRESETS.get(profile.preset, PRESETS["wp-anime"])["urls"])
    base.update({k: v for k, v in profile.urls.items() if v})
    return base
