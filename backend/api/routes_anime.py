from fastapi import APIRouter, Query
from typing import Optional
from models.anime import HomeData, AnimeDetail, AnimeListResponse
from scrapers.samehadaku.home import scrape_home
from scrapers.samehadaku.detail import scrape_anime_detail
from scrapers.samehadaku.list import scrape_anime_list

router = APIRouter(tags=["Anime"])

@router.get("/home", response_model=HomeData)
async def get_home():
    """Mengambil data beranda: banner utama, episode rilis terbaru, dan anime terpopuler."""
    return await scrape_home()

@router.get("/anime/{slug}", response_model=AnimeDetail)
async def get_anime_detail(slug: str):
    """Mengambil detail anime berdasarkan slug beserta daftar semua episode."""
    return await scrape_anime_detail(slug)

@router.get("/list", response_model=AnimeListResponse)
async def get_anime_catalog(
    page: int = Query(1, ge=1, description="Nomor halaman"),
    genre: Optional[str] = Query(None, description="Filter berdasarkan slug genre (misal: action, romance)")
):
    """Mengambil katalog daftar anime dengan paginasi dan filter genre opsional."""
    return await scrape_anime_list(page=page, genre=genre)
