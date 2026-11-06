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
import com.neetrust.domain.model.VideoServerDto
import com.neetrust.ui.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Klien API untuk wajik-anime-api (https://github.com/wajik45/wajik-anime-api).
 * Menyediakan metadata anime sub Indo (Otakudesu / Samehadaku) dan tautan video streaming.
 */
class WajikAnimeClient {
    private val logger = AppLogger.of("WajikAnimeClient")
    private val client = NetworkClient.externalHttpClient

    private suspend fun fetchJson(path: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (baseUrl in AppConfig.WAJIK_FALLBACK_URLS) {
            val url = "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
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
                            val json = JSONObject(body)
                            return@withContext Result.success(json)
                        }
                    } else {
                        lastError = Exception("HTTP ${response.code} dari $url")
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception("Gagal menghubungi wajik-anime-api"))
    }

    suspend fun getHome(): Result<HomeDataDto> = withContext(Dispatchers.IO) {
        val jsonResult = fetchJson("otakudesu/ongoing?page=1")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal memuat beranda"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val dataObj = root.optJSONObject("data")
            val animeArray = dataObj?.optJSONArray("animeList")
                ?: root.optJSONArray("data")
                ?: JSONArray()

            val banners = mutableListOf<HomeBannerDto>()
            val latest = mutableListOf<LatestEpisodeDto>()
            val popular = mutableListOf<AnimeDto>()

            for (i in 0 until animeArray.length()) {
                val item = animeArray.optJSONObject(i) ?: continue
                val title = item.optString("title", "Anime")
                val poster = item.optString("poster").ifBlank { item.optString("thumb") }
                val animeId = item.optString("animeId").ifBlank { item.optString("slug", "anime-$i") }
                val eps = item.optString("episodes").ifBlank { item.optString("episode", "1") }
                val releaseDay = item.optString("releaseDay").ifBlank { item.optString("latestReleaseDate", "Update") }

                val animeDto = AnimeDto(
                    title = title,
                    slug = animeId,
                    poster = poster,
                    type = "TV",
                    status = "Ongoing",
                    score = item.optString("score", "8.5"),
                    latestEpisode = eps
                )

                if (i < 3) {
                    banners.add(
                        HomeBannerDto(
                            title = title,
                            slug = animeId,
                            poster = poster,
                            genres = listOf("Aksi", "Petualangan", "Fantasi"),
                            synopsis = "Tonton $title episode terbaru sub Indo sekarang."
                        )
                    )
                }

                latest.add(
                    LatestEpisodeDto(
                        title = "$title Ep $eps",
                        episodeSlug = "$animeId-episode-$eps",
                        animeSlug = animeId,
                        poster = poster,
                        episodeNumber = eps,
                        releaseTime = releaseDay
                    )
                )

                popular.add(animeDto)
            }

            if (latest.isEmpty() && popular.isEmpty()) {
                return@withContext Result.failure(Exception("Data kosong dari wajik-anime-api"))
            }

            Result.success(
                HomeDataDto(
                    banners = banners,
                    latestEpisodes = latest,
                    popularAnime = popular,
                    source = "wajik-anime-api"
                )
            )
        } catch (e: Exception) {
            logger.e("Error parsing wajik home data", e)
            Result.failure(e)
        }
    }

    suspend fun getAnimeDetail(animeId: String): Result<AnimeDetailDto> = withContext(Dispatchers.IO) {
        val cleanId = animeId.trim()
        val jsonResult = fetchJson("otakudesu/anime/$cleanId")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal memuat detail"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val data = root.optJSONObject("data") ?: root

            val title = data.optString("title").ifBlank { cleanId.replace("-", " ").capitalize() }
            val poster = data.optString("poster").ifBlank { data.optString("thumb") }
            val synopsis = data.optString("synopsis").ifBlank { "Sinopsis belum tersedia." }
            val score = data.optString("score", "8.5")
            val status = data.optString("status", "Ongoing")
            val type = data.optString("type", "TV")
            val totalEpisodes = data.optString("total_episodes").ifBlank { data.optString("episodes", "?") }

            val genresList = mutableListOf<String>()
            val genresArr = data.optJSONArray("genres") ?: data.optJSONArray("genreList")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    val gObj = genresArr.optJSONObject(i)
                    val gName = gObj?.optString("name") ?: genresArr.optString(i)
                    if (gName.isNotBlank()) genresList.add(gName)
                }
            }

            val episodesList = mutableListOf<EpisodeListItemDto>()
            val epArr = data.optJSONArray("episodeList") ?: data.optJSONArray("episodes")
            if (epArr != null) {
                for (i in 0 until epArr.length()) {
                    val epObj = epArr.optJSONObject(i) ?: continue
                    val epTitle = epObj.optString("title", "Episode ${i + 1}")
                    val epId = epObj.optString("episodeId").ifBlank { epObj.optString("slug", "$cleanId-episode-${i + 1}") }
                    val epUploaded = epObj.optString("uploadedAt").ifBlank { epObj.optString("date") }
                    val epNum = Regex("""\d+""").find(epTitle)?.value ?: "${i + 1}"

                    episodesList.add(
                        EpisodeListItemDto(
                            title = epTitle,
                            slug = epId,
                            number = epNum,
                            uploadedAt = epUploaded
                        )
                    )
                }
            }

            Result.success(
                AnimeDetailDto(
                    title = title,
                    slug = cleanId,
                    poster = poster,
                    synopsis = synopsis,
                    score = score,
                    status = status,
                    type = type,
                    totalEpisodes = totalEpisodes,
                    genres = genresList.ifEmpty { listOf("Anime", "Sub Indo") },
                    episodes = episodesList,
                    source = "wajik-anime-api"
                )
            )
        } catch (e: Exception) {
            logger.e("Error parsing wajik detail", e)
            Result.failure(e)
        }
    }

    suspend fun getEpisodeStream(episodeSlug: String): Result<EpisodeStreamDto> = withContext(Dispatchers.IO) {
        val cleanSlug = episodeSlug.trim()
        val jsonResult = fetchJson("otakudesu/episode/$cleanSlug")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal memuat streaming"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val data = root.optJSONObject("data") ?: root

            val title = data.optString("title", cleanSlug.replace("-", " ").capitalize())
            val defaultUrl = data.optString("streamUrl").ifBlank { data.optString("defaultStreamingUrl") }
            val animeId = data.optString("animeId").ifBlank { cleanSlug.substringBefore("-episode-") }
            val epNumber = Regex("""\d+""").findAll(cleanSlug).lastOrNull()?.value ?: "1"

            val servers = mutableListOf<VideoServerDto>()
            val serversArr = data.optJSONArray("servers") ?: data.optJSONArray("serverList")
            if (serversArr != null) {
                for (i in 0 until serversArr.length()) {
                    val sObj = serversArr.optJSONObject(i) ?: continue
                    val name = sObj.optString("serverName").ifBlank { sObj.optString("name", "Server ${i + 1}") }
                    val id = sObj.optString("serverId").ifBlank { "srv_${i + 1}" }
                    val url = sObj.optString("streamUrl").ifBlank { sObj.optString("url") }
                    val quality = sObj.optString("quality", "720p")
                    if (url.isNotBlank()) {
                        val isEmbed = url.contains("embed") || url.contains("iframe") || !url.endsWith(".mp4")
                        servers.add(VideoServerDto(serverId = id, serverName = name, streamUrl = url, quality = quality, isEmbed = isEmbed))
                    }
                }
            }

            if (servers.isEmpty() && defaultUrl.isNotBlank()) {
                val isEmbed = defaultUrl.contains("embed") || defaultUrl.contains("iframe") || !defaultUrl.endsWith(".mp4")
                servers.add(VideoServerDto(serverId = "srv_main", serverName = "Server Utama", streamUrl = defaultUrl, quality = "720p", isEmbed = isEmbed))
            }

            val prevObj = data.optJSONObject("previousEpisode")
            val prevSlug = prevObj?.optString("episodeId")?.ifBlank { null }
            val nextObj = data.optJSONObject("nextEpisode")
            val nextSlug = nextObj?.optString("episodeId")?.ifBlank { null }

            Result.success(
                EpisodeStreamDto(
                    title = title,
                    episodeSlug = cleanSlug,
                    animeSlug = animeId,
                    animeTitle = animeId.replace("-", " ").capitalize(),
                    episodeNumber = epNumber,
                    defaultStreamUrl = servers.firstOrNull()?.streamUrl ?: defaultUrl,
                    servers = servers,
                    prevEpisodeSlug = prevSlug,
                    nextEpisodeSlug = nextSlug,
                    source = "wajik-anime-api"
                )
            )
        } catch (e: Exception) {
            logger.e("Error parsing wajik episode stream", e)
            Result.failure(e)
        }
    }

    suspend fun searchAnime(query: String): Result<List<AnimeDto>> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val jsonResult = fetchJson("otakudesu/search?q=$encoded")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal mencari anime"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val dataObj = root.optJSONObject("data")
            val listArr = dataObj?.optJSONArray("animeList")
                ?: root.optJSONArray("data")
                ?: JSONArray()

            val result = mutableListOf<AnimeDto>()
            for (i in 0 until listArr.length()) {
                val item = listArr.optJSONObject(i) ?: continue
                val title = item.optString("title", "Anime")
                val poster = item.optString("poster").ifBlank { item.optString("thumb") }
                val slug = item.optString("animeId").ifBlank { item.optString("slug", "anime-$i") }
                val score = item.optString("score", "N/A")
                val status = item.optString("status", "Ongoing")
                val type = item.optString("type", "TV")
                result.add(AnimeDto(title = title, slug = slug, poster = poster, score = score, status = status, type = type))
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOngoing(page: Int = 1): Result<AnimeListResponseDto> = withContext(Dispatchers.IO) {
        val jsonResult = fetchJson("otakudesu/ongoing?page=$page")
        if (jsonResult.isFailure) {
            return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Gagal memuat ongoing"))
        }

        try {
            val root = jsonResult.getOrThrow()
            val dataObj = root.optJSONObject("data")
            val listArr = dataObj?.optJSONArray("animeList")
                ?: root.optJSONArray("data")
                ?: JSONArray()

            val items = mutableListOf<AnimeDto>()
            for (i in 0 until listArr.length()) {
                val item = listArr.optJSONObject(i) ?: continue
                items.add(
                    AnimeDto(
                        title = item.optString("title", "Anime"),
                        slug = item.optString("animeId").ifBlank { item.optString("slug") },
                        poster = item.optString("poster").ifBlank { item.optString("thumb") },
                        score = item.optString("score", "8.0"),
                        status = "Ongoing",
                        type = "TV",
                        latestEpisode = item.optString("episodes")
                    )
                )
            }
            Result.success(AnimeListResponseDto(items = items, source = "wajik-anime-api"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
