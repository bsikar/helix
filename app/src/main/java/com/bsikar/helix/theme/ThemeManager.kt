package com.bsikar.helix.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ThemeManager {
    val lightTheme = AppTheme(
        backgroundColor = Color(0xFFEFEBE3),
        surfaceColor = Color(0xFFE8E2D9),
        primaryTextColor = Color(0xFF3C3836),
        secondaryTextColor = Color(0xFF7C6F64),
        accentColor = Color(0xFFD79921)
    )

    val darkTheme = AppTheme(
        backgroundColor = Color(0xFF1D2021),
        surfaceColor = Color(0xFF282828),
        primaryTextColor = Color(0xFFF9F5D7),
        secondaryTextColor = Color(0xFFA89984),
        accentColor = Color(0xFFD79921)
    )
    
    @Composable
    fun getTheme(mode: ThemeMode): AppTheme {
        return when (mode) {
            ThemeMode.LIGHT -> lightTheme
            ThemeMode.DARK -> darkTheme
            ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) darkTheme else lightTheme
        }
    }
}