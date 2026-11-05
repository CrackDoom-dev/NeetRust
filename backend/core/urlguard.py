import asyncio
import ipaddress
import socket
from urllib.parse import urlparse
from core.config import settings

class UnsafeUrlError(ValueError):
    pass

def normalize_base_url(url: str) -> str:
    url = (url or "").strip()
    if not url:
        raise UnsafeUrlError("URL kosong")
    if "://" not in url:
        url = "https://" + url
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise UnsafeUrlError("URL harus diawali http:// atau https://")
    return f"{parsed.scheme}://{parsed.netloc}{parsed.path}".rstrip("/")

async def ensure_public_url(url: str) -> str:
    """Normalisasi URL dan tolak alamat lokal/privat (mencegah SSRF)."""
    clean = normalize_base_url(url)
    if settings.ALLOW_PRIVATE_SOURCES:
        return clean
    parsed = urlparse(clean)
    port = parsed.port or (443 if parsed.scheme == "https" else 80)
    try:
        infos = await asyncio.to_thread(socket.getaddrinfo, parsed.hostname, port)
    except socket.gaierror:
        raise UnsafeUrlError("Host tidak bisa di-resolve")
    for info in infos:
        ip = ipaddress.ip_address(info[4][0])
        if not ip.is_global:
            raise UnsafeUrlError("Alamat lokal/privat tidak diizinkan sebagai sumber")
    return clean
