# [KATALOG] db.database
import aiosqlite
import logging
from core.config import settings

logger = logging.getLogger("db.database")

async def get_db():
    db = await aiosqlite.connect(settings.SQLITE_DB_PATH)
    db.row_factory = aiosqlite.Row
    try:
        yield db
    finally:
        await db.close()

async def init_db():
    logger.info(f"Initializing SQLite database at: {settings.SQLITE_DB_PATH}")
    async with aiosqlite.connect(settings.SQLITE_DB_PATH) as db:
        # Users Table
        await db.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                bio TEXT DEFAULT '',
                avatar_url TEXT DEFAULT '',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        
        # Favorites Table
        await db.execute("""
            CREATE TABLE IF NOT EXISTS favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                anime_slug TEXT NOT NULL,
                title TEXT NOT NULL,
                poster TEXT,
                score TEXT,
                status TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, anime_slug),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """)

        # History Table
        await db.execute("""
            CREATE TABLE IF NOT EXISTS history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                anime_slug TEXT NOT NULL,
                anime_title TEXT NOT NULL,
                episode_slug TEXT NOT NULL,
                episode_number TEXT NOT NULL,
                poster TEXT,
                last_position_seconds INTEGER DEFAULT 0,
                duration_seconds INTEGER DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, episode_slug),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """)
        await db.commit()
    logger.info("Database initialized successfully.")
