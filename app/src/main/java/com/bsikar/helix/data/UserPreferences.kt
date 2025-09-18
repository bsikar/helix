package com.bsikar.helix.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun fromString(value: String?): ThemeMode {
            return try {
                valueOf(value ?: SYSTEM.name)
            } catch (e: IllegalArgumentException) {
                SYSTEM
            }
        }
    }
}

class UserPreferences private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("helix_prefs", Context.MODE_PRIVATE)

    private val _fontSize = MutableStateFlow(prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE))
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _lineHeight = MutableStateFlow(prefs.getFloat(KEY_LINE_HEIGHT, DEFAULT_LINE_HEIGHT))
    val lineHeight: StateFlow<Float> = _lineHeight.asStateFlow()

    private val _onlyShowImages = MutableStateFlow(prefs.getBoolean(KEY_ONLY_SHOW_IMAGES, DEFAULT_ONLY_SHOW_IMAGES))
    val onlyShowImages: StateFlow<Boolean> = _onlyShowImages.asStateFlow()

    private val _themeMode =
        MutableStateFlow(ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE.name)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(Color(prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR.toArgb())))
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    private val _recentBooksCount = MutableStateFlow(prefs.getInt(KEY_RECENT_BOOKS_COUNT, DEFAULT_RECENT_BOOKS_COUNT))
    val recentBooksCount: StateFlow<Int> = _recentBooksCount.asStateFlow()

    fun setFontSize(size: Float) {
        _fontSize.value = size
        prefs.edit { putFloat(KEY_FONT_SIZE, size) }
    }

    fun setLineHeight(height: Float) {
        _lineHeight.value = height
        prefs.edit { putFloat(KEY_LINE_HEIGHT, height) }
    }

    fun setOnlyShowImages(show: Boolean) {
        _onlyShowImages.value = show
        prefs.edit { putBoolean(KEY_ONLY_SHOW_IMAGES, show) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun setAccentColor(color: Color) {
        _accentColor.value = color
        prefs.edit { putInt(KEY_ACCENT_COLOR, color.toArgb()) }
    }

    fun setRecentBooksCount(count: Int) {
        _recentBooksCount.value = count
        prefs.edit { putInt(KEY_RECENT_BOOKS_COUNT, count) }
    }

    companion object {
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_LINE_HEIGHT = "line_height"
        private const val KEY_ONLY_SHOW_IMAGES = "only_show_images"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_RECENT_BOOKS_COUNT = "recent_books_count"
        private const val DEFAULT_FONT_SIZE = 16f
        private const val DEFAULT_LINE_HEIGHT = 1.5f
        private const val DEFAULT_ONLY_SHOW_IMAGES = false
        private const val DEFAULT_RECENT_BOOKS_COUNT = 3
        val DEFAULT_THEME_MODE = ThemeMode.SYSTEM
        @Suppress("MagicNumber")
        val DEFAULT_ACCENT_COLOR = Color(0xFF6200EE) // Purple500

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

@Composable
fun rememberUserPreferences(): UserPreferences {
    val context = LocalContext.current
    return remember { UserPreferences.getInstance(context) }
}
