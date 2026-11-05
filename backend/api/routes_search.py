from fastapi import APIRouter, Query
from models.anime import AnimeCard
from scrapers.samehadaku.search import search_anime

router = APIRouter(tags=["Search"])

@router.get("/search", response_model=list[AnimeCard])
async def search_anime_endpoint(
    q: str = Query(..., min_length=1, description="Kata kunci pencarian anime")
):
    """Mencari anime berdasarkan judul atau kata kunci."""
    return await search_anime(query=q)
