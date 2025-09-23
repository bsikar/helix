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
import com.bsikar.helix.R
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.UiState
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
import com.bsikar.helix.managers.ImportManager
// SearchUtils no longer needed - search logic moved to ViewModel
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
    planToReadBooks: List<com.bsikar.helix.data.model.Book>,
    completedBooks: List<com.bsikar.helix.data.model.Book>,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProgressSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onSeeAllClick: (String, List<com.bsikar.helix.data.model.Book>) -> Unit = { _, _ -> },
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (com.bsikar.helix.data.model.Book) -> Unit = { _ -> },
    libraryManager: LibraryManager? = null,
    libraryState: UiState<List<com.bsikar.helix.data.model.Book>> = UiState.Success(emptyList()),
    errorMessage: String? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    importManager: ImportManager? = null
) {
    // Search query now comes from ViewModel instead of local state
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Import progress state
    val importProgress by importManager?.importProgress?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    // Create mutable internal lists to allow book updates
    // Use a key that includes book properties to detect internal book changes
    val readingBooksKey = readingBooks.map { "${it.id}-${it.coverDisplayMode}-${it.userSelectedColor}" }.joinToString(",")
    val planToReadBooksKey = planToReadBooks.map { "${it.id}-${it.coverDisplayMode}-${it.userSelectedColor}" }.joinToString(",")
    val completedBooksKey = completedBooks.map { "${it.id}-${it.coverDisplayMode}-${it.userSelectedColor}" }.joinToString(",")
    
    var internalReadingBooks by remember(readingBooksKey) { mutableStateOf(readingBooks) }
    var internalPlanToReadBooks by remember(planToReadBooksKey) { mutableStateOf(planToReadBooks) }
    var internalCompletedBooks by remember(completedBooksKey) { mutableStateOf(completedBooks) }
    
    // Function to update a book in the appropriate list
    val updateBookInLists = { updatedBook: com.bsikar.helix.data.model.Book ->
        // Update in reading books
        internalReadingBooks = internalReadingBooks.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        
        // Update in plan to read books
        internalPlanToReadBooks = internalPlanToReadBooks.map { book ->
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
    var scanMessage by remember { mutableStateOf("") }
    
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    onBookClick = onBookClick,
                    onStartReading = onStartReading,
                    onMarkCompleted = onMarkCompleted,
                    onMoveToPlanToRead = onMoveToPlanToRead,
                    onSetProgress = onSetProgress,
                    onEditTags = onEditTags,
                    onUpdateBookSettings = updateBookInLists
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
                            onBookClick = onBookClick,
                            onStartReading = onStartReading,
                            onMarkCompleted = onMarkCompleted,
                            onMoveToPlanToRead = onMoveToPlanToRead,
                            onSetProgress = onSetProgress,
                            onEditTags = onEditTags,
                            onUpdateBookSettings = updateBookInLists
                        )
                    }
                }
            }
        }
    }
    val onRefresh = {
        if (libraryManager != null) {
            isRefreshing = true
            scanMessage = "Scanning for new books..."
            libraryManager.rescanWatchedDirectoriesAsync { success, message, newCount ->
                isRefreshing = false
                scanMessage = if (success) {
                    if (newCount > 0) "Found $newCount new books!" else "No new books found"
                } else {
                    "Scan failed: $message"
                }
                // Clear message after 3 seconds
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    scanMessage = ""
                }
            }
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
                            onClick = onRefresh,
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
                    onRefresh = onRefresh
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
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            // Show scan status message
            if (scanMessage.isNotEmpty()) {
                item(key = "scan_message") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (scanMessage.contains("Found") && !scanMessage.contains("No new")) 
                                theme.accentColor.copy(alpha = 0.1f) 
                            else 
                                theme.surfaceColor
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = theme.accentColor,
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                text = scanMessage,
                                color = if (scanMessage.contains("Found") && !scanMessage.contains("No new")) 
                                    theme.accentColor 
                                else 
                                    theme.primaryTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (internalReadingBooks.isNotEmpty()) {
                item(key = "reading_header") {
                    LibrarySectionHeader(
                        title = "Reading",
                        subtitle = "Last read",
                        theme = theme,
                        isAscending = false, // TODO: Connect to ViewModel sorting state
                        onSortClick = { /* TODO: Connect to ViewModel sorting functions */ },
                        onSeeAllClick = { onSeeAllClick("Reading", internalReadingBooks) }
                    )
                }
                item(key = "reading_books") {
                    ResponsiveBookSection(
                        books = internalReadingBooks,
                        showProgress = true
                    )
                }
            }

            if (internalPlanToReadBooks.isNotEmpty()) {
                item(key = "plan_to_read_header") {
                    LibrarySectionHeader(
                        title = "Plan to read",
                        subtitle = "Title",
                        theme = theme,
                        isAscending = true, // TODO: Connect to ViewModel sorting state
                        onSortClick = { /* TODO: Connect to ViewModel sorting functions */ },
                        onSeeAllClick = { onSeeAllClick("Plan to read", internalPlanToReadBooks) }
                    )
                }
                item(key = "plan_to_read_books") {
                    ResponsiveBookSection(
                        books = internalPlanToReadBooks,
                        showProgress = false
                    )
                }
            }

            if (internalCompletedBooks.isNotEmpty()) {
                item(key = "read_header") {
                    LibrarySectionHeader(
                        title = "Read",
                        subtitle = "Title",
                        theme = theme,
                        isAscending = true, // TODO: Connect to ViewModel sorting state
                        onSortClick = { /* TODO: Connect to ViewModel sorting functions */ },
                        onSeeAllClick = { onSeeAllClick("Read", internalCompletedBooks) }
                    )
                }
                item(key = "read_books") {
                    ResponsiveBookSection(
                        books = internalCompletedBooks,
                        showProgress = false
                    )
                }
            }

            if (internalReadingBooks.isEmpty() && internalPlanToReadBooks.isEmpty() && internalCompletedBooks.isEmpty()) {
                item {
                    Text(
                        text = "No books found.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        textAlign = TextAlign.Center,
                        color = theme.secondaryTextColor
                    )
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
            .padding(start = 16.dp, end = 4.dp),
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
