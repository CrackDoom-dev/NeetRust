package com.neetrust.data.api

import com.neetrust.domain.model.AnimeDetailDto
import com.neetrust.domain.model.AnimeDto
import com.neetrust.domain.model.AnimeListResponseDto
import com.neetrust.domain.model.ProbeResultDto
import com.neetrust.domain.model.ScheduleResponseDto
import com.neetrust.domain.model.SourceAddResultDto
import com.neetrust.domain.model.SourceCreateDto
import com.neetrust.domain.model.SourceDto
import com.neetrust.domain.model.SourceOrderDto
import com.neetrust.domain.model.SourceUpdateDto
import com.neetrust.domain.model.EpisodeStreamDto
import com.neetrust.domain.model.FavoriteItemDto
import com.neetrust.domain.model.FavoriteRequestDto
import com.neetrust.domain.model.GoogleAuthRequestDto
import com.neetrust.domain.model.HistoryItemDto
import com.neetrust.domain.model.HistoryRequestDto
import com.neetrust.domain.model.HomeDataDto
import com.neetrust.domain.model.TokenResponseDto
import com.neetrust.domain.model.UserProfileDto
import com.neetrust.domain.model.UserProfileUpdateDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("home")
    suspend fun getHome(@Query("source") source: String? = null): HomeDataDto

    @GET("anime/{slug}")
    suspend fun getAnimeDetail(
        @Path("slug") slug: String,
        @Query("source") source: String? = null
    ): AnimeDetailDto

    @GET("episode/{slug}")
    suspend fun getEpisodeStream(
        @Path("slug") slug: String,
        @Query("source") source: String? = null
    ): EpisodeStreamDto

    @GET("search")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("source") source: String? = null
    ): List<AnimeDto>

    @GET("list")
    suspend fun getAnimeList(
        @Query("page") page: Int = 1,
        @Query("genre") genre: String? = null
    ): AnimeListResponseDto

    @GET("schedule")
    suspend fun getSchedule(@Query("source") source: String? = null): ScheduleResponseDto

    @GET("ongoing")
    suspend fun getOngoing(
        @Query("page") page: Int = 1,
        @Query("source") source: String? = null
    ): AnimeListResponseDto

    @GET("movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("source") source: String? = null
    ): AnimeListResponseDto

    @GET("sources")
    suspend fun getSources(): List<SourceDto>

    @POST("sources")
    suspend fun addSource(
        @Body body: SourceCreateDto,
        @Query("verify") verify: Boolean = true,
        @Query("force") force: Boolean = false
    ): SourceAddResultDto

    @POST("sources/probe")
    suspend fun probeSource(@Body body: SourceCreateDto): ProbeResultDto

    @PATCH("sources/{id}")
    suspend fun updateSource(
        @Path("id") id: String,
        @Body body: SourceUpdateDto
    ): SourceDto

    @DELETE("sources/{id}")
    suspend fun deleteSource(@Path("id") id: String): Map<String, Any>

    @PUT("sources/order")
    suspend fun reorderSources(@Body body: SourceOrderDto): List<SourceDto>

    @POST("sources/{id}/reset")
    suspend fun resetSource(@Path("id") id: String): SourceDto

    @POST("auth/google")
    suspend fun authWithGoogle(@Body request: GoogleAuthRequestDto): TokenResponseDto

    @GET("users/me")
    suspend fun getMyProfile(): UserProfileDto

    @PUT("users/me")
    suspend fun updateMyProfile(@Body update: UserProfileUpdateDto): UserProfileDto

    @GET("users/me/favorites")
    suspend fun getFavorites(): List<FavoriteItemDto>

    @POST("users/me/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequestDto): Map<String, Any>

    @DELETE("users/me/favorites/{slug}")
    suspend fun removeFavorite(@Path("slug") slug: String): Map<String, Any>

    @GET("users/me/history")
    suspend fun getHistory(): List<HistoryItemDto>

    @POST("users/me/history")
    suspend fun saveHistory(@Body request: HistoryRequestDto): Map<String, Any>

    @DELETE("users/me/history")
    suspend fun clearHistory(): Map<String, Any>
}
