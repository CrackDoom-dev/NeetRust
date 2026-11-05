# NeetRust Backend API (Samehadaku Scraper & User Service)

Backend REST API untuk aplikasi streaming anime NeetRust. Dibangun menggunakan **FastAPI**, **Scrapling** (dengan fallback **HTTPX + BeautifulSoup**), autentikasi Google OAuth & JWT, serta database **SQLite** untuk profil, favorit, dan riwayat tontonan.

---

## 🚀 Fitur Utama
1. **Scraping Samehadaku**: Mengambil data beranda (banner, episode terbaru, anime populer), katalog daftar anime, detail anime, pencarian, dan tautan streaming/embed episode.
2. **Scrapling & HTTPX Fallback**: Menggunakan Scrapling sebagai scraper cerdas, dan fallback otomatis ke HTTPX jika Scrapling menghadapi kendala.
3. **In-Memory TTL Cache**: Mengurangi beban scraping dan mempercepat respon server.
4. **Google Authentication & JWT**: Login dengan Google ID Token, pembuatan akun otomatis, serta penerbitan JWT Access Token & Refresh Token.
5. **Manajemen Pengguna**: Edit profil (nama, bio, avatar), bookmark favorit, dan pencatatan riwayat posisi tontonan video secara presisi.
6. **Error Handling Seragam**: Respon error selalu memiliki format konsisten: `{"error": "pesan", "code": "KODE_ERROR", "detail": {}}`.

---

## 🛠️ Variabel Lingkungan (.env)
Buat file `.env` di folder `backend/`:
```env
SAMEHADAKU_BASE_URL=https://samehadaku.email
JWT_SECRET_KEY=kunci-rahasia-jwt-produksi-anda
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=60
REFRESH_TOKEN_EXPIRE_DAYS=30
GOOGLE_CLIENT_ID=client-id-google-oauth-anda.apps.googleusercontent.com
SQLITE_DB_PATH=neetrust.db
CACHE_DEFAULT_TTL=300
```

---

## 📦 Cara Menjalankan

```bash
cd backend
pip install -r requirements.txt
python main.py
```
Akses Swagger UI dokumentasi di: `http://localhost:8000/docs`

---

## 📡 Dokumentasi Endpoint & Contoh Response

### 1. Beranda (Home)
- **Endpoint**: `GET /home`
- **Contoh Response**:
```json
{
  "banners": [
    {
      "title": "Sousou no Frieren",
      "slug": "sousou-no-frieren",
      "poster": "https://.../frieren.jpg",
      "genres": ["Adventure", "Drama", "Fantasy"],
      "synopsis": "Petualangan Frieren..."
    }
  ],
  "latest_episodes": [
    {
      "title": "Solo Leveling Episode 12",
      "episode_slug": "solo-leveling-episode-12",
      "anime_slug": "solo-leveling",
      "poster": "https://.../solo.jpg",
      "episode_number": "12",
      "release_time": "15 menit yang lalu"
    }
  ],
  "popular_anime": [
    {
      "title": "One Piece",
      "slug": "one-piece",
      "poster": "https://.../op.jpg",
      "type": "TV",
      "status": "Ongoing",
      "score": "8.89"
    }
  ]
}
```

### 2. Detail Anime
- **Endpoint**: `GET /anime/{slug}`
- **Contoh Response**:
```json
{
  "title": "Sousou no Frieren",
  "slug": "sousou-no-frieren",
  "poster": "https://.../poster.jpg",
  "synopsis": "Setelah mengalahkan Raja Iblis...",
  "type": "TV",
  "status": "Completed",
  "score": "9.14",
  "genres": ["Adventure", "Drama", "Fantasy"],
  "episodes": [
    {
      "title": "Episode 28",
      "slug": "sousou-no-frieren-episode-28",
      "number": "28",
      "uploaded_at": "22 Maret 2024"
    }
  ]
}
```

### 3. Pemutar Episode Video
- **Endpoint**: `GET /episode/{slug}`
- **Contoh Response**:
```json
{
  "title": "Sousou no Frieren Episode 28",
  "episode_slug": "sousou-no-frieren-episode-28",
  "anime_slug": "sousou-no-frieren",
  "episode_number": "28",
  "default_stream_url": "https://stream.server.com/embed/123",
  "servers": [
    {
      "server_id": "srv_1_2901",
      "server_name": "Server HD 1",
      "stream_url": "https://stream.server.com/embed/123",
      "quality": "720p",
      "is_embed": true
    },
    {
      "server_id": "srv_2_2902",
      "server_name": "Server 1080p",
      "stream_url": "https://stream.server.com/embed/456",
      "quality": "1080p",
      "is_embed": true
    }
  ],
  "download_links": [
    {
      "quality": "720p",
      "host_name": "Google Drive",
      "url": "https://drive.google.com/..."
    }
  ],
  "prev_episode_slug": "sousou-no-frieren-episode-27",
  "next_episode_slug": null
}
```

### 4. Katalog & Pencarian
- `GET /list?page=1&genre=action`
- `GET /search?q=naruto`

### 5. Autentikasi & Akun Pengguna
- `POST /auth/google` (Body: `{"id_token": "..."}`) -> mengembalikan `access_token` & `refresh_token`
- `POST /auth/refresh` (Body: `{"refresh_token": "..."}`)
- `GET /users/me` (Header: `Authorization: Bearer <token>`)
- `PUT /users/me` (Body: `{"name": "...", "bio": "...", "avatar_url": "..."}`)
- `GET /users/me/favorites` & `POST /users/me/favorites` & `DELETE /users/me/favorites/{slug}`
- `GET /users/me/history` & `POST /users/me/history` & `DELETE /users/me/history`
