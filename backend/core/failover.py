import asyncio
import logging
from typing import Any, Optional

from core.config import settings
from core.errors import AppError, NotFoundError, ScrapingError
from core.registry import registry
from core.resolver import resolve_detail, resolve_episode
from scrapers.generic import GenericSource

logger = logging.getLogger("core.failover")


def _valid_home(r) -> bool:
    return bool(r.banners or r.latest_episodes or r.popular_anime)

def _valid_list(r) -> bool:
    return bool(r.items)

def _valid_detail(r) -> bool:
    return bool(r.title and (r.episodes or (r.synopsis and not r.synopsis.startswith("Tidak ada sinopsis"))))

def _valid_episode(r) -> bool:
    return bool(r.servers or r.default_stream_url or r.download_links)

def _valid_schedule(r) -> bool:
    return any(d.items for d in r.days)

def _valid_search(r) -> bool:
    return bool(r)


VALIDATORS = {
    "home": _valid_home,
    "search": _valid_search,
    "list": _valid_list,
    "ongoing": _valid_list,
    "movies": _valid_list,
    "detail": _valid_detail,
    "episode": _valid_episode,
    "schedule": _valid_schedule,
}

# Operasi yang boleh mengembalikan hasil kosong tanpa dianggap error sumber.
EMPTY_OK = {"search"}


async def _call(src: GenericSource, op: str, args: tuple, kw: dict, native: bool):
    fn = getattr(src, op)
    validator = VALIDATORS[op]
    try:
        result = await fn(*args, **kw)
        if not validator(result):
            raise ScrapingError("Hasil kosong / selector tidak cocok")
        return result
    except Exception as direct_err:
        # Slug milik sumber lain: cari padanannya lewat judul.
        if native or op not in ("detail", "episode"):
            raise
        resolver = resolve_detail if op == "detail" else resolve_episode
        logger.info(f"[{src.id}] slug langsung gagal ({direct_err}); mencari lewat judul")
        result = await resolver(src, args[0])
        if not validator(result):
            raise ScrapingError("Hasil kosong setelah pencarian judul")
        return result


async def run_op(op: str, *args, source: Optional[str] = None, strict: bool = False, **kw) -> Any:
    """Jalankan operasi pada sumber berurutan sampai ada yang memberi hasil valid."""
    candidates = registry.candidates(preferred=source, strict=strict)
    if not candidates:
        raise ScrapingError("Tidak ada sumber aktif. Aktifkan atau tambahkan sumber di menu Sumber.")

    # Slug dianggap milik sumber yang diminta, atau (tanpa parameter source) sumber berprioritas tertinggi,
    # bukan sumber yang kebetulan paling depan karena cooldown.
    enabled = [p for p in registry.ordered() if p.enabled]
    native_id = source if source and registry.get(source) else (enabled[0].id if enabled else candidates[0].id)
    errors: dict[str, str] = {}
    not_found = 0
    empty_result = None

    for profile in candidates:
        src = GenericSource(profile)
        try:
            result = await asyncio.wait_for(
                _call(src, op, args, kw, native=(profile.id == native_id)),
                timeout=settings.SOURCE_TIMEOUT_SECONDS,
            )
            registry.mark_ok(profile.id)
            return result
        except NotFoundError as e:
            # 404 bukan salah sumbernya, jadi tidak memicu cooldown.
            errors[profile.id] = e.message
            not_found += 1
        except Exception as e:
            msg = e.message if isinstance(e, AppError) else f"{type(e).__name__}: {e}"
            errors[profile.id] = msg
            if op in EMPTY_OK and isinstance(e, ScrapingError) and "kosong" in msg.lower():
                empty_result = []  # pencarian tanpa hasil bukan kegagalan sumber
            else:
                registry.mark_fail(profile.id, msg)
            logger.warning(f"[{profile.id}] {op} gagal: {msg}")

    if op in EMPTY_OK and empty_result is not None:
        return empty_result
    if not_found == len(errors) and errors:
        raise NotFoundError("Data tidak ditemukan di semua sumber")
    err = ScrapingError("Semua sumber gagal")
    err.detail = {"errors": errors}
    raise err
