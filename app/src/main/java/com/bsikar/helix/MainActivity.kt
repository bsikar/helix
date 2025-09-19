package com.bsikar.helix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.screens.MainApp
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Initialize UserPreferencesManager with context
            val preferencesManager = remember { UserPreferencesManager(this@MainActivity) }
            val userPreferences by preferencesManager.preferences
            val theme = ThemeManager.getTheme(userPreferences.themeMode)

            val systemUiController = rememberSystemUiController()
            LaunchedEffect(userPreferences.themeMode) {
                val isLight = theme == ThemeManager.lightTheme
                systemUiController.setSystemBarsColor(
                    color = theme.backgroundColor,
                    darkIcons = isLight,
                    isNavigationBarContrastEnforced = false
                )
                systemUiController.navigationBarDarkContentEnabled = isLight
            }

            MaterialTheme {
                MainApp(
                    currentTheme = userPreferences.themeMode,
                    onThemeChange = { newTheme -> preferencesManager.updateTheme(newTheme) },
                    theme = theme,
                    preferencesManager = preferencesManager
                )
            }
        }
    }
}
