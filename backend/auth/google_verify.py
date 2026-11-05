import logging
from google.oauth2 import id_token
from google.auth.transport import requests
from core.config import settings
from core.errors import UnauthorizedError

logger = logging.getLogger("auth.google_verify")

async def verify_google_token(token_string: str) -> dict:
    """
    Verifikasi Google ID Token yang dikirim dari Android Credential Manager / Google Sign-In.
    Mengembalikan dictionary data profil pengguna (email, name, picture, sub).
    """
    try:
        request_adapter = requests.Request()
        audience = settings.GOOGLE_CLIENT_ID if settings.GOOGLE_CLIENT_ID else None

        id_info = id_token.verify_oauth2_token(
            token_string,
            request_adapter,
            audience=audience
        )

        if id_info.get("iss") not in ["accounts.google.com", "https://accounts.google.com"]:
            raise UnauthorizedError("Penerbit token Google tidak valid")

        return {
            "google_id": id_info.get("sub"),
            "email": id_info.get("email"),
            "name": id_info.get("name", "Pengguna"),
            "picture": id_info.get("picture", "")
        }
    except Exception as e:
        logger.error(f"Google Token Verification failed: {e}")
        raise UnauthorizedError(f"Verifikasi token Google gagal: {str(e)}")
