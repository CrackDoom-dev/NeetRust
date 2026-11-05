// [KATALOG] shared.data.api.ApiService
package com.example.neetrust.data.api

import com.example.neetrust.domain.model.AnimeDetailDto
import com.example.neetrust.domain.model.AnimeDto
import com.example.neetrust.domain.model.AnimeListResponseDto
import com.example.neetrust.domain.model.EpisodeStreamDto
import com.example.neetrust.domain.model.FavoriteItemDto
import com.example.neetrust.domain.model.FavoriteRequestDto
import com.example.neetrust.domain.model.GoogleAuthRequestDto
import com.example.neetrust.domain.model.HistoryItemDto
import com.example.neetrust.domain.model.HistoryRequestDto
import com.example.neetrust.domain.model.HomeDataDto
import com.example.neetrust.domain.model.TokenResponseDto
import com.example.neetrust.domain.model.UserProfileDto
import com.example.neetrust.domain.model.UserProfileUpdateDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("home")
    suspend fun getHome(): HomeDataDto

    @GET("anime/{slug}")
    suspend fun getAnimeDetail(@Path("slug") slug: String): AnimeDetailDto

    @GET("episode/{slug}")
    suspend fun getEpisodeStream(@Path("slug") slug: String): EpisodeStreamDto

    @GET("search")
    suspend fun searchAnime(@Query("q") query: String): List<AnimeDto>

    @GET("list")
    suspend fun getAnimeList(
        @Query("page") page: Int = 1,
        @Query("genre") genre: String? = null
    ): AnimeListResponseDto

    // Auth
    @POST("auth/google")
    suspend fun authWithGoogle(@Body request: GoogleAuthRequestDto): TokenResponseDto

    // User Profile
    @GET("users/me")
    suspend fun getMyProfile(): UserProfileDto

    @PUT("users/me")
    suspend fun updateMyProfile(@Body update: UserProfileUpdateDto): UserProfileDto

    // Favorites
    @GET("users/me/favorites")
    suspend fun getFavorites(): List<FavoriteItemDto>

    @POST("users/me/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequestDto): Map<String, Any>

    @DELETE("users/me/favorites/{slug}")
    suspend fun removeFavorite(@Path("slug") slug: String): Map<String, Any>

    // History
    @GET("users/me/history")
    suspend fun getHistory(): List<HistoryItemDto>

    @POST("users/me/history")
    suspend fun saveHistory(@Body request: HistoryRequestDto): Map<String, Any>

    @DELETE("users/me/history")
    suspend fun clearHistory(): Map<String, Any>
}
