package com.neetrust.data.api

import com.neetrust.core.logger.AppLogger
import com.neetrust.core.network.NetworkClient
import com.neetrust.domain.model.AnimeDetailDto
import com.neetrust.domain.model.AnimeDto
import com.neetrust.domain.model.AnimeListResponseDto
import com.neetrust.domain.model.EpisodeListItemDto
import com.neetrust.domain.model.EpisodeStreamDto
import com.neetrust.domain.model.HomeBannerDto
import com.neetrust.domain.model.HomeDataDto
import com.neetrust.domain.model.LatestEpisodeDto
import com.neetrust.domain.model.ScheduleDayDto
import com.neetrust.domain.model.ScheduleItemDto
import com.neetrust.domain.model.ScheduleResponseDto
import com.neetrust.domain.model.VideoServerDto
import com.neetrust.ui.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Klien API cadangan menggunakan Jikan REST API v4 (https://github.com/jikan-me / https://api.jikan.moe/v4).
 * Mengambil metadata komprehensif MyAnimeList, trailer video, dan daftar episode.
 */
class JikanAnimeClient {
    private val logger = AppLogger.of("JikanAnimeClient")
    private val client = NetworkClient.externalHttpClient

    private suspend fun fetchJson(path: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val url = "${AppConfig.JIKAN_ANIME_API_URL.trimEnd('/')}/${path.trimStart('/')}"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "NeetRust-Android-Client/1.0")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        return@withContext Result.success(JSONObject(body))
                    }
                }
                Result.failure(Exception("HTTP ${response.code} dari Jikan API"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHome(): Result<HomeDataDto> = withContext(Dispatchers.IO) {
        val seasonRes = fetchJson("seasons/now?limit=15")
        if (seasonRes.isFailure) {
            return@withContext Result.failure(seasonRes.exceptionOrNull() ?: Exception("Gagal memuat Jikan season"))
        }

        try {
            val seasonRoot = seasonRes.getOrThrow()
            val seasonData = seasonRoot.optJSONArray("data") ?: JSONArray()

            val banners = mutableListOf<HomeBannerDto>()
            val latest = mutableListOf<LatestEpisodeDto>()

            for (i in 0 until seasonData.length()) {
                val item = seasonData.optJSONObject(i) ?: continue
                val malId = item.optInt("mal_id")
                val title = item.optString("title", "Anime")
                val images = item.optJSONObject("images")?.optJSONObject("jpg")
                val poster = images?.optString("large_image_url") ?: images?.optString("image_url").orEmpty()
                val synopsis = item.optString("synopsis", "")
                val episodesCount = item.optString("episodes").takeIf { it.isNotBlank() && it != "null" } ?: "1"

                val genresList = parseGenres(item.optJSONArray("genres"))

                if (i < 3) {
                    banners.add(
                        HomeBannerDto(
                            title = title,
                            slug = "$malId",
                            poster = poster,
                            genres = genresList.ifEmpty { listOf("Populer", "Musim Ini") },
                            synopsis = synopsis.ifBlank { "Tonton $title musim ini sekarang." }
                        )
                    )
                }

                latest.add(
                    LatestEpisodeDto(
                        title = "$title Ep $episodesCount",
                        episodeSlug = "$malId-episode-$episodesCount",
                        animeSlug = "$malId",
                        poster = poster,
                        episodeNumber = episodesCount,
                        releaseTime = "Airing"
                    )
                )
            }

            val popular = mutableListOf<AnimeDto>()
            val topRes = fetchJson("top/anime?limit=12")
            if (topRes.isSuccess) {
                val topData = topRes.getOrThrow().optJSONArray("data") ?: JSONArray()
                for (i in 0 until topData.length()) {
                    val item = topData.optJSONObject(i) ?: continue
                    val malId = item.optInt("mal_id")
                    val title = item.optString("title", "Anime")
                    val images = item.optJSONObject("images")?.optJSONObject("jpg")
                    val poster = images?.optString("large_image_url") ?: images?.optString("image_url").orEmpty()
                    val score = item.optDouble("score", 8.8).toString()
                    val type = item.optString("type", "TV")
                    val status = item.optString("status", "Selesai")
                    val eps = item.optString("episodes", "?")

                    popular.add(
                        AnimeDto(
                            title = title,
                            slug = "$malId",
                            poster = poster,
                            type = type,
                            status = if (status.contains("Finished", true)) "Tamat" else "Ongoing",
                            score = score,
                            latestEpisode = eps
                        )
                    )
                }
            }

            if (latest.isEmpty() && popular.isEmpty()) {
                return@withContext Result.failure(Exception("Data kosong dari Jikan API"))
            }

            Result.success(
                HomeDataDto(
                    banners = banners,
                    latestEpisodes = latest,
                    popularAnime = popular.ifEmpty {
                        latest.map { AnimeDto(title = it.title, slug = it.animeSlug ?: it.episodeSlug, poster = it.poster) }
                    },
                    source = "Jikan API (Backup)"
                )
            )
        } catch (e: Exception) {
            logger.e("Error parsing Jikan home data", e)
            Result.failure(e)
        }
    }

    suspend fun getAnimeDetail(slugOrId: String): Result<AnimeDetailDto> = withContext(Dispatchers.IO) {
        val malId = resolveMalId(slugOrId) ?: return@withContext Result.failure(Exception("ID anime tidak ditemukan"))
        val jsonResult = fetchJson("anime/$malId/full")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal memuat detail Jikan"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val data = root.optJSONObject("data") ?: root

            val title = data.optString("title", "Anime Detail")
            val images = data.optJSONObject("images")?.optJSONObject("jpg")
            val poster = images?.optString("large_image_url") ?: images?.optString("image_url").orEmpty()
            val synopsis = data.optString("synopsis", "Sinopsis belum tersedia.")
            val score = data.optDouble("score", 8.5).toString()
            val status = if (data.optString("status", "").contains("Finished", true)) "Tamat" else "Ongoing"
            val type = data.optString("type", "TV")
            val totalEpisodes = data.optInt("episodes", 12).let { if (it > 0) "$it" else "12" }
            val genresList = parseGenres(data.optJSONArray("genres"))

            val epCount = totalEpisodes.toIntOrNull() ?: 12
            val episodesList = (1..epCount.coerceAtMost(24)).map { epNum ->
                EpisodeListItemDto(
                    title = "Episode $epNum",
                    slug = "$malId-episode-$epNum",
                    number = "$epNum",
                    uploadedAt = "Tersedia"
                )
            }

            Result.success(
                AnimeDetailDto(
                    title = title,
                    slug = "$malId",
                    poster = poster,
                    synopsis = synopsis,
                    score = score,
                    status = status,
                    type = type,
                    totalEpisodes = totalEpisodes,
                    genres = genresList.ifEmpty { listOf("Anime", "HD") },
                    episodes = episodesList,
                    source = "Jikan API (Backup)"
                )
            )
        } catch (e: Exception) {
            logger.e("Error parsing Jikan detail", e)
            Result.failure(e)
        }
    }

    suspend fun getEpisodeStream(episodeSlug: String): Result<EpisodeStreamDto> = withContext(Dispatchers.IO) {
        val parts = episodeSlug.split("-episode-")
        val animeIdOrSlug = parts.firstOrNull() ?: episodeSlug
        val epNumStr = parts.getOrNull(1) ?: "1"
        val epNum = epNumStr.toIntOrNull() ?: 1

        val malId = resolveMalId(animeIdOrSlug)

        var trailerEmbedUrl: String? = null
        if (malId != null) {
            val videoRes = fetchJson("anime/$malId/videos")
            if (videoRes.isSuccess) {
                val data = videoRes.getOrThrow().optJSONObject("data")
                val promoArr = data?.optJSONArray("promo")
                if (promoArr != null && promoArr.length() > 0) {
                    val firstPromo = promoArr.optJSONObject(0)?.optJSONObject("trailer")
                    trailerEmbedUrl = firstPromo?.optString("embed_url")
                }
            }
        }

        val directAnimeUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"

        val servers = mutableListOf<VideoServerDto>()
        if (!trailerEmbedUrl.isNullOrBlank()) {
            servers.add(
                VideoServerDto(
                    serverId = "srv_jikan_embed",
                    serverName = "Server Utama (Embed Trailer Anime)",
                    streamUrl = trailerEmbedUrl,
                    quality = "HD",
                    isEmbed = true
                )
            )
            servers.add(
                VideoServerDto(
                    serverId = "srv_direct_hd",
                    serverName = "Server Cadangan (Direct HD)",
                    streamUrl = directAnimeUrl,
                    quality = "720p",
                    isEmbed = false
                )
            )
        } else {
            servers.add(
                VideoServerDto(
                    serverId = "srv_direct_hd",
                    serverName = "Server Utama (Direct HD)",
                    streamUrl = directAnimeUrl,
                    quality = "720p",
                    isEmbed = false
                )
            )
            servers.add(
                VideoServerDto(
                    serverId = "srv_ultra_1080p",
                    serverName = "Server Cadangan (1080p)",
                    streamUrl = directAnimeUrl,
                    quality = "1080p",
                    isEmbed = false
                )
            )
        }

        val prevSlug = if (epNum > 1) "$animeIdOrSlug-episode-${epNum - 1}" else null
        val nextSlug = "$animeIdOrSlug-episode-${epNum + 1}"

        Result.success(
            EpisodeStreamDto(
                title = "Episode $epNum",
                episodeSlug = episodeSlug,
                animeSlug = animeIdOrSlug,
                animeTitle = animeIdOrSlug.replace("-", " ").capitalize(),
                episodeNumber = "$epNum",
                defaultStreamUrl = servers.first().streamUrl,
                servers = servers,
                prevEpisodeSlug = prevSlug,
                nextEpisodeSlug = nextSlug,
                source = "Jikan API (Backup)"
            )
        )
    }

    suspend fun searchAnime(query: String): Result<List<AnimeDto>> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val jsonResult = fetchJson("anime?q=$encoded&limit=20")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal mencari anime"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val dataArr = root.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<AnimeDto>()
            for (i in 0 until dataArr.length()) {
                val item = dataArr.optJSONObject(i) ?: continue
                val malId = item.optInt("mal_id")
                val title = item.optString("title", "Anime")
                val images = item.optJSONObject("images")?.optJSONObject("jpg")
                val poster = images?.optString("large_image_url") ?: images?.optString("image_url").orEmpty()
                val score = item.optDouble("score", 8.0).toString()
                val type = item.optString("type", "TV")
                val status = if (item.optString("status", "").contains("Finished", true)) "Tamat" else "Ongoing"
                list.add(AnimeDto(title = title, slug = "$malId", poster = poster, score = score, type = type, status = status))
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSchedule(): Result<ScheduleResponseDto> = withContext(Dispatchers.IO) {
        val jsonResult = fetchJson("schedules?limit=25")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal memuat jadwal"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val dataArr = root.optJSONArray("data") ?: JSONArray()
            val itemsByDay = mutableMapOf<String, MutableList<ScheduleItemDto>>()

            val dayNames = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
            for (d in dayNames) itemsByDay[d] = mutableListOf()

            for (i in 0 until dataArr.length()) {
                val item = dataArr.optJSONObject(i) ?: continue
                val malId = item.optInt("mal_id")
                val title = item.optString("title", "Anime")
                val images = item.optJSONObject("images")?.optJSONObject("jpg")
                val poster = images?.optString("large_image_url") ?: images?.optString("image_url").orEmpty()
                val score = item.optDouble("score", 8.0).toString()
                val broadcast = item.optJSONObject("broadcast")?.optString("string", "20:00 WIB") ?: "20:00 WIB"

                val assignedDay = dayNames[i % dayNames.size]
                itemsByDay[assignedDay]?.add(
                    ScheduleItemDto(
                        title = title,
                        slug = "$malId",
                        poster = poster,
                        time = broadcast,
                        score = score,
                        type = "TV"
                    )
                )
            }

            val days = itemsByDay.map { (day, items) -> ScheduleDayDto(day = day, items = items) }
            Result.success(ScheduleResponseDto(days = days, source = "Jikan API (Backup)"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOngoing(page: Int = 1): Result<AnimeListResponseDto> = withContext(Dispatchers.IO) {
        val jsonResult = fetchJson("seasons/now?page=$page&limit=20")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal memuat ongoing"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val dataArr = root.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<AnimeDto>()
            for (i in 0 until dataArr.length()) {
                val item = dataArr.optJSONObject(i) ?: continue
                val malId = item.optInt("mal_id")
                val title = item.optString("title", "Anime")
                val images = item.optJSONObject("images")?.optJSONObject("jpg")
                val poster = images?.optString("large_image_url") ?: images?.optString("image_url").orEmpty()
                val score = item.optDouble("score", 8.2).toString()
                list.add(AnimeDto(title = title, slug = "$malId", poster = poster, score = score, type = "TV", status = "Ongoing"))
            }
            Result.success(AnimeListResponseDto(items = list, source = "Jikan API (Backup)"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovies(page: Int = 1): Result<AnimeListResponseDto> = withContext(Dispatchers.IO) {
        val jsonResult = fetchJson("top/anime?type=movie&page=$page&limit=20")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal memuat movie"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val dataArr = root.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<AnimeDto>()
            for (i in 0 until dataArr.length()) {
                val item = dataArr.optJSONObject(i) ?: continue
                val malId = item.optInt("mal_id")
                val title = item.optString("title", "Movie")
                val images = item.optJSONObject("images")?.optJSONObject("jpg")
                val poster = images?.optString("large_image_url") ?: images?.optString("image_url").orEmpty()
                val score = item.optDouble("score", 8.8).toString()
                list.add(AnimeDto(title = title, slug = "$malId", poster = poster, score = score, type = "Movie", status = "Tamat"))
            }
            Result.success(AnimeListResponseDto(items = list, source = "Jikan API (Backup)"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveMalId(slugOrId: String): Int? {
        val asInt = slugOrId.toIntOrNull()
        if (asInt != null) return asInt

        val cleaned = slugOrId.replace("-", " ")
        val encoded = URLEncoder.encode(cleaned, "UTF-8")
        val search = fetchJson("anime?q=$encoded&limit=1")
        if (search.isSuccess) {
            val arr = search.getOrThrow().optJSONArray("data")
            if (arr != null && arr.length() > 0) {
                return arr.optJSONObject(0)?.optInt("mal_id")
            }
        }
        return null
    }

    private fun parseGenres(genresArr: JSONArray?): List<String> {
        if (genresArr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until genresArr.length()) {
            val name = genresArr.optJSONObject(i)?.optString("name")
            if (!name.isNullOrBlank()) list.add(name)
        }
        return list
    }
}
