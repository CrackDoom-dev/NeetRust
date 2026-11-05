import aiosqlite
import logging
from core.errors import NotFoundError
from users.models import (
    UserProfile, UserProfileUpdate,
    FavoriteRequest, FavoriteItem,
    HistoryRequest, HistoryItem
)

logger = logging.getLogger("users.service")

async def get_or_create_user(db: aiosqlite.Connection, user_data: dict) -> UserProfile:
    user_id = user_data["google_id"]
    email = user_data["email"]
    name = user_data.get("name", "Pengguna NeetRust")
    picture = user_data.get("picture", "")

    async with db.execute("SELECT id, email, name, bio, avatar_url FROM users WHERE id = ?", (user_id,)) as cursor:
        row = await cursor.fetchone()
        if row:
            return UserProfile(
                id=row["id"],
                email=row["email"],
                name=row["name"],
                bio=row["bio"],
                avatar_url=row["avatar_url"]
            )

    await db.execute(
        "INSERT INTO users (id, email, name, bio, avatar_url) VALUES (?, ?, ?, ?, ?)",
        (user_id, email, name, "", picture)
    )
    await db.commit()
    logger.info(f"Created new user: {user_id} ({email})")
    return UserProfile(id=user_id, email=email, name=name, bio="", avatar_url=picture)

async def get_user_by_id(db: aiosqlite.Connection, user_id: str) -> UserProfile:
    async with db.execute("SELECT id, email, name, bio, avatar_url FROM users WHERE id = ?", (user_id,)) as cursor:
        row = await cursor.fetchone()
        if not row:
            raise NotFoundError("Pengguna tidak ditemukan")
        return UserProfile(
            id=row["id"],
            email=row["email"],
            name=row["name"],
            bio=row["bio"],
            avatar_url=row["avatar_url"]
        )

async def update_user_profile(db: aiosqlite.Connection, user_id: str, update: UserProfileUpdate) -> UserProfile:
    user = await get_user_by_id(db, user_id)
    new_name = update.name if update.name is not None else user.name
    new_bio = update.bio if update.bio is not None else user.bio
    new_avatar = update.avatar_url if update.avatar_url is not None else user.avatar_url

    await db.execute(
        "UPDATE users SET name = ?, bio = ?, avatar_url = ? WHERE id = ?",
        (new_name, new_bio, new_avatar, user_id)
    )
    await db.commit()
    return UserProfile(id=user_id, email=user.email, name=new_name, bio=new_bio, avatar_url=new_avatar)

async def add_favorite(db: aiosqlite.Connection, user_id: str, req: FavoriteRequest) -> dict:
    await db.execute("""
        INSERT INTO favorites (user_id, anime_slug, title, poster, score, status)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(user_id, anime_slug) DO UPDATE SET
            title = excluded.title,
            poster = excluded.poster,
            score = excluded.score,
            status = excluded.status
    """, (user_id, req.anime_slug, req.title, req.poster, req.score, req.status))
    await db.commit()
    return {"message": "Berhasil ditambahkan ke favorit", "anime_slug": req.anime_slug}

async def get_favorites(db: aiosqlite.Connection, user_id: str) -> list[FavoriteItem]:
    async with db.execute(
        "SELECT id, anime_slug, title, poster, score, status, created_at FROM favorites WHERE user_id = ? ORDER BY id DESC",
        (user_id,)
    ) as cursor:
        rows = await cursor.fetchall()
        return [
            FavoriteItem(
                id=r["id"],
                anime_slug=r["anime_slug"],
                title=r["title"],
                poster=r["poster"],
                score=r["score"],
                status=r["status"],
                created_at=str(r["created_at"])
            )
            for r in rows
        ]

async def remove_favorite(db: aiosqlite.Connection, user_id: str, anime_slug: str) -> dict:
    await db.execute("DELETE FROM favorites WHERE user_id = ? AND anime_slug = ?", (user_id, anime_slug))
    await db.commit()
    return {"message": "Berhasil dihapus dari favorit", "anime_slug": anime_slug}

async def save_history(db: aiosqlite.Connection, user_id: str, req: HistoryRequest) -> dict:
    await db.execute("""
        INSERT INTO history (user_id, anime_slug, anime_title, episode_slug, episode_number, poster, last_position_seconds, duration_seconds, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(user_id, episode_slug) DO UPDATE SET
            last_position_seconds = excluded.last_position_seconds,
            duration_seconds = excluded.duration_seconds,
            updated_at = CURRENT_TIMESTAMP
    """, (
        user_id, req.anime_slug, req.anime_title, req.episode_slug, req.episode_number,
        req.poster, req.last_position_seconds, req.duration_seconds
    ))
    await db.commit()
    return {"message": "Riwayat menonton berhasil disimpan"}

async def get_history(db: aiosqlite.Connection, user_id: str) -> list[HistoryItem]:
    async with db.execute(
        "SELECT id, anime_slug, anime_title, episode_slug, episode_number, poster, last_position_seconds, duration_seconds, updated_at FROM history WHERE user_id = ? ORDER BY updated_at DESC",
        (user_id,)
    ) as cursor:
        rows = await cursor.fetchall()
        return [
            HistoryItem(
                id=r["id"],
                anime_slug=r["anime_slug"],
                anime_title=r["anime_title"],
                episode_slug=r["episode_slug"],
                episode_number=r["episode_number"],
                poster=r["poster"],
                last_position_seconds=r["last_position_seconds"],
                duration_seconds=r["duration_seconds"],
                updated_at=str(r["updated_at"])
            )
            for r in rows
        ]

async def clear_history(db: aiosqlite.Connection, user_id: str) -> dict:
    await db.execute("DELETE FROM history WHERE user_id = ?", (user_id,))
    await db.commit()
    return {"message": "Seluruh riwayat berhasil dibersihkan"}
