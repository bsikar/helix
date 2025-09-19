package com.bsikar.helix.ui.screens

import androidx.compose.runtime.*
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode

@Composable
fun MainApp(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    preferencesManager: UserPreferencesManager
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var currentBook by remember { mutableStateOf<Book?>(null) }
    var seeAllData by remember { mutableStateOf<Pair<String, List<Book>>?>(null) }

    when {
        showSettings -> {
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                theme = theme,
                onBackClick = { showSettings = false }
            )
        }
        seeAllData != null -> {
            SeeAllScreen(
                title = seeAllData!!.first,
                books = seeAllData!!.second,
                theme = theme,
                onBackClick = { seeAllData = null },
                onBookClick = { book -> currentBook = book }
            )
        }
        currentBook != null -> {
            ReaderScreen(
                book = currentBook!!,
                theme = theme,
                onBackClick = { currentBook = null },
                preferencesManager = preferencesManager
            )
        }
        else -> {
            when (selectedTab) {
                0 -> LibraryScreen(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    theme = theme,
                    onNavigateToSettings = { showSettings = true },
                    onBookClick = { book -> currentBook = book },
                    onSeeAllClick = { title, books -> seeAllData = title to books }
                )
                1 -> RecentsScreen(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    theme = theme,
                    onNavigateToSettings = { showSettings = true },
                    onBookClick = { book -> currentBook = book }
                )
                2 -> BrowseScreen(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    theme = theme,
                    onNavigateToSettings = { showSettings = true },
                    onBookClick = { book -> currentBook = book },
                    onSeeAllClick = { title, books -> seeAllData = title to books }
                )
                else -> LibraryScreen(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    theme = theme,
                    onNavigateToSettings = { showSettings = true },
                    onBookClick = { book -> currentBook = book },
                    onSeeAllClick = { title, books -> seeAllData = title to books }
                )
            }
        }
    }
}
