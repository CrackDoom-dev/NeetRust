// [KATALOG] ui.config.AppConfig
package com.example.neetrust.ui.config

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* =====================================================================
 * PRESET TEMA (diterjemahkan dari sistem preset Mori ke Kotlin)
 * ===================================================================== */

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** MONO = aksen sama dengan warna primary (gaya Mori). VIOLET = indigo ala AnimeLovers. */
enum class AccentPreset(val label: String) { MONO("Monokrom"), VIOLET("Violet") }

/** radiusL = chip/tombol/kartu kecil, radiusXL = kartu besar/dialog. */
enum class CornerPreset(val label: String, val radiusL: Dp, val radiusXL: Dp) {
    SHARP("Tajam", 3.dp, 4.dp),
    MODERN("Modern", 6.dp, 10.dp),
    ROUND("Bulat", 16.dp, 24.dp)
}

/** cardAlpha = opasitas kartu, borderAlpha = opasitas garis tepi. */
enum class GlassPreset(val label: String, val cardAlpha: Float, val borderAlpha: Float) {
    OFF("Mati", 1f, 0.18f),
    SUBTLE("Halus", 0.92f, 0.18f),
    DEEP("Dalam", 0.84f, 0.22f)
}

/**
 * resPrefix = awalan nama file font di res/font (huruf kecil).
 * Contoh: inter_regular.ttf, inter_medium.ttf, inter_semibold.ttf, inter_bold.ttf
 * Kalau file belum ada, otomatis jatuh ke font sistem (tidak error).
 */
enum class FontPreset(val label: String, val resPrefix: String?) {
    SYSTEM("Sistem", null),
    INTER("Inter", "inter"),
    JAKARTA("Plus Jakarta Sans", "jakarta"),
    OUTFIT("Outfit", "outfit"),
    MONTSERRAT("Montserrat", "montserrat")
}

enum class BackgroundPreset(val label: String) {
    STARS("Bintang"),
    WAVES("Gelombang"),
    FIREFLIES("Kunang-kunang"),
    NONE("Polos")
}

/* =====================================================================
 * SATU PUSAT KONFIGURASI NEETRUST
 * Semua pilihan desain, identitas, dan jaringan diatur dari sini.
 * ===================================================================== */
object AppConfig {

    // === 1. JARINGAN & BACKEND ===
    // 10.0.2.2 hanya untuk emulator. Di HP asli ganti ke IP/domain backend.
    const val BACKEND_BASE_URL: String = "http://10.0.2.2:8000"
    const val NETWORK_TIMEOUT_SECONDS: Long = 15L

    // === 2. IDENTITAS & BRANDING ===
    const val APP_NAME: String = "NeetRust"
    const val APP_TAGLINE: String = "Nonton anime sub Indo, tanpa ribet."
    const val APP_VERSION_LABEL: String = "v1.0.0 (Release)"

    // === 3. LOGO & ILUSTRASI (nama file drawable, tanpa ekstensi) ===
    // Taruh file di res/drawable. Kalau belum ada, layar memakai ikon cadangan.
    const val LOGO_DRAWABLE_NAME: String = "ic_neetrust_logo"
    const val SPLASH_ILLUSTRATION_NAME: String = "img_splash_mascot"
    const val DEFAULT_AVATAR_NAME: String = "img_default_avatar"

    // === 4. SPLASH ===
    const val SPLASH_DURATION_MS: Long = 1800L
    const val SPLASH_TITLE: String = "NeetRust"
    const val SPLASH_SUBTITLE: String = "Nonton anime sub Indo, tanpa ribet."

    // === 5. AKUN & AVATAR ===
    // DEFAULT_AVATAR_URL masih dipakai SessionStore. Ganti ke DEFAULT_AVATAR_NAME
    // setelah layar profil dimigrasi.
    const val DEFAULT_AVATAR_URL: String = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=150"
    const val GUEST_USER_NAME: String = "Pengguna Tamu"
    const val GUEST_USER_BIO: String = "Menjelajah anime dalam mode tamu."
    const val GUEST_USER_ID: String = "guest_user"
    val AVATAR_RING_WIDTH: Dp = 2.5.dp

    // === 6. DEFAULT TEMA (pengguna bisa ubah di Settings, tersimpan di ThemeStore) ===
    val DEFAULT_THEME_MODE: ThemeMode = ThemeMode.DARK
    val DEFAULT_ACCENT: AccentPreset = AccentPreset.MONO
    val DEFAULT_CORNER: CornerPreset = CornerPreset.MODERN
    val DEFAULT_GLASS: GlassPreset = GlassPreset.SUBTLE
    val DEFAULT_FONT: FontPreset = FontPreset.SYSTEM
    val DEFAULT_BACKGROUND: BackgroundPreset = BackgroundPreset.STARS
    const val DEFAULT_ANIMATIONS_ENABLED: Boolean = true

    // === 7. BORDER ===
    // Mori memakai garis 1.5 tebal.
    val BORDER_WIDTH: Dp = 1.5.dp

    // === 8. KOMPATIBILITAS LAYAR LAMA ===
    // Layar yang belum dimigrasi masih membaca nilai statis ini.
    // Layar baru: pakai NeetRust.tokens dan NeetRust.colors dari ui/theme/Theme.kt.
    val CARD_CORNER_RADIUS: Dp = 10.dp
    val BUTTON_CORNER_RADIUS: Dp = 6.dp
    val CHIP_CORNER_RADIUS: Dp = 6.dp
    val BORDER_COLOR: Color = Color(0x38F8F8FA)

    // === 9. PEMUTAR VIDEO (MEDIA3) ===
    const val DEFAULT_STREAM_QUALITY: String = "720p"
    val AVAILABLE_QUALITIES: List<String> = listOf("360p", "480p", "720p", "1080p")
    const val AUTO_PLAY_NEXT_EPISODE: Boolean = true
    const val SEEK_FORWARD_INCREMENT_MS: Long = 10000L
    const val SEEK_REWIND_INCREMENT_MS: Long = 10000L

    // === 10. OPSI SETTINGS ===
    val SERVER_OPTIONS: List<String> = listOf("Server Utama (HD)", "Server Cadangan 1", "Server Cadangan 2")
}
