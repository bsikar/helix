package com.bsikar.helix.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class AppTheme(
    val backgroundColor: Color,
    val surfaceColor: Color,
    val primaryTextColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    // Material 3 color scheme
    val colorScheme: ColorScheme? = null,
    // Reader-specific colors
    val readerBackgroundColor: Color = backgroundColor,
    val readerTextColor: Color = primaryTextColor,
    val readerAccentColor: Color = accentColor,
    // Theme properties
    val isDynamic: Boolean = false,
    val isReaderOptimized: Boolean = false
)