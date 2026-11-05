package com.neetrust.ui.config

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class ThemeSettings(
    val mode: ThemeMode = AppConfig.DEFAULT_THEME_MODE,
    val accent: AccentPreset = AppConfig.DEFAULT_ACCENT,
    val corner: CornerPreset = AppConfig.DEFAULT_CORNER,
    val glass: GlassPreset = AppConfig.DEFAULT_GLASS,
    val font: FontPreset = AppConfig.DEFAULT_FONT,
    val background: BackgroundPreset = AppConfig.DEFAULT_BACKGROUND,
    val animationsEnabled: Boolean = AppConfig.DEFAULT_ANIMATIONS_ENABLED
)

class ThemeStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var settings: ThemeSettings by mutableStateOf(load())
        private set

    fun update(transform: (ThemeSettings) -> ThemeSettings) {
        val next = transform(settings)
        settings = next
        save(next)
    }

    fun reset() = update { ThemeSettings() }

    private fun load(): ThemeSettings {
        val d = ThemeSettings()
        return ThemeSettings(
            mode = prefs.readEnum(KEY_MODE, d.mode),
            accent = prefs.readEnum(KEY_ACCENT, d.accent),
            corner = prefs.readEnum(KEY_CORNER, d.corner),
            glass = prefs.readEnum(KEY_GLASS, d.glass),
            font = prefs.readEnum(KEY_FONT, d.font),
            background = prefs.readEnum(KEY_BACKGROUND, d.background),
            animationsEnabled = prefs.getBoolean(KEY_ANIM, d.animationsEnabled)
        )
    }

    private fun save(s: ThemeSettings) {
        prefs.edit()
            .putString(KEY_MODE, s.mode.name)
            .putString(KEY_ACCENT, s.accent.name)
            .putString(KEY_CORNER, s.corner.name)
            .putString(KEY_GLASS, s.glass.name)
            .putString(KEY_FONT, s.font.name)
            .putString(KEY_BACKGROUND, s.background.name)
            .putBoolean(KEY_ANIM, s.animationsEnabled)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "neetrust_theme"
        const val KEY_MODE = "mode"
        const val KEY_ACCENT = "accent"
        const val KEY_CORNER = "corner"
        const val KEY_GLASS = "glass"
        const val KEY_FONT = "font"
        const val KEY_BACKGROUND = "background"
        const val KEY_ANIM = "animations"
    }
}

private inline fun <reified T : Enum<T>> SharedPreferences.readEnum(key: String, default: T): T =
    runCatching { enumValueOf<T>(getString(key, default.name) ?: default.name) }.getOrDefault(default)

fun Context.drawableIdByName(name: String): Int =
    resources.getIdentifier(name, "drawable", packageName)
