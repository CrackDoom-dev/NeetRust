package com.neetrust.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GoogleAuthRequestDto(
    @Json(name = "id_token") val idToken: String
)

@JsonClass(generateAdapter = true)
data class TokenResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String = "Bearer",
    val user: UserProfileDto
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    val id: String,
    val email: String,
    val name: String,
    val bio: String = "",
    @Json(name = "avatar_url") val avatarUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class UserProfileUpdateDto(
    val name: String? = null,
    val bio: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class FavoriteRequestDto(
    @Json(name = "anime_slug") val animeSlug: String,
    val title: String,
    val poster: String = "",
    val score: String = "N/A",
    val status: String = "Ongoing"
)

@JsonClass(generateAdapter = true)
data class FavoriteItemDto(
    val id: Int = 0,
    @Json(name = "anime_slug") val animeSlug: String,
    val title: String,
    val poster: String = "",
    val score: String = "N/A",
    val status: String = "Ongoing",
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class HistoryRequestDto(
    @Json(name = "anime_slug") val animeSlug: String,
    @Json(name = "anime_title") val animeTitle: String,
    @Json(name = "episode_slug") val episodeSlug: String,
    @Json(name = "episode_number") val episodeNumber: String,
    val poster: String = "",
    @Json(name = "last_position_seconds") val lastPositionSeconds: Long = 0,
    @Json(name = "duration_seconds") val durationSeconds: Long = 0
)

@JsonClass(generateAdapter = true)
data class HistoryItemDto(
    val id: Int = 0,
    @Json(name = "anime_slug") val animeSlug: String,
    @Json(name = "anime_title") val animeTitle: String,
    @Json(name = "episode_slug") val episodeSlug: String,
    @Json(name = "episode_number") val episodeNumber: String,
    val poster: String = "",
    @Json(name = "last_position_seconds") val lastPositionSeconds: Long = 0,
    @Json(name = "duration_seconds") val durationSeconds: Long = 0,
    @Json(name = "updated_at") val updatedAt: String = ""
)
