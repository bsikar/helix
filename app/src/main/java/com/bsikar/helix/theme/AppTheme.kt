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
    val isReaderOptimized: Boolean = false,
    // Standardized opacity levels
    val alphaDisabled: Float = 0.3f,
    val alphaMedium: Float = 0.5f,
    val alphaHigh: Float = 0.7f,
    val alphaOverlay: Float = 0.1f,
    val alphaSubtle: Float = 0.2f,
    // Status colors
    val successColor: Color = Color(0xFF4CAF50),
    val errorColor: Color = Color(0xFFFF5722),
    val warningColor: Color = Color(0xFFFF9800)
)