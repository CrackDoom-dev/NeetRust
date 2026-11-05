# [KATALOG] scrapers.samehadaku.selectors

class SamehadakuSelectors:
    """
    Kamus terpusat untuk semua CSS Selector situs Samehadaku.
    Jika layout situs web berubah di kemudian hari, ubah selector HANYA di file ini.
    """
    # === HOME PAGE ===
    HOME_BANNER_CONTAINER = ".slider-content .owl-item, .hero-slider .item, .top-slider .swiper-slide"
    HOME_BANNER_TITLE = ".title, .entry-title, h2, h3"
    HOME_BANNER_THUMB = "img"
    HOME_BANNER_URL = "a"
    HOME_BANNER_GENRE = ".genre, .genres a"

    # Home - Latest Episode Releases
    LATEST_EPISODES_CONTAINER = ".post-show ul li, .widget_latest_episodelist ul li, .latest-updates article, .relat ul li"
    LATEST_EPISODE_TITLE = ".entry-title a, h2 a, .title a"
    LATEST_EPISODE_THUMB = ".thumb img, img"
    LATEST_EPISODE_NUMBER = ".eps, .ep, .dtl .author"
    LATEST_EPISODE_RELEASE_TIME = ".date, .dtla .posted"
    LATEST_EPISODE_URL = "a"

    # Home - Popular / Top Rated
    POPULAR_CONTAINER = ".widget_topanime ul li, .top-anime-item, .pop-anm ul li"
    POPULAR_TITLE = ".title a, h2 a, .entry-title a"
    POPULAR_THUMB = "img"
    POPULAR_RATING = ".score, .rating, .numscore"
    POPULAR_URL = "a"

    # === ANIME DIRECTORY / LIST ===
    ANIME_LIST_CONTAINER = ".animepost, article.animpost, .animpost"
    ANIME_LIST_TITLE = ".title, h2, .entry-title"
    ANIME_LIST_THUMB = "img"
    ANIME_LIST_TYPE = ".type, .type-anime"
    ANIME_LIST_STATUS = ".status"
    ANIME_LIST_SCORE = ".score"
    ANIME_LIST_URL = "a"
    PAGINATION_NEXT = ".pagination .next, .page-numbers.next, a.next"
    PAGINATION_CURRENT = ".pagination .current, .page-numbers.current, span.current"

    # === ANIME DETAIL ===
    DETAIL_TITLE = ".entry-title, .infoanime h1, .infox h1"
    DETAIL_THUMB = ".thumb img, .poster img"
    DETAIL_SYNOPSIS = ".entry-content-single, .desc, .synopsis, .entry-content"
    DETAIL_SCORE = ".rating-prc .num, .score"
    DETAIL_STATUS = ".spe span:contains('Status'), .info-content span:has(b)"
    DETAIL_GENRES = ".genre-info a, .genres a, .genre a"
    DETAIL_EPISODES_CONTAINER = ".lstepsiode ul li, .episodelist ul li, .eps-list li"
    DETAIL_EPISODE_TITLE = ".eps a, .title a, a"
    DETAIL_EPISODE_DATE = ".date"
    DETAIL_INFO_CONTAINER = ".info-content span, .spe span"

    # === EPISODE STREAMING & SERVERS ===
    EPISODE_STREAM_IFRAME = "#server, #player_embed iframe, .player-embed iframe, .responsive-embed-stream iframe"
    EPISODE_SERVER_LIST = "#server ul li, .server-item, .server_item"
    EPISODE_SERVER_BUTTON = "div[data-post], .btn-server, a[data-post]"
    EPISODE_TITLE_HEADER = ".entry-title, h1.title"
    EPISODE_NAV_PREV = ".naveps .nvs.nv-prev a, a.prev-eps"
    EPISODE_NAV_NEXT = ".naveps .nvs.nv-next a, a.next-eps"
    EPISODE_NAV_ALL = ".naveps .nvs.nv-all a, a.all-eps"
    EPISODE_DOWNLOAD_CONTAINER = ".download-eps, .list-download, .download"

    # === SEARCH ===
    SEARCH_CONTAINER = ".animepost, article.animpost, .search-item"
    SEARCH_TITLE = ".title, h2, .entry-title"
    SEARCH_THUMB = "img"
    SEARCH_URL = "a"
    SEARCH_TYPE = ".type"
    SEARCH_STATUS = ".status"
    SEARCH_SCORE = ".score"

selectors = SamehadakuSelectors()
