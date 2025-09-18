package com.bsikar.helix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bsikar.helix.data.rememberUserPreferences
import com.bsikar.helix.ui.theme.HelixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize cache systems
        initializeCaches()
        setContent {
            val userPreferences = rememberUserPreferences()

            HelixTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "library") {
                    composable("library") {
                        LibraryScreen(navController = navController)
                    }
                    composable("reader/{bookPath}") { backStackEntry ->
                        val bookPath = backStackEntry.arguments?.getString("bookPath")
                        if (bookPath != null) {
                            ReaderScreen(
                                bookPath = bookPath,
                                navController = navController,
                                userPreferences = userPreferences
                            )
                        }
                    }
                    composable("settings") {
                        SettingsScreen(
                            navController = navController
                        )
                    }
                }
            }
        }
    }

    private fun initializeCaches() {
        // Initialize image cache with application context
        ImageCache.initialize(this)

        // Initialize persistent disk cache
        PersistentImageCache.initialize(this)
    }
}
