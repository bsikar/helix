package com.bsikar.helix.ui.screens

import androidx.compose.runtime.*
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.viewmodels.LibraryViewModel

@Composable
fun MainApp(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    preferencesManager: UserPreferencesManager,
    libraryViewModel: LibraryViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var currentBook by remember { mutableStateOf<Book?>(null) }
    var seeAllData by remember { mutableStateOf<Pair<String, List<Book>>?>(null) }

    // Collect states from ViewModel
    val allBooks by libraryViewModel.allBooks.collectAsState()
    val readingBooks by libraryViewModel.readingBooks.collectAsState()
    val planToReadBooks by libraryViewModel.planToReadBooks.collectAsState()
    val completedBooks by libraryViewModel.completedBooks.collectAsState()
    val recentBooks by libraryViewModel.recentBooks.collectAsState()

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
                onBackClick = { 
                    currentBook = null 
                },
                onUpdateReadingPosition = { bookId, currentPage, currentChapter, scrollPosition ->
                    libraryViewModel.updateReadingPosition(bookId, currentPage, currentChapter, scrollPosition)
                },
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
                    readingBooks = readingBooks,
                    planToReadBooks = planToReadBooks,
                    completedBooks = completedBooks,
                    onNavigateToSettings = { showSettings = true },
                    onBookClick = { book -> currentBook = book },
                    onSeeAllClick = { title, books -> seeAllData = title to books },
                    onStartReading = { bookId -> libraryViewModel.startReading(bookId) },
                    onMarkCompleted = { bookId -> libraryViewModel.markAsCompleted(bookId) },
                    onMoveToPlanToRead = { bookId -> libraryViewModel.moveToplanToRead(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) }
                )
                1 -> RecentsScreen(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    theme = theme,
                    recentBooks = recentBooks,
                    onNavigateToSettings = { showSettings = true },
                    onBookClick = { book -> currentBook = book },
                    onStartReading = { bookId -> libraryViewModel.startReading(bookId) },
                    onMarkCompleted = { bookId -> libraryViewModel.markAsCompleted(bookId) },
                    onMoveToPlanToRead = { bookId -> libraryViewModel.moveToplanToRead(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) }
                )
                2 -> BrowseScreen(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    theme = theme,
                    onNavigateToSettings = { showSettings = true },
                    onBookClick = { book -> currentBook = book },
                    onSeeAllClick = { title, books -> seeAllData = title to books },
                    allBooks = allBooks,
                    onStartReading = { bookId -> libraryViewModel.startReading(bookId) },
                    onMarkCompleted = { bookId -> libraryViewModel.markAsCompleted(bookId) },
                    onMoveToPlanToRead = { bookId -> libraryViewModel.moveToplanToRead(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) }
                )
                else -> LibraryScreen(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    theme = theme,
                    readingBooks = readingBooks,
                    planToReadBooks = planToReadBooks,
                    completedBooks = completedBooks,
                    onNavigateToSettings = { showSettings = true },
                    onBookClick = { book -> currentBook = book },
                    onSeeAllClick = { title, books -> seeAllData = title to books },
                    onStartReading = { bookId -> libraryViewModel.startReading(bookId) },
                    onMarkCompleted = { bookId -> libraryViewModel.markAsCompleted(bookId) },
                    onMoveToPlanToRead = { bookId -> libraryViewModel.moveToplanToRead(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) }
                )
            }
        }
    }
}
