package com.bsikar.helix.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.managers.ImportManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.viewmodels.LibraryViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun MainApp(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    libraryViewModel: LibraryViewModel,
    importManager: ImportManager? = null
) {
    val allBooks by libraryViewModel.allBooks.collectAsState()
    val readingBooks by libraryViewModel.readingBooks.collectAsState()
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val isRefreshing by libraryViewModel.isRefreshing.collectAsState()
    val scanMessage by libraryViewModel.scanMessage.collectAsState()
    val errorMessage by libraryViewModel.errorMessage.collectAsState()

    val importProgressFlow: Flow<List<com.bsikar.helix.data.model.ImportProgress>> = remember(importManager) {
        importManager?.importProgress ?: flowOf(emptyList())
    }
    val importProgress by importProgressFlow.collectAsState(initial = emptyList())

    var activeBook by remember { mutableStateOf<Book?>(null) }
    var pairedAudiobook by remember { mutableStateOf<Book?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val audiobooks = remember(allBooks) { allBooks.filter { it.isAudiobook() } }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            libraryViewModel.clearError()
        }
    }

    when {
        activeBook != null -> {
            ImmersiveReaderScreen(
                book = activeBook!!,
                pairedAudiobook = pairedAudiobook,
                availableAudiobooks = audiobooks,
                onBack = {
                    activeBook = null
                    pairedAudiobook = null
                },
                onPairAudiobook = { selection ->
                    pairedAudiobook = selection
                },
                onUpdateReadingPosition = { id, page, chapter, scroll ->
                    libraryViewModel.updateReadingPosition(id, page, chapter, scroll)
                },
                onAudiobookProgress = { id, position, speed ->
                    libraryViewModel.updateAudiobookProgress(id, position, speed)
                },
                theme = theme
            )
        }
        showSettings -> {
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                theme = theme,
                onClose = { showSettings = false }
            )
        }
        else -> {
            HomeScreen(
                theme = theme,
                currentTheme = currentTheme,
                allBooks = allBooks,
                readingBooks = readingBooks,
                audiobooks = audiobooks,
                searchQuery = searchQuery,
                onSearchQueryChange = { libraryViewModel.updateSearchQuery(it) },
                onBookSelected = { book ->
                    activeBook = book
                    pairedAudiobook = if (book.isAudiobook()) book else null
                    libraryViewModel.startReading(book.id)
                },
                onOpenSettings = { showSettings = true },
                onToggleTheme = { onThemeChange(nextTheme(currentTheme)) },
                onRescanLibrary = { libraryViewModel.refreshLibrary() },
                isRefreshing = isRefreshing,
                importProgress = importProgress,
                snackbarHostState = snackbarHostState,
                scanMessage = scanMessage
            )
        }
    }
}

private fun nextTheme(current: ThemeMode): ThemeMode = when (current) {
    ThemeMode.DARK -> ThemeMode.LIGHT
    ThemeMode.LIGHT -> ThemeMode.DARK
    ThemeMode.HIGH_CONTRAST -> ThemeMode.DARK
    ThemeMode.NIGHT_MODE -> ThemeMode.LIGHT
    else -> ThemeMode.DARK
}
