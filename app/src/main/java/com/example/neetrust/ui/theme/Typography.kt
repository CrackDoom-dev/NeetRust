// [KATALOG] ui.theme.Typography
package com.example.neetrust.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.neetrust.ui.config.FontPreset

/**
 * Cari file font di res/font berdasarkan awalan preset.
 * Nama file yang dicari (huruf kecil):
 *   <prefix>_regular, <prefix>_medium, <prefix>_semibold, <prefix>_bold
 * Contoh: inter_regular.ttf. File yang tidak ada dilewati.
 * Kalau tidak ada satu pun, jatuh ke font sistem tanpa error.
 */
fun resolveFontFamily(context: Context, preset: FontPreset): FontFamily {
    val prefix = preset.resPrefix ?: return FontFamily.SansSerif
    val weights = listOf(
        "regular" to FontWeight.Normal,
        "medium" to FontWeight.Medium,
        "semibold" to FontWeight.SemiBold,
        "bold" to FontWeight.Bold
    )
    val fonts = weights.mapNotNull { (suffix, weight) ->
        val id = context.resources.getIdentifier("${prefix}_$suffix", "font", context.packageName)
        if (id != 0) Font(id, weight) else null
    }
    return if (fonts.isEmpty()) FontFamily.SansSerif else FontFamily(fonts)
}

/** Warna teks tidak diset di sini, mengikuti onSurface dari ColorScheme. */
fun neetRustTypography(family: FontFamily): Typography {
    val b = Typography()
    fun TextStyle.f() = copy(fontFamily = family)
    return Typography(
        displayLarge = b.displayLarge.f(),
        displayMedium = b.displayMedium.f(),
        displaySmall = b.displaySmall.f(),
        headlineLarge = b.headlineLarge.f().copy(
            fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp
        ),
        headlineMedium = b.headlineMedium.f().copy(
            fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp
        ),
        headlineSmall = b.headlineSmall.f(),
        titleLarge = b.titleLarge.f().copy(
            fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp
        ),
        titleMedium = b.titleMedium.f().copy(
            fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp
        ),
        titleSmall = b.titleSmall.f().copy(
            fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp
        ),
        bodyLarge = b.bodyLarge.f().copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = b.bodyMedium.f().copy(fontSize = 13.sp, lineHeight = 19.sp),
        bodySmall = b.bodySmall.f().copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = b.labelLarge.f().copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelMedium = b.labelMedium.f().copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall = b.labelSmall.f().copy(fontSize = 11.sp)
    )
}

/** Alias lama, dipertahankan agar kode lama tetap terkompilasi. */
val NeetRustTypography: Typography = neetRustTypography(FontFamily.SansSerif)
