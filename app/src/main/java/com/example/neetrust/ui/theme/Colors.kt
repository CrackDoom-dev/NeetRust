// [KATALOG] ui.theme.Colors
package com.example.neetrust.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.neetrust.ui.config.AccentPreset
import com.example.neetrust.ui.config.GlassPreset

/**
 * TOKEN WARNA NEETRUST
 * Nilai dasar diterjemahkan dari variables.css milik Mori:
 *   Dark : bg #141416, surface #1E1E22, primary #F8F8FA, teks sekunder #94949E
 *   Light: bg #FFFBF2, surface #F7F3E9, primary #1A1917, teks sekunder #6E6A64
 * Aksen VIOLET adalah tambahan untuk nuansa AnimeLovers (bukan dari Mori).
 */
@Immutable
data class NeetRustColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceHover: Color,
    val card: Color,          // surface + opasitas preset glass
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSubtle: Color,
    val textMain: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,        // garis halus (alpha preset glass)
    val borderStrong: Color,  // garis penuh ala Mori
    val divider: Color,
    val danger: Color,
    val warning: Color,       // bintang rating
    val overlay: Color,       // lapisan gelap di atas poster/player
    val glow: Color           // cahaya gradien di atas background (transparan jika MONO)
)

fun neetRustColors(isDark: Boolean, accent: AccentPreset, glass: GlassPreset): NeetRustColors {
    val bg = if (isDark) Color(0xFF141416) else Color(0xFFFFFBF2)
    val surface = if (isDark) Color(0xFF1E1E22) else Color(0xFFF7F3E9)
    val hover = if (isDark) Color(0xFF28282D) else Color(0xFFEFE9DA)
    val primary = if (isDark) Color(0xFFF8F8FA) else Color(0xFF1A1917)
    val onPrimary = if (isDark) Color(0xFF141416) else Color(0xFFFFFBF2)

    val isViolet = accent == AccentPreset.VIOLET
    val accentColor = if (isViolet) Color(0xFF6C63FF) else primary
    val onAccent = if (isViolet) Color(0xFFFFFFFF) else onPrimary
    val accentSubtle = when {
        isViolet && isDark -> Color(0xFF2A2660)
        isViolet -> Color(0xFFE4E1FF)
        else -> hover
    }
    val glow = when {
        isViolet && isDark -> Color(0xFF2B2670)
        isViolet -> Color(0xFFD9D4FF)
        else -> Color.Transparent
    }

    return NeetRustColors(
        isDark = isDark,
        bg = bg,
        surface = surface,
        surfaceHover = hover,
        card = surface.copy(alpha = glass.cardAlpha),
        primary = primary,
        onPrimary = onPrimary,
        accent = accentColor,
        onAccent = onAccent,
        accentSubtle = accentSubtle,
        textMain = primary,
        textSecondary = if (isDark) Color(0xFF94949E) else Color(0xFF6E6A64),
        textMuted = if (isDark) Color(0xFF6B6B74) else Color(0xFF9A958C),
        border = primary.copy(alpha = glass.borderAlpha),
        borderStrong = primary,
        divider = primary.copy(alpha = 0.12f),
        danger = Color(0xFFE5534B),
        warning = Color(0xFFE5A952),
        overlay = Color(0xCC0A0A0C),
        glow = glow
    )
}

/* =====================================================================
 * ALIAS KOMPATIBILITAS
 * Layar lama masih memakai nama ini. Nilainya statis (dark + monokrom),
 * jadi belum ikut berubah saat preset diganti. Hapus bertahap setelah
 * tiap layar pindah ke NeetRust.colors.
 * ===================================================================== */
val MoriBgDeep = Color(0xFF141416)
val MoriBgCard = Color(0xFF1E1E22)
val MoriBgSurface = Color(0xFF1E1E22)
val MoriBgSurfaceHover = Color(0xFF28282D)

val MoriEmeraldPrimary = Color(0xFFF8F8FA)   // sekarang = primary Mori (putih)
val MoriEmeraldLight = Color(0xFFFFFFFF)
val MoriEmeraldDark = Color(0xFF28282D)
val MoriEmeraldSubtle = Color(0xFF28282D)

val MoriAmberAccent = Color(0xFFE5A952)
val MoriAmberSubtle = Color(0xFF3B2E1C)
val MoriCoralAlert = Color(0xFFE5534B)

val MoriBorder = Color(0x38F8F8FA)
val MoriBorderFocused = Color(0xFFF8F8FA)
val MoriDivider = Color(0x1FF8F8FA)

val MoriTextPrimary = Color(0xFFF8F8FA)
val MoriTextSecondary = Color(0xFF94949E)
val MoriTextMuted = Color(0xFF6B6B74)
val MoriTextInverse = Color(0xFF141416)

val MoriPlayerOverlay = Color(0xCC0A0A0C)
val MoriPlayerControls = Color(0xFFF8F8FA)
