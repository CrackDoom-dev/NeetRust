from fastapi import APIRouter, HTTPException, Query
from pydantic import ValidationError

from core.probe import probe_profile
from core.registry import registry
from core.urlguard import UnsafeUrlError, ensure_public_url
from models.source import (
    ProbeResult, SourceAddResult, SourceCreate, SourceInfo, SourceOrder, SourceUpdate,
)
from scrapers.profiles import PRESETS, SiteProfile

router = APIRouter(prefix="/sources", tags=["Sources"])


def _info(p: SiteProfile) -> SourceInfo:
    return SourceInfo(
        id=p.id, name=p.name, base_url=p.base_url, preset=p.preset, enabled=p.enabled,
        priority=p.priority, builtin=p.builtin, **registry.health(p.id),
    )


def _check_selectors(selectors: dict) -> None:
    try:
        import soupsieve
    except ImportError:
        return
    for key, value in selectors.items():
        try:
            soupsieve.compile(value)
        except Exception as e:
            raise HTTPException(400, f"Selector '{key}' tidak valid: {e}")


async def _clean_url(url: str) -> str:
    try:
        return await ensure_public_url(url)
    except UnsafeUrlError as e:
        raise HTTPException(400, str(e))


@router.get("", response_model=list[SourceInfo])
async def list_sources():
    """Daftar sumber berurutan sesuai prioritas beserta status kesehatannya."""
    return [_info(p) for p in registry.ordered()]


@router.post("/probe", response_model=ProbeResult)
async def probe_new(body: SourceCreate):
    """Uji sebuah situs tanpa menyimpannya."""
    if body.preset not in PRESETS:
        raise HTTPException(400, f"Preset tidak dikenal. Pilihan: {', '.join(PRESETS)}")
    _check_selectors(body.selectors)
    url = await _clean_url(body.base_url)
    profile = SiteProfile(id="tmp", name=body.name or url, base_url=url, preset=body.preset,
                          urls=body.urls, selectors=body.selectors)
    return await probe_profile(profile)


@router.post("", response_model=SourceAddResult)
async def add_source(
    body: SourceCreate,
    verify: bool = Query(True, description="Uji situs dulu sebelum disimpan"),
    force: bool = Query(False, description="Simpan walau hasil uji gagal"),
):
    if body.preset not in PRESETS:
        raise HTTPException(400, f"Preset tidak dikenal. Pilihan: {', '.join(PRESETS)}")
    _check_selectors(body.selectors)
    url = await _clean_url(body.base_url)
    if any(p.base_url.rstrip("/") == url for p in registry.ordered()):
        raise HTTPException(409, "Situs ini sudah ada di daftar sumber")

    host = url.split("://", 1)[1].split("/")[0]
    profile = SiteProfile(
        id=registry.unique_id(body.id or host.replace("www.", "").split(".")[0]),
        name=body.name or host, base_url=url, preset=body.preset,
        urls=body.urls, selectors=body.selectors,
    )
    probe = None
    if verify:
        probe = await probe_profile(profile)
        if not probe.ok and not force:
            raise HTTPException(422, f"Situs tidak bisa di-parse. {probe.summary} "
                                     "Sesuaikan selector/URL atau kirim ulang dengan force=true.")
    try:
        saved = await registry.add(profile)
    except (ValueError, ValidationError) as e:
        raise HTTPException(400, str(e))
    return SourceAddResult(source=_info(saved), probe=probe)


@router.put("/order", response_model=list[SourceInfo])
async def reorder_sources(body: SourceOrder):
    """Atur urutan prioritas. Sumber pertama dipakai lebih dulu."""
    await registry.reorder(body.ids)
    return [_info(p) for p in registry.ordered()]


@router.post("/{source_id}/probe", response_model=ProbeResult)
async def probe_existing(source_id: str):
    profile = registry.get(source_id)
    if not profile:
        raise HTTPException(404, "Sumber tidak ditemukan")
    return await probe_profile(profile)


@router.post("/{source_id}/reset", response_model=SourceInfo)
async def reset_source_health(source_id: str):
    profile = registry.get(source_id)
    if not profile:
        raise HTTPException(404, "Sumber tidak ditemukan")
    registry.reset_health(source_id)
    return _info(profile)


@router.patch("/{source_id}", response_model=SourceInfo)
async def update_source(source_id: str, body: SourceUpdate):
    profile = registry.get(source_id)
    if not profile:
        raise HTTPException(404, "Sumber tidak ditemukan")
    data = profile.model_dump()
    if body.name is not None:
        data["name"] = body.name
    if body.base_url is not None:
        data["base_url"] = await _clean_url(body.base_url)
    if body.enabled is not None:
        data["enabled"] = body.enabled
    if body.urls is not None:
        data["urls"] = body.urls
    if body.selectors is not None:
        _check_selectors(body.selectors)
        data["selectors"] = body.selectors
    updated = SiteProfile(**data)
    await registry.update(updated)
    registry.reset_health(source_id)
    return _info(updated)


@router.delete("/{source_id}")
async def delete_source(source_id: str):
    profile = registry.get(source_id)
    if not profile:
        raise HTTPException(404, "Sumber tidak ditemukan")
    if profile.builtin:
        raise HTTPException(400, "Sumber bawaan tidak bisa dihapus, nonaktifkan saja")
    await registry.remove(source_id)
    return {"deleted": source_id}
