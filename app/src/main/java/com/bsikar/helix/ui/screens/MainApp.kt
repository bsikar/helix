package com.bsikar.helix.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsikar.helix.R
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.UiState
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.viewmodels.LibraryViewModel
import com.bsikar.helix.viewmodels.ReaderViewModel
import com.bsikar.helix.managers.ImportManager

@Composable
fun MainApp(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    preferencesManager: UserPreferencesManager,
    libraryViewModel: LibraryViewModel,
    importManager: ImportManager? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsScrollTarget by remember { mutableStateOf<String?>(null) }
    var currentBook by remember { mutableStateOf<Book?>(null) }
    var seeAllData by remember { mutableStateOf<Pair<String, List<com.bsikar.helix.data.model.Book>>?>(null) }

    // Collect states from ViewModel - use filtered results for search functionality
    val allBooks by libraryViewModel.allBooks.collectAsState()
    val readingBooks by libraryViewModel.filteredReadingBooks.collectAsState()
    val planToReadBooks by libraryViewModel.filteredPlanToReadBooks.collectAsState()
    val completedBooks by libraryViewModel.filteredCompletedBooks.collectAsState()
    val recentBooks by libraryViewModel.recentBooks.collectAsState()
    val libraryState by libraryViewModel.libraryState.collectAsState()
    val errorMessage by libraryViewModel.errorMessage.collectAsState()
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    
    // Collect sorting states
    val readingSortAscending by libraryViewModel.readingSortAscending.collectAsState()
    val planToReadSortAscending by libraryViewModel.planToReadSortAscending.collectAsState()
    val completedSortAscending by libraryViewModel.completedSortAscending.collectAsState()

    // Handle global library state (for operations like imports/scans)
    when (libraryState) {
        is UiState.Loading -> {
            // Show loading overlay only for critical operations
            // The individual book states are still available for display
        }
        is UiState.Error -> {
            // Show error snackbar or dialog
            LaunchedEffect(libraryState) {
                // Error is handled, could show a snackbar here
            }
        }
        is UiState.Success -> {
            // Success state, continue with normal UI
        }
    }

    // Show error message if present
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Could show snackbar for the error message
        }
    }

    when {
        showSettings -> {
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                theme = theme,
                onBackClick = { 
                    showSettings = false
                    settingsScrollTarget = null
                },
                libraryManager = libraryViewModel.libraryManager,
                scrollToSection = settingsScrollTarget,
                importManager = importManager
            )
        }
        seeAllData != null -> {
            SeeAllScreen(
                title = seeAllData!!.first,
                books = seeAllData!!.second,
                theme = theme,
                onBackClick = { seeAllData = null },
                onBookClick = { book -> 
                    currentBook = book
                    seeAllData = null // Clear see all data to navigate to reader
                }
            )
        }
        currentBook != null -> {
            val readerViewModel: ReaderViewModel = hiltViewModel()
            ReaderScreen(
                book = currentBook!!,
                theme = theme,
                onBackClick = { 
                    currentBook = null 
                },
                onUpdateReadingPosition = { bookId, currentPage, currentChapter, scrollPosition ->
                    libraryViewModel.updateReadingPosition(bookId, currentPage, currentChapter, scrollPosition)
                    // Update the currentBook state with the latest data from the updated allBooks list
                    currentBook = allBooks.find { it.id == bookId } ?: currentBook
                },
                onUpdateBookSettings = { updatedBook ->
                    libraryViewModel.updateBookSettings(updatedBook)
                    currentBook = updatedBook  // Update the currentBook state immediately
                },
                preferencesManager = preferencesManager,
                libraryManager = libraryViewModel.libraryManager,
                readerViewModel = readerViewModel
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
                    onNavigateToSettings = { 
                        showSettings = true
                        settingsScrollTarget = null
                    },
                    onNavigateToProgressSettings = {
                        showSettings = true
                        settingsScrollTarget = "progress"
                    },
                    onBookClick = { book -> currentBook = book },
                    onSeeAllClick = { title, books -> seeAllData = title to books },
                    onStartReading = { bookId -> libraryViewModel.startReading(bookId) },
                    onMarkCompleted = { bookId -> libraryViewModel.markAsCompleted(bookId) },
                    onMoveToPlanToRead = { bookId -> libraryViewModel.moveToPlanToRead(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) },
                    onUpdateBookSettings = { book -> libraryViewModel.updateBookSettings(book) },
                    libraryManager = libraryViewModel.libraryManager,
                    libraryState = libraryState,
                    errorMessage = errorMessage,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query -> libraryViewModel.updateSearchQuery(query) },
                    // Sorting parameters
                    readingSortAscending = readingSortAscending,
                    planToReadSortAscending = planToReadSortAscending,
                    completedSortAscending = completedSortAscending,
                    onToggleReadingSort = { libraryViewModel.toggleReadingSortOrder() },
                    onTogglePlanToReadSort = { libraryViewModel.togglePlanToReadSortOrder() },
                    onToggleCompletedSort = { libraryViewModel.toggleCompletedSortOrder() },
                    onRefresh = { libraryViewModel.refreshBooks() }
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
                    onMoveToPlanToRead = { bookId -> libraryViewModel.moveToPlanToRead(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) },
                    onRefresh = { libraryViewModel.refreshBooks() }
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
                    onMoveToPlanToRead = { bookId -> libraryViewModel.moveToPlanToRead(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) },
                    onUpdateBookSettings = { book -> libraryViewModel.updateBookSettings(book) },
                    onRefresh = { libraryViewModel.refreshBooks() }
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
                    onNavigateToSettings = { 
                        showSettings = true
                        settingsScrollTarget = null
                    },
                    onNavigateToProgressSettings = {
                        showSettings = true
                        settingsScrollTarget = "progress"
                    },
                    onBookClick = { book -> currentBook = book },
                    onSeeAllClick = { title, books -> seeAllData = title to books },
                    onStartReading = { bookId -> libraryViewModel.startReading(bookId) },
                    onMarkCompleted = { bookId -> libraryViewModel.markAsCompleted(bookId) },
                    onMoveToPlanToRead = { bookId -> libraryViewModel.moveToPlanToRead(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) },
                    onUpdateBookSettings = { book -> libraryViewModel.updateBookSettings(book) },
                    libraryManager = libraryViewModel.libraryManager,
                    libraryState = libraryState,
                    errorMessage = errorMessage,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query -> libraryViewModel.updateSearchQuery(query) },
                    // Sorting parameters
                    readingSortAscending = readingSortAscending,
                    planToReadSortAscending = planToReadSortAscending,
                    completedSortAscending = completedSortAscending,
                    onToggleReadingSort = { libraryViewModel.toggleReadingSortOrder() },
                    onTogglePlanToReadSort = { libraryViewModel.togglePlanToReadSortOrder() },
                    onToggleCompletedSort = { libraryViewModel.toggleCompletedSortOrder() }
                )
            }
        }
    }
}
