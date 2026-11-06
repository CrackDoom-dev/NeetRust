package com.neetrust.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.neetrust.auth.SessionStore
import com.neetrust.core.logger.AppLogger
import com.neetrust.data.api.ApiService
import com.neetrust.domain.model.AnimeDetailDto
import com.neetrust.domain.model.AnimeDto
import com.neetrust.domain.model.EpisodeStreamDto
import com.neetrust.domain.model.FavoriteItemDto
import com.neetrust.domain.model.FavoriteRequestDto
import com.neetrust.domain.model.HistoryItemDto
import com.neetrust.domain.model.HistoryRequestDto
import com.neetrust.domain.model.HomeBannerDto
import com.neetrust.domain.model.HomeDataDto
import com.neetrust.domain.model.LatestEpisodeDto
import com.neetrust.domain.model.VideoServerDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AnimeRepository(
    private val context: Context,
    private val apiService: ApiService,
    private val sessionStore: SessionStore
) {
    private val logger = AppLogger.of("AnimeRepository")
    private val localPrefs: SharedPreferences = context.getSharedPreferences("neetrust_local_data", Context.MODE_PRIVATE)

    private val wajikClient = com.neetrust.data.api.WajikAnimeClient()
    private val jikanClient = com.neetrust.data.api.JikanAnimeClient()

    private val _favorites = MutableStateFlow<List<FavoriteItemDto>>(emptyList())
    val favorites: StateFlow<List<FavoriteItemDto>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItemDto>>(emptyList())
    val history: StateFlow<List<HistoryItemDto>> = _history.asStateFlow()

    suspend fun getHomeData(): Result<HomeDataDto> = withContext(Dispatchers.IO) {
        // 1. Coba Wajik Anime API (Utama)
        wajikClient.getHome().onSuccess {
            logger.i("Berhasil memuat beranda dari Wajik Anime API")
            return@withContext Result.success(it)
        }

        // 2. Coba Jikan API (Cadangan)
        jikanClient.getHome().onSuccess {
            logger.i("Berhasil memuat beranda dari Jikan API (Cadangan)")
            return@withContext Result.success(it)
        }

        // 3. Coba local backend jika aktif
        try {
            val data = apiService.getHome()
            return@withContext Result.success(data)
        } catch (_: Exception) {}

        // 4. Katalog lokal
        logger.i("Memuat data katalog lokal.")
        Result.success(getFallbackHomeData())
    }

    suspend fun getAnimeDetail(slug: String): Result<AnimeDetailDto> = withContext(Dispatchers.IO) {
        // 1. Coba Wajik Anime API (Utama)
        wajikClient.getAnimeDetail(slug).onSuccess {
            logger.i("Berhasil memuat detail $slug dari Wajik Anime API")
            return@withContext Result.success(it)
        }

        // 2. Coba Jikan API (Cadangan)
        jikanClient.getAnimeDetail(slug).onSuccess {
            logger.i("Berhasil memuat detail $slug dari Jikan API (Cadangan)")
            return@withContext Result.success(it)
        }

        // 3. Coba local backend
        try {
            val data = apiService.getAnimeDetail(slug)
            return@withContext Result.success(data)
        } catch (_: Exception) {}

        // 4. Fallback lokal
        Result.success(getFallbackAnimeDetail(slug))
    }

    suspend fun getEpisodeStream(slug: String): Result<EpisodeStreamDto> = withContext(Dispatchers.IO) {
        // 1. Coba Wajik Anime API (Utama)
        wajikClient.getEpisodeStream(slug).onSuccess {
            logger.i("Berhasil memuat stream $slug dari Wajik Anime API")
            return@withContext Result.success(it)
        }

        // 2. Coba Jikan API (Cadangan)
        jikanClient.getEpisodeStream(slug).onSuccess {
            logger.i("Berhasil memuat stream $slug dari Jikan API (Cadangan)")
            return@withContext Result.success(it)
        }

        // 3. Coba local backend
        try {
            val data = apiService.getEpisodeStream(slug)
            return@withContext Result.success(data)
        } catch (_: Exception) {}

        // 4. Fallback lokal
        Result.success(getFallbackEpisodeStream(slug))
    }

    suspend fun searchAnime(query: String): Result<List<AnimeDto>> = withContext(Dispatchers.IO) {
        wajikClient.searchAnime(query).onSuccess { if (it.isNotEmpty()) return@withContext Result.success(it) }
        jikanClient.searchAnime(query).onSuccess { if (it.isNotEmpty()) return@withContext Result.success(it) }
        try {
            val data = apiService.searchAnime(query)
            if (data.isNotEmpty()) return@withContext Result.success(data)
        } catch (_: Exception) {}

        val filtered = getFallbackHomeData().popularAnime.filter {
            it.title.contains(query, ignoreCase = true)
        }
        Result.success(filtered)
    }

    suspend fun getAnimeCatalog(page: Int = 1, genre: String? = null): Result<List<AnimeDto>> = withContext(Dispatchers.IO) {
        wajikClient.getOngoing(page).onSuccess { if (it.items.isNotEmpty()) return@withContext Result.success(it.items) }
        jikanClient.getOngoing(page).onSuccess { if (it.items.isNotEmpty()) return@withContext Result.success(it.items) }
        try {
            val data = apiService.getAnimeList(page, genre)
            if (data.items.isNotEmpty()) return@withContext Result.success(data.items)
        } catch (_: Exception) {}
        Result.success(getFallbackHomeData().popularAnime)
    }

    suspend fun refreshFavorites() = withContext(Dispatchers.IO) {
        if (sessionStore.isGuestMode.value) {
            loadLocalFavorites()
            return@withContext
        }
        try {
            val list = apiService.getFavorites()
            _favorites.value = list
        } catch (e: Exception) {
            logger.w("Failed to fetch favorites from server, reading local store: ${e.message}")
            loadLocalFavorites()
        }
    }

    suspend fun addFavorite(animeSlug: String, title: String, poster: String, score: String, status: String) = withContext(Dispatchers.IO) {
        val newItem = FavoriteItemDto(
            id = (System.currentTimeMillis() % 100000).toInt(),
            animeSlug = animeSlug,
            title = title,
            poster = poster,
            score = score,
            status = status,
            createdAt = "Baru saja"
        )
        val current = _favorites.value.filter { it.animeSlug != animeSlug }.toMutableList()
        current.add(0, newItem)
        _favorites.value = current
        saveLocalFavorites(current)

        if (!sessionStore.isGuestMode.value) {
            try {
                apiService.addFavorite(FavoriteRequestDto(animeSlug, title, poster, score, status))
            } catch (e: Exception) {
                logger.w("Failed to sync favorite to server: ${e.message}")
            }
        }
    }

    suspend fun removeFavorite(animeSlug: String) = withContext(Dispatchers.IO) {
        val updated = _favorites.value.filter { it.animeSlug != animeSlug }
        _favorites.value = updated
        saveLocalFavorites(updated)

        if (!sessionStore.isGuestMode.value) {
            try {
                apiService.removeFavorite(animeSlug)
            } catch (e: Exception) {
                logger.w("Failed to sync remove favorite to server: ${e.message}")
            }
        }
    }

    fun isFavorite(animeSlug: String): Boolean {
        return _favorites.value.any { it.animeSlug == animeSlug }
    }

    suspend fun refreshHistory() = withContext(Dispatchers.IO) {
        if (sessionStore.isGuestMode.value) {
            loadLocalHistory()
            return@withContext
        }
        try {
            val list = apiService.getHistory()
            _history.value = list
        } catch (e: Exception) {
            logger.w("Failed to fetch history from server, reading local: ${e.message}")
            loadLocalHistory()
        }
    }

    suspend fun saveWatchHistory(
        animeSlug: String,
        animeTitle: String,
        episodeSlug: String,
        episodeNumber: String,
        poster: String,
        lastPositionSecs: Long,
        durationSecs: Long
    ) = withContext(Dispatchers.IO) {
        val item = HistoryItemDto(
            id = (System.currentTimeMillis() % 100000).toInt(),
            animeSlug = animeSlug,
            animeTitle = animeTitle,
            episodeSlug = episodeSlug,
            episodeNumber = episodeNumber,
            poster = poster,
            lastPositionSeconds = lastPositionSecs,
            durationSeconds = durationSecs,
            updatedAt = "Baru saja"
        )
        val current = _history.value.filter { it.episodeSlug != episodeSlug }.toMutableList()
        current.add(0, item)
        _history.value = current
        saveLocalHistory(current)

        localPrefs.edit().putLong("resume_pos_$episodeSlug", lastPositionSecs).apply()

        if (!sessionStore.isGuestMode.value) {
            try {
                apiService.saveHistory(
                    HistoryRequestDto(
                        animeSlug, animeTitle, episodeSlug, episodeNumber, poster, lastPositionSecs, durationSecs
                    )
                )
            } catch (e: Exception) {
                logger.w("Failed to sync history to server: ${e.message}")
            }
        }
    }

    fun getSavedPosition(episodeSlug: String): Long {
        return localPrefs.getLong("resume_pos_$episodeSlug", 0L)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        _history.value = emptyList()
        localPrefs.edit().remove("cached_history").apply()
        if (!sessionStore.isGuestMode.value) {
            try {
                apiService.clearHistory()
            } catch (e: Exception) {
                logger.w("Failed to clear server history: ${e.message}")
            }
        }
    }

    private fun loadLocalFavorites() {
        val raw = localPrefs.getString("cached_favs", "") ?: ""
        if (raw.isBlank()) return

        val items = raw.split(";;;").filter { it.isNotBlank() }.mapNotNull { block ->
            val parts = block.split("|||")
            if (parts.size >= 4) {
                FavoriteItemDto(
                    animeSlug = parts[0],
                    title = parts[1],
                    poster = parts[2],
                    score = parts.getOrNull(3) ?: "N/A",
                    status = parts.getOrNull(4) ?: "Ongoing",
                    createdAt = "Lokal"
                )
            } else null
        }
        _favorites.value = items
    }

    private fun saveLocalFavorites(items: List<FavoriteItemDto>) {
        val str = items.joinToString(";;;") { "${it.animeSlug}|||${it.title}|||${it.poster}|||${it.score}|||${it.status}" }
        localPrefs.edit().putString("cached_favs", str).apply()
    }

    private fun loadLocalHistory() {
        val raw = localPrefs.getString("cached_history", "") ?: ""
        if (raw.isBlank()) return
        val items = raw.split(";;;").filter { it.isNotBlank() }.mapNotNull { block ->
            val parts = block.split("|||")
            if (parts.size >= 7) {
                HistoryItemDto(
                    animeSlug = parts[0],
                    animeTitle = parts[1],
                    episodeSlug = parts[2],
                    episodeNumber = parts[3],
                    poster = parts[4],
                    lastPositionSeconds = parts[5].toLongOrNull() ?: 0L,
                    durationSeconds = parts[6].toLongOrNull() ?: 0L,
                    updatedAt = "Lokal"
                )
            } else null
        }
        _history.value = items
    }

    private fun saveLocalHistory(items: List<HistoryItemDto>) {
        val str = items.joinToString(";;;") {
            "${it.animeSlug}|||${it.animeTitle}|||${it.episodeSlug}|||${it.episodeNumber}|||${it.poster}|||${it.lastPositionSeconds}|||${it.durationSeconds}"
        }
        localPrefs.edit().putString("cached_history", str).apply()
    }

    private fun getFallbackHomeData(): HomeDataDto {
        val banners = listOf(
            HomeBannerDto(
                title = "Sousou no Frieren",
                slug = "sousou-no-frieren",
                poster = "https://cdn.myanimelist.net/images/anime/1015/138075l.jpg",
                genres = listOf("Petualangan", "Drama", "Fantasi"),
                synopsis = "Setelah mengalahkan Raja Iblis bersama pahlawan Himmel, penyihir elf Frieren memulai perjalanan panjang memahami arti kehidupan fana."
            ),
            HomeBannerDto(
                title = "Solo Leveling: Arise",
                slug = "solo-leveling",
                poster = "https://cdn.myanimelist.net/images/anime/1672/140381l.jpg",
                genres = listOf("Aksi", "Supernatural", "Fantasi"),
                synopsis = "Sung Jin-woo, hunter terlemah di dunia, tiba-tiba terbangun dengan sistem leveling misterius yang hanya bisa diakses oleh dirinya."
            )
        )
        val latest = listOf(
            LatestEpisodeDto("Solo Leveling Episode 12", "solo-leveling-episode-12", "solo-leveling", "https://cdn.myanimelist.net/images/anime/1672/140381l.jpg", "12", "10 mnt"),
            LatestEpisodeDto("Frieren Episode 28", "sousou-no-frieren-episode-28", "sousou-no-frieren", "https://cdn.myanimelist.net/images/anime/1015/138075l.jpg", "28", "30 mnt"),
            LatestEpisodeDto("One Piece Episode 1100", "one-piece-episode-1100", "one-piece", "https://cdn.myanimelist.net/images/anime/1244/138851l.jpg", "1100", "1 jam"),
            LatestEpisodeDto("Jujutsu Kaisen S2 Episode 23", "jujutsu-kaisen-s2-episode-23", "jujutsu-kaisen-s2", "https://cdn.myanimelist.net/images/anime/1792/138022l.jpg", "23", "3 jam")
        )
        val popular = listOf(
            AnimeDto("Sousou no Frieren", "sousou-no-frieren", "https://cdn.myanimelist.net/images/anime/1015/138075l.jpg", "TV", "Tamat", "9.14", "28"),
            AnimeDto("Solo Leveling", "solo-leveling", "https://cdn.myanimelist.net/images/anime/1672/140381l.jpg", "TV", "Ongoing", "8.75", "12"),
            AnimeDto("One Piece", "one-piece", "https://cdn.myanimelist.net/images/anime/1244/138851l.jpg", "TV", "Ongoing", "8.89", "1100"),
            AnimeDto("Jujutsu Kaisen", "jujutsu-kaisen-s2", "https://cdn.myanimelist.net/images/anime/1792/138022l.jpg", "TV", "Tamat", "8.90", "23")
        )
        return HomeDataDto(banners, latest, popular)
    }

    private fun getFallbackAnimeDetail(slug: String): AnimeDetailDto {
        val title = slug.replace("-", " ").replaceFirstChar { it.uppercase() }
        val poster = when {
            slug.contains("frieren", true) -> "https://cdn.myanimelist.net/images/anime/1015/138075l.jpg"
            slug.contains("solo", true) -> "https://cdn.myanimelist.net/images/anime/1672/140381l.jpg"
            slug.contains("piece", true) -> "https://cdn.myanimelist.net/images/anime/1244/138851l.jpg"
            slug.contains("jujutsu", true) -> "https://cdn.myanimelist.net/images/anime/1792/138022l.jpg"
            slug.contains("dungeon", true) -> "https://cdn.myanimelist.net/images/anime/1487/138656l.jpg"
            slug.contains("mashle", true) -> "https://cdn.myanimelist.net/images/anime/1922/140026l.jpg"
            slug.contains("mugen", true) -> "https://cdn.myanimelist.net/images/anime/1404/109375l.jpg"
            slug.contains("suzume", true) -> "https://cdn.myanimelist.net/images/anime/1710/124103l.jpg"
            slug.contains("kimi", true) -> "https://cdn.myanimelist.net/images/anime/1935/127974l.jpg"
            else -> "https://cdn.myanimelist.net/images/anime/1015/138075l.jpg"
        }

        val episodes = (1..12).map { ep ->
            com.neetrust.domain.model.EpisodeListItemDto(
                title = "Episode $ep",
                slug = "$slug-episode-$ep",
                number = "$ep",
                uploadedAt = "2024"
            )
        }.reversed()

        return AnimeDetailDto(
            title = title,
            slug = slug,
            poster = poster,
            synopsis = "Serial anime populer ini menceritakan petualangan epik dengan animasi memukau dan kisah karakter yang mendalam, menghadirkan nuansa emosional dan pertarungan sengit di setiap episodenya.",
            score = "8.95",
            status = "Ongoing",
            type = "TV",
            totalEpisodes = "12",
            genres = listOf("Aksi", "Petualangan", "Fantasi", "Drama"),
            episodes = episodes
        )
    }

    private fun getFallbackEpisodeStream(slug: String): EpisodeStreamDto {
        val animeSlug = if (slug.contains("-episode-")) slug.substringBefore("-episode-") else slug
        val epNum = if (slug.contains("-episode-")) slug.substringAfterLast("-episode-") else "1"
        val animeTitle = animeSlug.replace("-", " ").replaceFirstChar { it.uppercase() }

        val embedUrl = when {
            animeSlug.contains("frieren", true) -> "https://www.youtube.com/embed/ZEkwCGJ3o7M?autoplay=1"
            animeSlug.contains("solo", true) -> "https://www.youtube.com/embed/916hyC_Xw1E?autoplay=1"
            animeSlug.contains("piece", true) -> "https://www.youtube.com/embed/l_98K4_6UQk?autoplay=1"
            animeSlug.contains("jujutsu", true) -> "https://www.youtube.com/embed/O6qVieflwqs?autoplay=1"
            else -> "https://www.youtube.com/embed/ZEkwCGJ3o7M?autoplay=1"
        }

        val directStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"

        val servers = listOf(
            VideoServerDto("srv_1", "Server Utama (Direct HD)", directStreamUrl, "720p", isEmbed = false),
            VideoServerDto("srv_2", "Server Cadangan (Embed Web)", embedUrl, "HD", isEmbed = true),
            VideoServerDto("srv_3", "Server 1080p Ultra", directStreamUrl, "1080p", isEmbed = false)
        )

        return EpisodeStreamDto(
            title = "$animeTitle Episode $epNum",
            episodeSlug = slug,
            animeSlug = animeSlug,
            animeTitle = animeTitle,
            episodeNumber = epNum,
            defaultStreamUrl = directStreamUrl,
            servers = servers,
            prevEpisodeSlug = if ((epNum.toIntOrNull() ?: 1) > 1) "$animeSlug-episode-${(epNum.toIntOrNull() ?: 2) - 1}" else null,
            nextEpisodeSlug = "$animeSlug-episode-${(epNum.toIntOrNull() ?: 1) + 1}"
        )
    }
}
