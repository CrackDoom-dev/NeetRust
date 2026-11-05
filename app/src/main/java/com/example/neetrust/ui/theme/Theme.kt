// [KATALOG] shared.ui.theme.Theme
package com.example.neetrust.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MoriDarkColorScheme = darkColorScheme(
    primary = MoriEmeraldPrimary,
    onPrimary = MoriTextInverse,
    primaryContainer = MoriEmeraldDark,
    onPrimaryContainer = MoriEmeraldLight,
    secondary = MoriAmberAccent,
    onSecondary = MoriTextInverse,
    secondaryContainer = MoriAmberSubtle,
    onSecondaryContainer = MoriAmberAccent,
    background = MoriBgDeep,
    onBackground = MoriTextPrimary,
    surface = MoriBgCard,
    onSurface = MoriTextPrimary,
    surfaceVariant = MoriBgSurface,
    onSurfaceVariant = MoriTextSecondary,
    outline = MoriBorder,
    error = MoriCoralAlert,
    onError = MoriTextPrimary
)

@Composable
fun NeetRustTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MoriDarkColorScheme,
        typography = NeetRustTypography,
        shapes = NeetRustShapes,
        content = content
    )
}
