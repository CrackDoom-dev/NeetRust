import difflib
import re
from typing import Optional

from core.errors import NotFoundError
from models.anime import AnimeCard, AnimeDetail
from models.episode import EpisodeStreamDetail
from scrapers.base import BaseSource

_EP_RE = re.compile(r"-(?:episode|eps|ep)-(\d+(?:-\d+)?)", re.I)


def slug_to_query(slug: str) -> str:
    s = _EP_RE.split(slug, maxsplit=1)[0]
    s = re.sub(r"-(?:subtitle-indonesia|sub-indo|sub-indonesia)\b.*$", "", s, flags=re.I)
    return s.replace("-", " ").strip()


def episode_from_slug(slug: str) -> Optional[str]:
    m = _EP_RE.search(slug)
    return m.group(1).replace("-", ".") if m else None


def norm_num(text: str) -> str:
    m = re.search(r"\d+(?:[.-]\d+)?", text or "")
    if not m:
        return ""
    val = float(m.group(0).replace("-", "."))
    return str(int(val)) if val == int(val) else str(val)


def _norm(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", text.lower()).strip()


def best_match(query: str, cards: list[AnimeCard], threshold: float = 0.6) -> Optional[AnimeCard]:
    q = _norm(query)
    best, best_score = None, 0.0
    for c in cards:
        score = max(
            difflib.SequenceMatcher(None, q, _norm(c.title)).ratio(),
            difflib.SequenceMatcher(None, q, _norm(c.slug)).ratio(),
        )
        if score > best_score:
            best, best_score = c, score
    return best if best_score >= threshold else None


async def resolve_detail(src: BaseSource, slug: str) -> AnimeDetail:
    """Cari anime yang sama di sumber lain lewat pencarian judul yang diturunkan dari slug."""
    query = slug_to_query(slug)
    match = best_match(query, await src.search(query))
    if not match:
        raise NotFoundError(f"'{query}' tidak ditemukan di {src.id}")
    return await src.detail(match.slug)


async def resolve_episode(src: BaseSource, slug: str) -> EpisodeStreamDetail:
    number = episode_from_slug(slug)
    if not number:
        raise NotFoundError("Nomor episode tidak bisa dibaca dari slug")
    detail = await resolve_detail(src, slug)
    wanted = norm_num(number)
    ep = next((e for e in detail.episodes if norm_num(e.number) == wanted), None)
    if not ep:
        raise NotFoundError(f"Episode {number} tidak ada di {src.id}")
    return await src.episode(ep.slug)
