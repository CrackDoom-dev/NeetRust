import asyncio

from core.config import settings
from core.errors import AppError
from models.source import ProbeOp, ProbeResult
from scrapers.generic import GenericSource
from scrapers.profiles import SiteProfile


def _err(e: Exception) -> str:
    return (e.message if isinstance(e, AppError) else f"{type(e).__name__}: {e}")[:160]


async def _run(name: str, fn, count):
    try:
        result = await asyncio.wait_for(fn(), timeout=settings.SOURCE_TIMEOUT_SECONDS)
    except Exception as e:
        return name, None, ProbeOp(ok=False, error=_err(e))
    n = count(result)
    op = ProbeOp(ok=n > 0, count=n,
                 error=None if n > 0 else "Hasil kosong, selector atau URL perlu disesuaikan")
    return name, result, op


async def probe_profile(profile: SiteProfile) -> ProbeResult:
    """Uji semua operasi pada sebuah profil dan laporkan bagian mana yang berhasil di-parse."""
    src = GenericSource(profile.model_copy(update={"id": f"probe-{profile.id}"}))

    first = await asyncio.gather(
        _run("home", src.home, lambda r: len(r.banners) + len(r.latest_episodes) + len(r.popular_anime)),
        _run("list", src.list, lambda r: len(r.items)),
        _run("search", lambda: src.search("one"), lambda r: len(r)),
        _run("ongoing", src.ongoing, lambda r: len(r.items)),
        _run("movies", src.movies, lambda r: len(r.items)),
        _run("schedule", src.schedule, lambda r: sum(len(d.items) for d in r.days)),
    )
    ops: dict[str, ProbeOp] = {}
    results: dict = {}
    for name, result, op in first:
        ops[name] = op
        results[name] = result

    slug = None
    for key in ("list", "ongoing", "movies"):
        r = results.get(key)
        if r is not None and r.items:
            slug = r.items[0].slug
            break
    if not slug and results.get("home") is not None and results["home"].popular_anime:
        slug = results["home"].popular_anime[0].slug

    detail = None
    if slug:
        _, detail, ops["detail"] = await _run(
            "detail", lambda: src.detail(slug), lambda r: 1 if r.title else 0)
    else:
        ops["detail"] = ProbeOp(ok=False, error="Tidak ada anime untuk diuji")

    ep_slug = None
    if detail is not None and detail.episodes:
        ep_slug = detail.episodes[0].slug
    elif results.get("home") is not None and results["home"].latest_episodes:
        ep_slug = results["home"].latest_episodes[0].episode_slug
    if ep_slug:
        _, _, ops["episode"] = await _run(
            "episode", lambda: src.episode(ep_slug),
            lambda r: len(r.servers) + (1 if r.default_stream_url else 0) + len(r.download_links))
    else:
        ops["episode"] = ProbeOp(ok=False, error="Tidak ada episode untuk diuji")

    good = [k for k, v in ops.items() if v.ok]
    bad = [k for k, v in ops.items() if not v.ok]
    summary = f"Berhasil: {', '.join(good) or '-'}. Gagal: {', '.join(bad) or '-'}."
    return ProbeResult(ok=ops["home"].ok or ops["list"].ok, ops=ops, summary=summary)
