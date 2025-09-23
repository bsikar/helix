package com.bsikar.helix.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object ThemeManager {
    // Default Material 3 color schemes
    private val defaultLightColorScheme = lightColorScheme(
        primary = Color(0xFF6B4423),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF2D2A7),
        onPrimaryContainer = Color(0xFF241000),
        secondary = Color(0xFF6F5942),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF8DDBE),
        onSecondaryContainer = Color(0xFF251806),
        background = Color(0xFFFEFCF5),
        onBackground = Color(0xFF1E1B13),
        surface = Color(0xFFFEFCF5),
        onSurface = Color(0xFF1E1B13)
    )
    
    private val defaultDarkColorScheme = darkColorScheme(
        primary = Color(0xFFD4B68A),
        onPrimary = Color(0xFF3B2709),
        primaryContainer = Color(0xFF523515),
        onPrimaryContainer = Color(0xFFF2D2A7),
        secondary = Color(0xFFDBC1A3),
        onSecondary = Color(0xFF3C2D19),
        secondaryContainer = Color(0xFF55422D),
        onSecondaryContainer = Color(0xFFF8DDBE),
        background = Color(0xFF16130B),
        onBackground = Color(0xFFE9E1D4),
        surface = Color(0xFF16130B),
        onSurface = Color(0xFFE9E1D4)
    )

    // Legacy themes (for backward compatibility)
    val lightTheme = AppTheme(
        backgroundColor = Color(0xFFEFEBE3),
        surfaceColor = Color(0xFFE8E2D9),
        primaryTextColor = Color(0xFF3C3836),
        secondaryTextColor = Color(0xFF7C6F64),
        accentColor = Color(0xFFD79921),
        colorScheme = defaultLightColorScheme
    )

    val darkTheme = AppTheme(
        backgroundColor = Color(0xFF1D2021),
        surfaceColor = Color(0xFF282828),
        primaryTextColor = Color(0xFFF9F5D7),
        secondaryTextColor = Color(0xFFA89984),
        accentColor = Color(0xFFD79921),
        colorScheme = defaultDarkColorScheme
    )
    
    // Reader-specific themes
    private val sepiaTheme = AppTheme(
        backgroundColor = Color(0xFFF7F3E9),
        surfaceColor = Color(0xFFF0EBD8),
        primaryTextColor = Color(0xFF5D4E37),
        secondaryTextColor = Color(0xFF8B7355),
        accentColor = Color(0xFFB8860B),
        readerBackgroundColor = Color(0xFFF7F3E9),
        readerTextColor = Color(0xFF5D4E37),
        readerAccentColor = Color(0xFFB8860B),
        isReaderOptimized = true
    )
    
    private val highContrastTheme = AppTheme(
        backgroundColor = Color(0xFFFFFFFF),
        surfaceColor = Color(0xFFF5F5F5),
        primaryTextColor = Color(0xFF000000),
        secondaryTextColor = Color(0xFF333333),
        accentColor = Color(0xFF0066CC),
        readerBackgroundColor = Color(0xFFFFFFFF),
        readerTextColor = Color(0xFF000000),
        readerAccentColor = Color(0xFF0066CC),
        isReaderOptimized = true
    )
    
    private val nightModeTheme = AppTheme(
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF121212),
        primaryTextColor = Color(0xFFE0E0E0),
        secondaryTextColor = Color(0xFFBBBBBB),
        accentColor = Color(0xFF66BB6A),
        readerBackgroundColor = Color(0xFF000000),
        readerTextColor = Color(0xFFE0E0E0),
        readerAccentColor = Color(0xFF66BB6A),
        isReaderOptimized = true
    )
    
    private val warmTheme = AppTheme(
        backgroundColor = Color(0xFFFFF8E1),
        surfaceColor = Color(0xFFFFF3C4),
        primaryTextColor = Color(0xFF4E342E),
        secondaryTextColor = Color(0xFF6D4C41),
        accentColor = Color(0xFFFF8F00),
        readerBackgroundColor = Color(0xFFFFF8E1),
        readerTextColor = Color(0xFF4E342E),
        readerAccentColor = Color(0xFFFF8F00),
        isReaderOptimized = true
    )
    
    private val coolTheme = AppTheme(
        backgroundColor = Color(0xFFE8F4FD),
        surfaceColor = Color(0xFFE1F5FE),
        primaryTextColor = Color(0xFF0D47A1),
        secondaryTextColor = Color(0xFF1565C0),
        accentColor = Color(0xFF2196F3),
        readerBackgroundColor = Color(0xFFE8F4FD),
        readerTextColor = Color(0xFF0D47A1),
        readerAccentColor = Color(0xFF2196F3),
        isReaderOptimized = true
    )
    
    @Composable
    fun getTheme(mode: ThemeMode): AppTheme {
        val context = LocalContext.current
        val isDarkTheme = isSystemInDarkTheme()
        
        return when (mode) {
            ThemeMode.LIGHT -> lightTheme
            ThemeMode.DARK -> darkTheme
            ThemeMode.SYSTEM -> if (isDarkTheme) darkTheme else lightTheme
            ThemeMode.DYNAMIC -> getDynamicTheme(context, isDarkTheme)
            ThemeMode.SEPIA -> sepiaTheme
            ThemeMode.HIGH_CONTRAST -> highContrastTheme
            ThemeMode.NIGHT_MODE -> nightModeTheme
            ThemeMode.WARM -> warmTheme
            ThemeMode.COOL -> coolTheme
        }
    }
    
    @Composable
    private fun getDynamicTheme(context: Context, isDarkTheme: Boolean): AppTheme {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamicColorScheme = if (isDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            
            AppTheme(
                backgroundColor = dynamicColorScheme.background,
                surfaceColor = dynamicColorScheme.surface,
                primaryTextColor = dynamicColorScheme.onBackground,
                secondaryTextColor = dynamicColorScheme.onSurfaceVariant,
                accentColor = dynamicColorScheme.primary,
                colorScheme = dynamicColorScheme,
                isDynamic = true
            )
        } else {
            // Fallback to system theme for older Android versions
            if (isDarkTheme) darkTheme else lightTheme
        }
    }
    
    /**
     * Get all available theme modes
     */
    fun getAllThemeModes(): List<ThemeMode> = ThemeMode.values().toList()
    
    /**
     * Get reader-optimized theme modes
     */
    fun getReaderThemeModes(): List<ThemeMode> = listOf(
        ThemeMode.LIGHT,
        ThemeMode.DARK,
        ThemeMode.SEPIA,
        ThemeMode.HIGH_CONTRAST,
        ThemeMode.NIGHT_MODE,
        ThemeMode.WARM,
        ThemeMode.COOL
    )
    
    /**
     * Check if dynamic colors are supported
     */
    fun isDynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}