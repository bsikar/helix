package com.bsikar.helix.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsikar.helix.R
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.UiState
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.PersistentAudioBar
import com.bsikar.helix.viewmodels.AudioBookReaderViewModel
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
    val audioBookViewModel: AudioBookReaderViewModel = hiltViewModel()
    var showSettings by remember { mutableStateOf(false) }
    var settingsScrollTarget by remember { mutableStateOf<String?>(null) }
    var currentBook by remember { mutableStateOf<Book?>(null) }
    var seeAllData by remember { mutableStateOf<Pair<String, List<com.bsikar.helix.data.model.Book>>?>(null) }

    // Collect states from ViewModel - use filtered results for search functionality
    val allBooks by libraryViewModel.allBooks.collectAsState()
    val readingBooks by libraryViewModel.filteredReadingBooks.collectAsState()
    val onDeckBooks by libraryViewModel.filteredOnDeckBooks.collectAsState()
    val completedBooks by libraryViewModel.filteredCompletedBooks.collectAsState()
    val libraryState by libraryViewModel.libraryState.collectAsState()
    val errorMessage by libraryViewModel.errorMessage.collectAsState()
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val contentFilter by libraryViewModel.contentFilter.collectAsState()
    val activeSecondaryFilters by libraryViewModel.activeSecondaryFilters.collectAsState()
    val playbackState by audioBookViewModel.playbackState.collectAsState()
    
    // Collect sorting states
    val readingSortAscending by libraryViewModel.readingSortAscending.collectAsState()
    val onDeckSortAscending by libraryViewModel.onDeckSortAscending.collectAsState()
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
            if (currentBook!!.isAudiobook()) {
                // Automatically mark audiobook as listening when opening
                LaunchedEffect(currentBook!!.id) {
                    libraryViewModel.startReading(currentBook!!.id)
                }
                AudioBookReaderScreen(
                    book = currentBook!!,
                    theme = theme,
                    onBackClick = {
                        currentBook = null
                    },
                    viewModel = audioBookViewModel
                )
            } else {
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
        }
        else -> {
            val fallbackAudiobook = allBooks
                .filter { it.isAudiobook() }
                .filter { it.progress > 0f && it.progress < 1f }
                .maxByOrNull { it.lastReadTimestamp }
            val currentlyPlayingAudiobook = playbackState.currentBook ?: fallbackAudiobook
            val persistentBarVisible = playbackState.isPlaying && playbackState.currentBook != null

            Box(modifier = Modifier.fillMaxSize()) {
                LibraryScreen(
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    theme = theme,
                    readingBooks = readingBooks,
                    onDeckBooks = onDeckBooks,
                    completedBooks = completedBooks,
                    allBooks = allBooks,
                    currentlyPlayingAudiobook = currentlyPlayingAudiobook,
                    contentFilter = contentFilter,
                    onContentFilterChange = { libraryViewModel.setContentFilter(it) },
                    activeSecondaryFilters = activeSecondaryFilters,
                    onToggleSecondaryFilter = { libraryViewModel.toggleSecondaryFilter(it) },
                    onClearSecondaryFilters = { libraryViewModel.clearSecondaryFilters() },
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
                    onMoveToOnDeck = { bookId -> libraryViewModel.moveToOnDeck(bookId) },
                    onSetProgress = { bookId, progress -> libraryViewModel.setBookProgress(bookId, progress) },
                    onEditTags = { bookId, newTags -> libraryViewModel.updateBookTags(bookId, newTags) },
                    onUpdateBookSettings = { book -> libraryViewModel.updateBookSettings(book) },
                    onRemoveFromLibrary = { bookId -> libraryViewModel.removeFromLibrary(bookId) },
                    libraryManager = libraryViewModel.libraryManager,
                    libraryState = libraryState,
                    errorMessage = errorMessage,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query -> libraryViewModel.updateSearchQuery(query) },
                    readingSortAscending = readingSortAscending,
                    onDeckSortAscending = onDeckSortAscending,
                    completedSortAscending = completedSortAscending,
                    onToggleReadingSort = { libraryViewModel.toggleReadingSortOrder() },
                    onToggleOnDeckSort = { libraryViewModel.toggleOnDeckSortOrder() },
                    onToggleCompletedSort = { libraryViewModel.toggleCompletedSortOrder() },
                    onRefresh = { libraryViewModel.refreshBooks() },
                    extraBottomPadding = if (persistentBarVisible) 88.dp else 0.dp
                )

                if (persistentBarVisible) {
                    PersistentAudioBar(
                        playbackState = playbackState,
                        theme = theme,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        onExpand = {
                            playbackState.currentBook?.let { book ->
                                currentBook = book
                            }
                        },
                        onPlayPause = { audioBookViewModel.togglePlayPause() }
                    )
                }
            }
        }
    }
}