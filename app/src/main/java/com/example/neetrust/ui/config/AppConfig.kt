// [KATALOG] shared.ui.config.AppConfig
package com.example.neetrust.ui.config

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AppConfig: SATU PUSAT KONFIGURASI APLIKASI NEETRUST
 * Mengatur Base URL backend, logo, splash, border, avatar default, label akun, dan preferensi pengaturan.
 * Jangan hardcode nilai konfigurasi di luar file ini!
 */
object AppConfig {
    // === 1. JARINGAN & BACKEND ===
    // Ganti IP ini sesuai alamat server backend Anda (10.0.2.2 untuk Android Emulator default)
    const val BACKEND_BASE_URL: String = "http://10.0.2.2:8000"
    const val NETWORK_TIMEOUT_SECONDS: Long = 15L

    // === 2. IDENTITAS & BRANDING ===
    const val APP_NAME: String = "NeetRust"
    const val APP_TAGLINE: String = "Nonton Anime Tanpa Ribet & Cepat"
    const val APP_VERSION_LABEL: String = "v1.0.0 (Release)"
    
    // === 3. SPLASH SCREEN CONFIGURATION ===
    const val SPLASH_DURATION_MS: Long = 1800L
    const val SPLASH_TITLE: String = "NeetRust"
    const val SPLASH_SUBTITLE: String = "Katalog Anime & Streaming Cepat"

    // === 4. DEFAULT AVATAR & AKUN ===
    const val DEFAULT_AVATAR_URL: String = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=150"
    const val GUEST_USER_NAME: String = "Pengguna Tamu"
    const val GUEST_USER_BIO: String = "Menjelajah anime dalam mode tamu."
    const val GUEST_USER_ID: String = "guest_user"

    // === 5. DESAIN: BORDER & RADIUS (Mori Theme Style) ===
    val CARD_CORNER_RADIUS: Dp = 12.dp
    val BUTTON_CORNER_RADIUS: Dp = 10.dp
    val CHIP_CORNER_RADIUS: Dp = 8.dp
    val BORDER_WIDTH: Dp = 1.dp
    val BORDER_COLOR: Color = Color(0xFF2C3E45)

    // === 6. PREFERENSI PEMUTAR VIDEO (MEDIA3) ===
    const val DEFAULT_STREAM_QUALITY: String = "720p"
    val AVAILABLE_QUALITIES: List<String> = listOf("360p", "480p", "720p", "1080p")
    const val AUTO_PLAY_NEXT_EPISODE: Boolean = true
    const val SEEK_FORWARD_INCREMENT_MS: Long = 10000L // 10 detik
    const val SEEK_REWIND_INCREMENT_MS: Long = 10000L  // 10 detik

    // === 7. OPSI PENGATURAN (SETTINGS) ===
    val SERVER_OPTIONS: List<String> = listOf("Server Utama (HD)", "Server Cadangan 1", "Server Cadangan 2")
}
