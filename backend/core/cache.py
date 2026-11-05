import time
import threading
import logging
from typing import Any, Optional

logger = logging.getLogger("core.cache")

class InMemoryCache:
    """Thread-safe In-Memory Cache with TTL (Time-To-Live)."""
    def __init__(self):
        self._storage: dict[str, tuple[Any, float]] = {}
        self._lock = threading.Lock()

    def get(self, key: str) -> Optional[Any]:
        with self._lock:
            item = self._storage.get(key)
            if not item:
                return None
            value, expiry = item
            if time.time() > expiry:
                del self._storage[key]
                logger.debug(f"Cache expired for key: {key}")
                return None
            logger.debug(f"Cache HIT for key: {key}")
            return value

    def set(self, key: str, value: Any, ttl: int = 300) -> None:
        with self._lock:
            expiry = time.time() + ttl
            self._storage[key] = (value, expiry)
            logger.debug(f"Cache SET for key: {key} (TTL: {ttl}s)")

    def delete(self, key: str) -> None:
        with self._lock:
            if key in self._storage:
                del self._storage[key]

    def clear(self) -> None:
        with self._lock:
            self._storage.clear()
            logger.info("Cache cleared completely")

cache = InMemoryCache()
