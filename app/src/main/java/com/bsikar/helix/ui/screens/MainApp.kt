package com.bsikar.helix.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.UiState
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.viewmodels.AudioBookReaderViewModel
import com.bsikar.helix.viewmodels.LibraryViewModel
import com.bsikar.helix.viewmodels.ReaderViewModel
import com.bsikar.helix.managers.ImportManager
import com.bsikar.helix.ui.components.AudioNowPlayingBar

@Composable
fun MainApp(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    preferencesManager: UserPreferencesManager,
    libraryViewModel: LibraryViewModel,
    importManager: ImportManager? = null
) {
    val audioBookReaderViewModel: AudioBookReaderViewModel = hiltViewModel()
    val playbackState by audioBookReaderViewModel.playbackState.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    var settingsScrollTarget by remember { mutableStateOf<String?>(null) }
    var currentBook by remember { mutableStateOf<Book?>(null) }

    val allBooks by libraryViewModel.allBooks.collectAsState()
    val filteredBooks by libraryViewModel.filteredLibraryBooks.collectAsState()
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val contentFilter by libraryViewModel.contentFilter.collectAsState()
    val activeTagFilters by libraryViewModel.activeTagFilters.collectAsState()
    val libraryState by libraryViewModel.libraryState.collectAsState()
    val errorMessage by libraryViewModel.errorMessage.collectAsState()
    val isRefreshing by libraryViewModel.isRefreshing.collectAsState()
    val scanMessage by libraryViewModel.scanMessage.collectAsState()

    val importProgressState = importManager?.importProgress?.collectAsState(initial = emptyList())
    val importProgress = importProgressState?.value ?: emptyList()

    val showNowPlayingBar = playbackState.currentBook != null &&
        (playbackState.isPlaying || playbackState.currentPositionMs > 0L)

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

    Box(modifier = Modifier.fillMaxSize()) {
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
            currentBook != null -> {
                if (currentBook!!.isAudiobook()) {
                    LaunchedEffect(currentBook!!.id) {
                        libraryViewModel.startReading(currentBook!!.id)
                    }
                    AudioBookReaderScreen(
                        book = currentBook!!,
                        theme = theme,
                        onBackClick = {
                            currentBook = null
                        }
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
                            currentBook = allBooks.find { it.id == bookId } ?: currentBook
                        },
                        onUpdateBookSettings = { updatedBook ->
                            libraryViewModel.updateBookSettings(updatedBook)
                            currentBook = updatedBook
                        },
                        preferencesManager = preferencesManager,
                        libraryManager = libraryViewModel.libraryManager,
                        readerViewModel = readerViewModel
                    )
                }
            }
            else -> {
                UnifiedLibraryScreen(
                    theme = theme,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query -> libraryViewModel.updateSearchQuery(query) },
                    contentFilter = contentFilter,
                    onContentFilterChange = { filter -> libraryViewModel.updateContentFilter(filter) },
                    activeTagFilters = activeTagFilters,
                    onToggleTagFilter = { tagId -> libraryViewModel.toggleTagFilter(tagId) },
                    onClearTagFilters = { libraryViewModel.clearTagFilters() },
                    books = filteredBooks,
                    allBooks = allBooks,
                    nowPlayingBookId = playbackState.currentBook?.id,
                    onBookClick = { book -> currentBook = book },
                    onStartReading = { bookId -> libraryViewModel.startReading(bookId) },
                    onMarkCompleted = { bookId -> libraryViewModel.markAsCompleted(bookId) },
                    onMoveToOnDeck = { bookId -> libraryViewModel.moveToOnDeck(bookId) },
                    onRemoveFromOnDeck = { bookId -> libraryViewModel.removeFromOnDeck(bookId) },
                    onRefresh = { libraryViewModel.refreshBooks() },
                    importProgress = importProgress,
                    isRefreshing = isRefreshing,
                    scanMessage = scanMessage,
                    onOpenSettings = {
                        showSettings = true
                        settingsScrollTarget = null
                    },
                    onOpenProgressSettings = {
                        showSettings = true
                        settingsScrollTarget = "progress"
                    },
                    showNowPlayingBarPadding = showNowPlayingBar && currentBook == null && !showSettings
                )
            }
        }

        if (showNowPlayingBar && currentBook == null && !showSettings) {
            AudioNowPlayingBar(
                playbackState = playbackState,
                theme = theme,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                onExpand = {
                    playbackState.currentBook?.let { book ->
                        currentBook = book
                    }
                },
                onPlayPause = { audioBookReaderViewModel.togglePlayPause() },
                onSkipForward = { audioBookReaderViewModel.skipForward() },
                onSkipBackward = { audioBookReaderViewModel.skipBackward() }
            )
        }
    }
}