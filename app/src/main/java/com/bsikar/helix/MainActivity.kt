package com.bsikar.helix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.managers.ImportManager
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.screens.MainApp
import com.bsikar.helix.viewmodels.LibraryViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: UserPreferencesManager
    
    @Inject
    lateinit var importManager: ImportManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val libraryViewModel: LibraryViewModel = hiltViewModel()
            val userPreferences by preferencesManager.preferences
            val theme = ThemeManager.getTheme(userPreferences.themeMode)

            val systemUiController = rememberSystemUiController()
            LaunchedEffect(userPreferences.themeMode, theme) {
                val isLight = when (userPreferences.themeMode) {
                    ThemeMode.LIGHT, ThemeMode.SEPIA, ThemeMode.WARM, ThemeMode.COOL -> true
                    ThemeMode.DARK, ThemeMode.NIGHT_MODE -> false
                    ThemeMode.HIGH_CONTRAST -> true
                    ThemeMode.SYSTEM -> theme == ThemeManager.lightTheme
                    ThemeMode.DYNAMIC -> theme.colorScheme?.background?.luminance() ?: 0.5f > 0.5f
                }
                
                systemUiController.setSystemBarsColor(
                    color = theme.backgroundColor,
                    darkIcons = isLight,
                    isNavigationBarContrastEnforced = false
                )
                systemUiController.navigationBarDarkContentEnabled = isLight
            }

            // Use Material 3 theme with proper color scheme
            MaterialTheme(
                colorScheme = theme.colorScheme ?: ThemeManager.lightTheme.colorScheme!!
            ) {
                MainApp(
                    currentTheme = userPreferences.themeMode,
                    onThemeChange = { newTheme -> preferencesManager.updateTheme(newTheme) },
                    theme = theme,
                    preferencesManager = preferencesManager,
                    libraryViewModel = libraryViewModel,
                    importManager = importManager
                )
            }
        }
    }
}
