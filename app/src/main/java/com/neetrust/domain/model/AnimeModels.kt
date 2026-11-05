package com.neetrust.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnimeDto(
    val title: String,
    val slug: String,
    val poster: String,
    val type: String? = "TV",
    val status: String? = "Ongoing",
    val score: String? = "N/A",
    @Json(name = "latest_episode") val latestEpisode: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    val source: String? = null
)

@JsonClass(generateAdapter = true)
data class HomeBannerDto(
    val title: String,
    val slug: String,
    val poster: String,
    val genres: List<String> = emptyList(),
    val synopsis: String? = null
)

@JsonClass(generateAdapter = true)
data class LatestEpisodeDto(
    val title: String,
    @Json(name = "episode_slug") val episodeSlug: String,
    @Json(name = "anime_slug") val animeSlug: String? = null,
    val poster: String,
    @Json(name = "episode_number") val episodeNumber: String,
    @Json(name = "release_time") val releaseTime: String
)

@JsonClass(generateAdapter = true)
data class HomeDataDto(
    val banners: List<HomeBannerDto> = emptyList(),
    @Json(name = "latest_episodes") val latestEpisodes: List<LatestEpisodeDto> = emptyList(),
    @Json(name = "popular_anime") val popularAnime: List<AnimeDto> = emptyList(),
    val source: String? = null
)

@JsonClass(generateAdapter = true)
data class EpisodeListItemDto(
    val title: String,
    val slug: String,
    val number: String,
    @Json(name = "uploaded_at") val uploadedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AnimeDetailDto(
    val title: String,
    val slug: String,
    val poster: String,
    val synopsis: String,
    @Json(name = "japanese_title") val japaneseTitle: String? = null,
    val type: String? = "TV",
    val status: String? = "Ongoing",
    val score: String? = "N/A",
    @Json(name = "total_episodes") val totalEpisodes: String? = "?",
    val duration: String? = "24 min",
    val season: String? = null,
    val studios: String? = null,
    val genres: List<String> = emptyList(),
    val episodes: List<EpisodeListItemDto> = emptyList(),
    val released: String? = null,
    val source: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoServerDto(
    @Json(name = "server_id") val serverId: String,
    @Json(name = "server_name") val serverName: String,
    @Json(name = "stream_url") val streamUrl: String,
    val quality: String = "720p",
    @Json(name = "is_embed") val isEmbed: Boolean = true
)

@JsonClass(generateAdapter = true)
data class DownloadLinkDto(
    val quality: String,
    @Json(name = "host_name") val hostName: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class EpisodeStreamDto(
    val title: String,
    @Json(name = "episode_slug") val episodeSlug: String,
    @Json(name = "anime_slug") val animeSlug: String,
    @Json(name = "anime_title") val animeTitle: String? = null,
    @Json(name = "episode_number") val episodeNumber: String,
    @Json(name = "default_stream_url") val defaultStreamUrl: String? = null,
    val servers: List<VideoServerDto> = emptyList(),
    @Json(name = "download_links") val downloadLinks: List<DownloadLinkDto> = emptyList(),
    @Json(name = "prev_episode_slug") val prevEpisodeSlug: String? = null,
    @Json(name = "next_episode_slug") val nextEpisodeSlug: String? = null,
    val source: String? = null
)

@JsonClass(generateAdapter = true)
data class AnimeListResponseDto(
    val items: List<AnimeDto> = emptyList(),
    val source: String? = null
)

@JsonClass(generateAdapter = true)
data class ScheduleItemDto(
    val title: String,
    val slug: String,
    val poster: String = "",
    val time: String? = null,
    val episode: String? = null,
    val type: String? = null,
    val score: String? = null
)

@JsonClass(generateAdapter = true)
data class ScheduleDayDto(
    val day: String,
    val items: List<ScheduleItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ScheduleResponseDto(
    val days: List<ScheduleDayDto> = emptyList(),
    val source: String? = null
)

@JsonClass(generateAdapter = true)
data class SourceDto(
    val id: String,
    val name: String,
    @Json(name = "base_url") val baseUrl: String,
    val preset: String = "wp-anime",
    val enabled: Boolean = true,
    val priority: Int = 100,
    val builtin: Boolean = false,
    val healthy: Boolean = true,
    @Json(name = "fail_count") val failCount: Int = 0,
    @Json(name = "cooldown_remaining") val cooldownRemaining: Int = 0,
    @Json(name = "last_error") val lastError: String? = null
)

@JsonClass(generateAdapter = true)
data class SourceCreateDto(
    val name: String? = null,
    @Json(name = "base_url") val baseUrl: String
)

@JsonClass(generateAdapter = true)
data class SourceUpdateDto(
    val enabled: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SourceOrderDto(
    val ids: List<String>
)

@JsonClass(generateAdapter = true)
data class ProbeOpDto(
    val ok: Boolean,
    val count: Int = 0,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ProbeResultDto(
    val ok: Boolean,
    val ops: Map<String, ProbeOpDto> = emptyMap(),
    val summary: String = ""
)

@JsonClass(generateAdapter = true)
data class SourceAddResultDto(
    val source: SourceDto,
    val probe: ProbeResultDto? = null
)
