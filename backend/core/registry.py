import json
import logging
import os
import re
import time
from typing import Optional

import aiosqlite

from core.config import settings
from scrapers.profiles import PRESETS, SiteProfile

logger = logging.getLogger("core.registry")

DEFAULT_ID = "samehadaku"


def slugify_id(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-") or "sumber"


class SourceRegistry:
    """Daftar sumber website (tersimpan di SQLite) + status kesehatan untuk failover."""

    def __init__(self):
        self._profiles: dict[str, SiteProfile] = {}
        self._state: dict[str, dict] = {}

    # ---------- penyimpanan ----------
    async def load(self) -> None:
        async with aiosqlite.connect(settings.SQLITE_DB_PATH) as db:
            await db.execute("""
                CREATE TABLE IF NOT EXISTS sources (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    base_url TEXT NOT NULL,
                    preset TEXT NOT NULL DEFAULT 'wp-anime',
                    urls TEXT NOT NULL DEFAULT '{}',
                    selectors TEXT NOT NULL DEFAULT '{}',
                    enabled INTEGER NOT NULL DEFAULT 1,
                    priority INTEGER NOT NULL DEFAULT 100,
                    builtin INTEGER NOT NULL DEFAULT 0
                )
            """)
            cur = await db.execute("SELECT id FROM sources WHERE id = ?", (DEFAULT_ID,))
            exists = await cur.fetchone()
            if not exists:
                await db.execute(
                    "INSERT INTO sources (id, name, base_url, preset, enabled, priority, builtin) "
                    "VALUES (?, ?, ?, 'wp-anime', 1, 0, 1)",
                    (DEFAULT_ID, "Samehadaku", settings.SAMEHADAKU_BASE_URL.rstrip("/")),
                )
            elif os.getenv("SAMEHADAKU_BASE_URL"):
                await db.execute(
                    "UPDATE sources SET base_url = ? WHERE id = ?",
                    (settings.SAMEHADAKU_BASE_URL.rstrip("/"), DEFAULT_ID),
                )
            await db.commit()

            db.row_factory = aiosqlite.Row
            cur = await db.execute("SELECT * FROM sources ORDER BY priority ASC, rowid ASC")
            rows = await cur.fetchall()

        self._profiles.clear()
        for r in rows:
            self._profiles[r["id"]] = SiteProfile(
                id=r["id"], name=r["name"], base_url=r["base_url"], preset=r["preset"],
                urls=json.loads(r["urls"] or "{}"), selectors=json.loads(r["selectors"] or "{}"),
                enabled=bool(r["enabled"]), priority=r["priority"], builtin=bool(r["builtin"]),
            )
            self._state.setdefault(r["id"], {"fails": 0, "until": 0.0, "error": None})
        logger.info(f"Sumber dimuat: {[p.id for p in self.ordered()]}")

    async def _save(self, p: SiteProfile) -> None:
        async with aiosqlite.connect(settings.SQLITE_DB_PATH) as db:
            await db.execute(
                "INSERT INTO sources (id, name, base_url, preset, urls, selectors, enabled, priority, builtin) "
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                "ON CONFLICT(id) DO UPDATE SET name=excluded.name, base_url=excluded.base_url, "
                "preset=excluded.preset, urls=excluded.urls, selectors=excluded.selectors, "
                "enabled=excluded.enabled, priority=excluded.priority",
                (p.id, p.name, p.base_url, p.preset, json.dumps(p.urls), json.dumps(p.selectors),
                 int(p.enabled), p.priority, int(p.builtin)),
            )
            await db.commit()

    # ---------- akses ----------
    def ordered(self) -> list[SiteProfile]:
        return sorted(self._profiles.values(), key=lambda p: (p.priority, p.id))

    def get(self, source_id: str) -> Optional[SiteProfile]:
        return self._profiles.get(source_id)

    def unique_id(self, wanted: str) -> str:
        base = slugify_id(wanted)
        candidate, n = base, 2
        while candidate in self._profiles:
            candidate = f"{base}-{n}"
            n += 1
        return candidate

    async def add(self, profile: SiteProfile) -> SiteProfile:
        if profile.preset not in PRESETS:
            raise ValueError(f"Preset '{profile.preset}' tidak dikenal")
        profile.priority = max([p.priority for p in self._profiles.values()] + [0]) + 10
        self._profiles[profile.id] = profile
        self._state.setdefault(profile.id, {"fails": 0, "until": 0.0, "error": None})
        await self._save(profile)
        return profile

    async def update(self, profile: SiteProfile) -> None:
        self._profiles[profile.id] = profile
        await self._save(profile)

    async def remove(self, source_id: str) -> None:
        async with aiosqlite.connect(settings.SQLITE_DB_PATH) as db:
            await db.execute("DELETE FROM sources WHERE id = ? AND builtin = 0", (source_id,))
            await db.commit()
        self._profiles.pop(source_id, None)
        self._state.pop(source_id, None)

    async def reorder(self, ids: list[str]) -> None:
        known = [i for i in ids if i in self._profiles]
        rest = [p.id for p in self.ordered() if p.id not in known]
        for idx, sid in enumerate(known + rest):
            self._profiles[sid].priority = idx * 10
            await self._save(self._profiles[sid])

    # ---------- kesehatan / failover ----------
    def mark_ok(self, source_id: str) -> None:
        self._state[source_id] = {"fails": 0, "until": 0.0, "error": None}

    def mark_fail(self, source_id: str, error: str) -> None:
        st = self._state.setdefault(source_id, {"fails": 0, "until": 0.0, "error": None})
        st["fails"] += 1
        st["error"] = error[:200]
        delay = min(settings.SOURCE_COOLDOWN_SECONDS * (2 ** (st["fails"] - 1)), 1800)
        st["until"] = time.time() + delay

    def reset_health(self, source_id: str) -> None:
        self.mark_ok(source_id)

    def health(self, source_id: str) -> dict:
        st = self._state.get(source_id, {"fails": 0, "until": 0.0, "error": None})
        remaining = max(0, int(st["until"] - time.time()))
        return {"healthy": remaining == 0, "fail_count": st["fails"],
                "cooldown_remaining": remaining, "last_error": st["error"]}

    def candidates(self, preferred: Optional[str] = None, strict: bool = False) -> list[SiteProfile]:
        """Urutan percobaan: pilihan eksplisit dulu, lalu sesuai prioritas; sumber yang sedang cooldown di belakang."""
        now = time.time()
        enabled = [p for p in self.ordered() if p.enabled]
        ok = [p for p in enabled if self._state.get(p.id, {}).get("until", 0.0) <= now]
        cooling = sorted(
            [p for p in enabled if p not in ok],
            key=lambda p: self._state.get(p.id, {}).get("until", 0.0),
        )
        result = ok + cooling
        if preferred:
            pref = self._profiles.get(preferred)
            if pref:
                if strict:
                    return [pref]
                result = [pref] + [p for p in result if p.id != pref.id]
        return result


registry = SourceRegistry()
