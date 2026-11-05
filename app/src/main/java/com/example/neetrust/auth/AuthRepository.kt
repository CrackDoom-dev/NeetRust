// [KATALOG] shared.auth.AuthRepository
package com.example.neetrust.auth

import com.example.neetrust.core.logger.AppLogger
import com.example.neetrust.data.api.ApiService
import com.example.neetrust.domain.model.GoogleAuthRequestDto
import com.example.neetrust.domain.model.UserProfileDto
import com.example.neetrust.domain.model.UserProfileUpdateDto
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val apiService: ApiService,
    private val sessionStore: SessionStore
) {
    private val logger = AppLogger.of("AuthRepository")

    val currentUser: StateFlow<UserProfileDto?> = sessionStore.currentUser
    val isGuestMode: StateFlow<Boolean> = sessionStore.isGuestMode

    suspend fun loginWithGoogle(idToken: String): Result<UserProfileDto> {
        return try {
            logger.i("Attempting login with Google ID token")
            val resp = apiService.authWithGoogle(GoogleAuthRequestDto(idToken = idToken))
            sessionStore.saveAuthSession(resp.accessToken, resp.refreshToken, resp.user)
            logger.i("Google login success for user: ${resp.user.name}")
            Result.success(resp.user)
        } catch (e: Exception) {
            logger.e("Google login failed", e)
            Result.failure(e)
        }
    }

    fun continueAsGuest() {
        logger.i("User continuing as guest")
        sessionStore.setGuestSession()
    }

    suspend fun updateProfile(name: String?, bio: String?, avatarUrl: String?): Result<UserProfileDto> {
        return try {
            if (sessionStore.isGuestMode.value) {
                val current = sessionStore.currentUser.value ?: return Result.failure(Exception("Tidak ada pengguna"))
                val updated = current.copy(
                    name = name ?: current.name,
                    bio = bio ?: current.bio,
                    avatarUrl = avatarUrl ?: current.avatarUrl
                )
                sessionStore.updateUserProfile(updated)
                return Result.success(updated)
            }
            val updated = apiService.updateMyProfile(UserProfileUpdateDto(name = name, bio = bio, avatarUrl = avatarUrl))
            sessionStore.updateUserProfile(updated)
            Result.success(updated)
        } catch (e: Exception) {
            logger.e("Profile update failed", e)
            Result.failure(e)
        }
    }

    fun logout() {
        logger.i("Logging out user")
        sessionStore.clearSession()
    }
}
