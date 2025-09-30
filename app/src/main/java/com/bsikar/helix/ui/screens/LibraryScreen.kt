package com.bsikar.helix.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.bsikar.helix.R
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.model.UiState
import com.bsikar.helix.viewmodels.AudioBookReaderViewModel
import com.bsikar.helix.data.ImportProgress
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.InfiniteHorizontalBookScroll
import com.bsikar.helix.ui.components.SearchBar
import com.bsikar.helix.ui.components.ResponsiveConfig
import com.bsikar.helix.ui.components.ResponsiveBookGrid
import com.bsikar.helix.ui.components.ResponsiveBookCard
import com.bsikar.helix.ui.components.getWindowSizeClass
import com.bsikar.helix.ui.components.WindowSizeClass
import com.bsikar.helix.ui.components.ImportProgressIndicator
import com.bsikar.helix.ui.components.CompactImportProgress
import com.bsikar.helix.ui.components.ResponsiveSpacing
import com.bsikar.helix.managers.ImportManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    readingBooks: List<com.bsikar.helix.data.model.Book>,
    onDeckBooks: List<com.bsikar.helix.data.model.Book>,
    completedBooks: List<com.bsikar.helix.data.model.Book>,
    allBooks: List<com.bsikar.helix.data.model.Book>,
    currentlyPlayingAudiobook: Book? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProgressSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onSeeAllClick: (String, List<com.bsikar.helix.data.model.Book>) -> Unit = { _, _ -> },
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (com.bsikar.helix.data.model.Book) -> Unit = { _ -> },
    onRemoveFromLibrary: (String) -> Unit = { _ -> },
    libraryManager: LibraryManager? = null,
    libraryState: UiState<List<com.bsikar.helix.data.model.Book>> = UiState.Success(emptyList()),
    errorMessage: String? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    importManager: ImportManager? = null,
    // Sorting states and functions
    readingSortAscending: Boolean = false,
    onDeckSortAscending: Boolean = true,
    completedSortAscending: Boolean = true,
    onToggleReadingSort: () -> Unit = {},
    onToggleOnDeckSort: () -> Unit = {},
    onToggleCompletedSort: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    // Search query now comes from ViewModel instead of local state
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Import progress state
    val importProgress by importManager?.importProgress?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    // Create mutable internal lists to allow book updates
    // Use keys that include order to ensure recomposition when sorting changes
    val readingBooksKey = remember(readingBooks.size, readingSortAscending) { readingBooks.map { it.id }.joinToString(",") }
    val onDeckBooksKey = remember(onDeckBooks.size, onDeckSortAscending) { onDeckBooks.map { it.id }.joinToString(",") }
    val completedBooksKey = remember(completedBooks.size, completedSortAscending) { completedBooks.map { it.id }.joinToString(",") }
    
    var internalReadingBooks by remember(readingBooksKey) { mutableStateOf(readingBooks) }
    var internalOnDeckBooks by remember(onDeckBooksKey) { mutableStateOf(onDeckBooks) }
    var internalCompletedBooks by remember(completedBooksKey) { mutableStateOf(completedBooks) }
    
    // Sync internal lists when source lists change (including sorting changes)
    LaunchedEffect(readingBooksKey) { internalReadingBooks = readingBooks }
    LaunchedEffect(onDeckBooksKey) { internalOnDeckBooks = onDeckBooks }
    LaunchedEffect(completedBooksKey) { internalCompletedBooks = completedBooks }
    
    // Function to update a book in the appropriate list
    val updateBookInLists = { updatedBook: com.bsikar.helix.data.model.Book ->
        // Update in reading books
        internalReadingBooks = internalReadingBooks.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        
        // Update in plan to read books
        internalOnDeckBooks = internalOnDeckBooks.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        
        // Update in completed books
        internalCompletedBooks = internalCompletedBooks.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        
        // Also call the parent's update function if provided
        onUpdateBookSettings(updatedBook)
    }
    
    // Import handlers
    val handleCancelImport: (String) -> Unit = { importId ->
        scope.launch {
            importManager?.cancelImport(importId)
        }
    }
    
    val handleRetryImport: (String) -> Unit = { importId ->
        scope.launch {
            importManager?.retryImport(importId)
        }
    }
    
    val handleDismissImport: (String) -> Unit = { importId ->
        // Import will be automatically removed from active list when completed
    }
    
    // Pull-to-refresh functionality
    
    // Responsive configuration
    val responsiveConfig = ResponsiveConfig.fromScreenWidth()
    val windowSizeClass = getWindowSizeClass()
    
    // Function to render books responsively
    @Composable
    fun ResponsiveBookSection(
        books: List<com.bsikar.helix.data.model.Book>,
        showProgress: Boolean
    ) {
        when (windowSizeClass) {
            WindowSizeClass.COMPACT -> {
                // Use horizontal scroll for phones
                InfiniteHorizontalBookScroll(
                    books = books,
                    showProgress = showProgress,
                    theme = theme,
                    searchQuery = searchQuery,
                    contentPadding = ResponsiveSpacing.contentPadding(),
                    onBookClick = onBookClick,
                    onStartReading = onStartReading,
                    onMarkCompleted = onMarkCompleted,
                    onMoveToOnDeck = onMoveToOnDeck,
                    onSetProgress = onSetProgress,
                    onEditTags = onEditTags,
                    onUpdateBookSettings = updateBookInLists,
                    onRemoveFromLibrary = onRemoveFromLibrary
                )
            }
            WindowSizeClass.MEDIUM, WindowSizeClass.EXPANDED -> {
                // Use responsive grid for tablets
                if (books.isNotEmpty()) {
                    ResponsiveBookGrid(
                        items = books,
                        config = responsiveConfig,
                        key = { book -> book.id }
                    ) { book ->
                        ResponsiveBookCard(
                            book = book,
                            showProgress = showProgress,
                            theme = theme,
                            searchQuery = searchQuery,
                            config = responsiveConfig,
                            isBrowseMode = false,
                            onBookClick = onBookClick,
                            onStartReading = onStartReading,
                            onMarkCompleted = onMarkCompleted,
                            onMoveToOnDeck = onMoveToOnDeck,
                            onSetProgress = onSetProgress,
                            onEditTags = onEditTags,
                            onUpdateBookSettings = updateBookInLists,
                            onRemoveFromLibrary = onRemoveFromLibrary
                        )
                    }
                }
            }
        }
    }
    val handleRefresh = {
        isRefreshing = true
        onRefresh() // Call the provided refresh function
        scope.launch {
            kotlinx.coroutines.delay(500) // Brief delay for visual feedback
            isRefreshing = false
        }
    }

    // Sorting is now handled in ViewModel - filtered results come pre-sorted


    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.surfaceColor,
                    titleContentColor = theme.primaryTextColor,
                ),
                title = {
                    Text(
                        stringResource(R.string.library),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryTextColor
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = theme.primaryTextColor
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = theme.surfaceColor,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = stringResource(R.string.cd_library),
                            tint = if (selectedTab == 0) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.library),
                            color = if (selectedTab == 0) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    icon = {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = stringResource(R.string.cd_recents),
                            tint = if (selectedTab == 1) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.recents),
                            color = if (selectedTab == 1) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    icon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.cd_browse),
                            tint = if (selectedTab == 2) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.browse),
                            color = if (selectedTab == 2) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        // Handle UiState for library operations
        when (libraryState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = theme.accentColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Loading library...",
                            color = theme.secondaryTextColor,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Library Error",
                            color = theme.primaryTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = libraryState.exception.message ?: "An unknown error occurred",
                            color = theme.secondaryTextColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { handleRefresh() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.accentColor
                            )
                        ) {
                            Text(
                                text = "Retry",
                                color = theme.surfaceColor
                            )
                        }
                    }
                }
            }
            is UiState.Success -> {
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = isRefreshing,
                    onRefresh = { handleRefresh() },
                    state = pullToRefreshState
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

            item(key = "search_bar") {
                SearchBar(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    theme = theme
                )
            }
            
            // Show import progress using the new ImportManager
            if (importProgress.isNotEmpty()) {
                item(key = "import_progress") {
                    ImportProgressIndicator(
                        importProgress = importProgress,
                        onDismiss = handleDismissImport,
                        onCancel = handleCancelImport,
                        onRetry = handleRetryImport,
                        modifier = Modifier.padding(horizontal = ResponsiveSpacing.medium())
                    )
                }
            }
            

            // Reading section (text-based books) - only show if there are books currently being read
            val textBooks = internalReadingBooks.filter { !it.isAudiobook() }
            
            if (textBooks.isNotEmpty()) {
                item(key = "reading_header") {
                    LibrarySectionHeader(
                        title = "Reading",
                        subtitle = "Last read",
                        theme = theme,
                        isAscending = readingSortAscending,
                        onSortClick = onToggleReadingSort,
                        onSeeAllClick = { onSeeAllClick("Reading", textBooks) }
                    )
                }
                item(key = "reading_books_${textBooks.size}") {
                    ResponsiveBookSection(
                        books = textBooks,
                        showProgress = true
                    )
                }
            }
            
            // Listening section (audiobooks) - show audiobooks that are being listened to
            val listeningBooks = allBooks.filter { 
                it.isAudiobook() && it.readingStatus == ReadingStatus.LISTENING
            }.sortedByDescending { it.lastReadTimestamp }
            val audioBooks = allBooks.filter { it.isAudiobook() }
            
            // Show listening section if there's a currently playing audiobook OR other listening books
            val hasListeningContent = currentlyPlayingAudiobook != null || listeningBooks.isNotEmpty()
            
            if (hasListeningContent) {
                item(key = "listening_header") {
                    LibrarySectionHeader(
                        title = "Listening",
                        subtitle = "Last played",
                        theme = theme,
                        isAscending = false,
                        onSortClick = { },
                        onSeeAllClick = { onSeeAllClick("Listening", audioBooks) }
                    )
                }
                
                // Show currently playing audiobook if available
                currentlyPlayingAudiobook?.let { book ->
                    item(key = "currently_playing") {
                        CurrentlyPlayingCard(
                            book = book,
                            theme = theme,
                            onBookClick = onBookClick,
                            modifier = Modifier.padding(horizontal = ResponsiveSpacing.medium(), vertical = ResponsiveSpacing.small())
                        )
                    }
                }
                
                // Show other listening books (excluding the currently playing one)
                val otherListeningBooks = listeningBooks.filter { it.id != currentlyPlayingAudiobook?.id }
                if (otherListeningBooks.isNotEmpty()) {
                    item(key = "listening_books_${otherListeningBooks.size}") {
                        ResponsiveBookSection(
                            books = otherListeningBooks,
                            showProgress = true
                        )
                    }
                }
            }

            if (internalOnDeckBooks.isNotEmpty()) {
                item(key = "plan_to_read_header") {
                    LibrarySectionHeader(
                        title = "On Deck",
                        subtitle = "Title",
                        theme = theme,
                        isAscending = onDeckSortAscending,
                        onSortClick = onToggleOnDeckSort,
                        onSeeAllClick = { onSeeAllClick("On Deck", internalOnDeckBooks) }
                    )
                }
                item(key = "on_deck_books_${internalOnDeckBooks.size}") {
                    ResponsiveBookSection(
                        books = internalOnDeckBooks,
                        showProgress = false
                    )
                }
            }

            if (internalCompletedBooks.isNotEmpty()) {
                item(key = "read_header") {
                    LibrarySectionHeader(
                        title = "Finished",
                        subtitle = "Title",
                        theme = theme,
                        isAscending = completedSortAscending,
                        onSortClick = onToggleCompletedSort,
                        onSeeAllClick = { onSeeAllClick("Finished", internalCompletedBooks) }
                    )
                }
                item(key = "read_books_${internalCompletedBooks.size}") {
                    ResponsiveBookSection(
                        books = internalCompletedBooks,
                        showProgress = false
                    )
                }
            }

            // Show overall empty state only if no books exist at all
            if (allBooks.isEmpty()) {
                item(key = "no_books_found") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = "Empty library",
                            modifier = Modifier.size(64.dp),
                            tint = theme.secondaryTextColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your library is empty",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.primaryTextColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import your books and audiobooks to get started.\nSupported formats: EPUB, M4B",
                            fontSize = 14.sp,
                            color = theme.secondaryTextColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
                    }
                }
            }
        }
        
        // Show error message overlay if present
        errorMessage?.let { message ->
            LaunchedEffect(message) {
                // Here you could show a Snackbar if you have a SnackbarHost
                // For now, the error will be handled by the UiState.Error case above
            }
        }
    }
}

@Composable
fun LibrarySectionHeader(
    title: String,
    subtitle: String,
    theme: AppTheme,
    isAscending: Boolean,
    onSortClick: () -> Unit,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ResponsiveSpacing.medium(), end = ResponsiveSpacing.small()),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryTextColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSortClick)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = theme.secondaryTextColor
                )
                Icon(
                    if (isAscending) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Sort by $subtitle",
                    modifier = Modifier.size(16.dp),
                    tint = theme.secondaryTextColor
                )
            }
        }

        TextButton(onClick = onSeeAllClick) {
            Text(
                text = "See All",
                fontSize = 13.sp,
                color = theme.accentColor
            )
        }
    }
}

@Composable
fun CurrentlyPlayingCard(
    book: Book,
    theme: AppTheme,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onBookClick(book) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.accentColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover art
            Card(
                modifier = Modifier
                    .size(60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                        Color.Transparent
                    } else {
                        book.getEffectiveCoverColor()
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = book.coverImagePath,
                            contentDescription = "Cover",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Book info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Now Playing",
                    fontSize = 12.sp,
                    color = theme.accentColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    fontSize = 14.sp,
                    color = theme.secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Play indicator
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Playing",
                tint = theme.accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}