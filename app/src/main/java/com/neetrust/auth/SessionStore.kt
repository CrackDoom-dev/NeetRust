package com.neetrust.auth

import android.content.Context
import android.content.SharedPreferences
import com.neetrust.core.network.NetworkClient
import com.neetrust.domain.model.UserProfileDto
import com.neetrust.ui.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("neetrust_session", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfileDto?>(null)
    val currentUser: StateFlow<UserProfileDto?> = _currentUser.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    init {
        NetworkClient.setTokenProvider { getAccessToken() }
        loadSavedSession()
    }

    private fun loadSavedSession() {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, false)
        val userId = prefs.getString(KEY_USER_ID, null)
        val userName = prefs.getString(KEY_USER_NAME, null)
        val userEmail = prefs.getString(KEY_USER_EMAIL, null)
        val userBio = prefs.getString(KEY_USER_BIO, "") ?: ""
        val userAvatar = prefs.getString(KEY_USER_AVATAR, "") ?: ""

        if (isGuest || userId == AppConfig.GUEST_USER_ID) {
            _isGuestMode.value = true
            _currentUser.value = UserProfileDto(
                id = AppConfig.GUEST_USER_ID,
                email = "guest@neetrust.app",
                name = AppConfig.GUEST_USER_NAME,
                bio = AppConfig.GUEST_USER_BIO,
                avatarUrl = AppConfig.DEFAULT_AVATAR_URL
            )
        } else if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            _isGuestMode.value = false
            _currentUser.value = UserProfileDto(
                id = userId,
                email = userEmail ?: "",
                name = userName ?: "Pengguna",
                bio = userBio,
                avatarUrl = userAvatar
            )
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveAuthSession(token: String, refreshToken: String, user: UserProfileDto) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_BIO, user.bio)
            .putString(KEY_USER_AVATAR, user.avatarUrl)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()

        _isGuestMode.value = false
        _currentUser.value = user
    }

    fun setGuestSession() {
        val guest = UserProfileDto(
            id = AppConfig.GUEST_USER_ID,
            email = "guest@neetrust.app",
            name = AppConfig.GUEST_USER_NAME,
            bio = AppConfig.GUEST_USER_BIO,
            avatarUrl = AppConfig.DEFAULT_AVATAR_URL
        )
        prefs.edit()
            .putBoolean(KEY_IS_GUEST, true)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .putString(KEY_USER_ID, guest.id)
            .putString(KEY_USER_NAME, guest.name)
            .putString(KEY_USER_EMAIL, guest.email)
            .apply()

        _isGuestMode.value = true
        _currentUser.value = guest
    }

    fun updateUserProfile(user: UserProfileDto) {
        prefs.edit()
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_BIO, user.bio)
            .putString(KEY_USER_AVATAR, user.avatarUrl)
            .apply()
        _currentUser.value = user
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _currentUser.value = null
        _isGuestMode.value = false
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_BIO = "user_bio"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_IS_GUEST = "is_guest"
    }
}
