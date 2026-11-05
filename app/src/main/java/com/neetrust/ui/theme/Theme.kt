package com.neetrust.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.neetrust.ui.config.AccentPreset
import com.neetrust.ui.config.AppConfig
import com.neetrust.ui.config.GlassPreset
import com.neetrust.ui.config.ThemeMode
import com.neetrust.ui.config.ThemeSettings

data class NeetRustTokens(
    val radiusL: Dp,
    val radiusXL: Dp,
    val borderWidth: Dp
)

val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

val LocalNeetColors = staticCompositionLocalOf {
    neetRustColors(true, AccentPreset.MONO, GlassPreset.SUBTLE)
}

val LocalNeetTokens = staticCompositionLocalOf {
    NeetRustTokens(
        radiusL = AppConfig.DEFAULT_CORNER.radiusL,
        radiusXL = AppConfig.DEFAULT_CORNER.radiusXL,
        borderWidth = AppConfig.BORDER_WIDTH
    )
}

object NeetRust {
    val colors: NeetRustColors
        @Composable @ReadOnlyComposable get() = LocalNeetColors.current

    val tokens: NeetRustTokens
        @Composable @ReadOnlyComposable get() = LocalNeetTokens.current

    val settings: ThemeSettings
        @Composable @ReadOnlyComposable get() = LocalThemeSettings.current
}

@Composable
fun NeetRustTheme(
    settings: ThemeSettings = LocalThemeSettings.current,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colors = remember(isDark, settings.accent, settings.glass) {
        neetRustColors(isDark, settings.accent, settings.glass)
    }
    val tokens = remember(settings.corner) {
        NeetRustTokens(
            radiusL = settings.corner.radiusL,
            radiusXL = settings.corner.radiusXL,
            borderWidth = AppConfig.BORDER_WIDTH
        )
    }
    val fontFamily = remember(settings.font) { resolveFontFamily(context, settings.font) }
    val typography = remember(fontFamily) { neetRustTypography(fontFamily) }
    val shapes = remember(settings.corner) { neetRustShapes(settings.corner) }

    val scheme = if (isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSubtle,
            onPrimaryContainer = colors.textMain,
            secondary = colors.warning,
            onSecondary = colors.bg,
            background = colors.bg,
            onBackground = colors.textMain,
            surface = colors.surface,
            onSurface = colors.textMain,
            surfaceVariant = colors.surfaceHover,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.danger,
            onError = colors.textMain
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSubtle,
            onPrimaryContainer = colors.textMain,
            secondary = colors.warning,
            onSecondary = colors.bg,
            background = colors.bg,
            onBackground = colors.textMain,
            surface = colors.surface,
            onSurface = colors.textMain,
            surfaceVariant = colors.surfaceHover,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.danger,
            onError = colors.bg
        )
    }

    CompositionLocalProvider(
        LocalThemeSettings provides settings,
        LocalNeetColors provides colors,
        LocalNeetTokens provides tokens
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
