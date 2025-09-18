package com.bsikar.helix.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bsikar.helix.data.ThemeMode
import com.bsikar.helix.data.rememberUserPreferences
import androidx.compose.ui.graphics.luminance

/**
 * Determines whether to use white or black text on the given color for optimal contrast
 */
@Suppress("UnusedParameter", "MagicNumber")
private fun getContrastingColor(backgroundColor: Color, isLightTheme: Boolean): Color {
    val luminance = backgroundColor.luminance()
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Suppress("MagicNumber")
private fun getLightColorScheme(accentColor: Color) = lightColorScheme(
    primary = accentColor,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color.White,
    onPrimary = getContrastingColor(accentColor, true),
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    primaryContainer = accentColor.copy(alpha = 0.12f),
    onPrimaryContainer = accentColor,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF5F5F5F)
)

@Suppress("MagicNumber")
private fun getDarkColorScheme(accentColor: Color) = darkColorScheme(
    primary = accentColor,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = getContrastingColor(accentColor, false),
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = accentColor.copy(alpha = 0.24f),
    onPrimaryContainer = accentColor,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFE0E0E0)
)

@Composable
@Suppress("FunctionNaming")
fun HelixTheme(
    content: @Composable () -> Unit
) {
    val userPreferences = rememberUserPreferences()
    val themeMode by userPreferences.themeMode.collectAsState()
    val accentColor by userPreferences.accentColor.collectAsState()

    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }

    // Never use dynamic colors when user has explicitly chosen an accent color
    // This ensures custom colors are always respected
    val useDynamicColor = false

    val colorScheme = when {
        useDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(accentColor)
        else -> getLightColorScheme(accentColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
