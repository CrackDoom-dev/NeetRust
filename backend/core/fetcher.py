# [KATALOG] core.fetcher
import logging
from typing import Optional
from bs4 import BeautifulSoup
import httpx
from core.errors import ScrapingError

logger = logging.getLogger("core.fetcher")

# Realistic browser headers
DEFAULT_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language": "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7",
    "Referer": "https://www.google.com/",
    "DNT": "1"
}

class ParsedPage:
    """Unified wrapper around Scrapling or BeautifulSoup element parsing."""
    def __init__(self, raw_html: str, soup: BeautifulSoup):
        self.raw_html = raw_html
        self.soup = soup

    def css(self, selector: str):
        return self.soup.select(selector)

    def css_first(self, selector: str):
        return self.soup.select_one(selector)

async def fetch_page(url: str, timeout: int = 15) -> ParsedPage:
    """
    Mengambil halaman web menggunakan Scrapling sebagai metode utama,
    dengan fallback otomatis ke httpx + BeautifulSoup jika Scrapling gagal atau tidak tersedia.
    """
    logger.info(f"Fetching URL: {url}")
    
    # 1. Coba menggunakan Scrapling
    try:
        from scrapling import Fetcher
        fetcher = Fetcher()
        response = fetcher.get(url, timeout=timeout)
        if response and response.status in (200, 304) and response.body:
            html = response.body.decode("utf-8", errors="ignore") if isinstance(response.body, bytes) else str(response.body)
            soup = BeautifulSoup(html, "html.parser")
            logger.info(f"Successfully fetched using Scrapling: {url}")
            return ParsedPage(raw_html=html, soup=soup)
    except Exception as e:
        logger.warning(f"Scrapling fetch attempt failed or not available for {url}: {e}. Falling back to httpx.")

    # 2. Fallback menggunakan HTTPX
    try:
        async with httpx.AsyncClient(headers=DEFAULT_HEADERS, follow_redirects=True, timeout=timeout) as client:
            resp = await client.get(url)
            if resp.status_code != 200:
                logger.error(f"HTTPX returned status {resp.status_code} for {url}")
                raise ScrapingError(f"Gagal mengambil data dari {url} (status: {resp.status_code})")
            html = resp.text
            soup = BeautifulSoup(html, "html.parser")
            logger.info(f"Successfully fetched using HTTPX fallback: {url}")
            return ParsedPage(raw_html=html, soup=soup)
    except Exception as e:
        logger.error(f"All fetch attempts failed for {url}: {e}")
        raise ScrapingError(f"Koneksi ke sumber gagal: {str(e)}")
