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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.ImportProgress
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.InfiniteHorizontalBookScroll
import com.bsikar.helix.ui.components.SearchBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    readingBooks: List<Book>,
    planToReadBooks: List<Book>,
    completedBooks: List<Book>,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProgressSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onSeeAllClick: (String, List<Book>) -> Unit = { _, _ -> },
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    libraryManager: LibraryManager? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Get import progress from LibraryManager
    val importProgress by libraryManager?.importProgress ?: remember { mutableStateOf<ImportProgress?>(null) }
    
    // Pull-to-refresh functionality  
    var scanMessage by remember { mutableStateOf("") }
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

    var readingSortAscending by remember { mutableStateOf(false) }
    var planToReadSortAscending by remember { mutableStateOf(true) }
    var readSortAscending by remember { mutableStateOf(true) }

    val filteredReadingBooks = remember(searchQuery, readingBooks, readingSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            readingBooks
        } else {
            readingBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        }
        if (readingSortAscending) {
            filtered.reversed()
        } else {
            filtered
        }
    }

    val filteredPlanToReadBooks = remember(searchQuery, planToReadBooks, planToReadSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            planToReadBooks
        } else {
            planToReadBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        }
        if (planToReadSortAscending) {
            filtered.sortedBy { it.title }
        } else {
            filtered.sortedByDescending { it.title }
        }
    }

    val filteredReadBooks = remember(searchQuery, completedBooks, readSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            completedBooks
        } else {
            completedBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        }
        if (readSortAscending) {
            filtered.sortedBy { it.title }
        } else {
            filtered.sortedByDescending { it.title }
        }
    }


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
                        "Library",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryTextColor
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
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
                            contentDescription = "Library",
                            tint = if (selectedTab == 0) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    label = {
                        Text(
                            "Library",
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
                            contentDescription = "Recents",
                            tint = if (selectedTab == 1) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    label = {
                        Text(
                            "Recents",
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
                            contentDescription = "Browse",
                            tint = if (selectedTab == 2) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    label = {
                        Text(
                            "Browse",
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

            item {
                SearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    theme = theme
                )
            }
            
            // Show import progress in library view
            importProgress?.let { progress ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onNavigateToProgressSettings() },
                        colors = CardDefaults.cardColors(containerColor = theme.surfaceColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = progress.percentage / 100f,
                                modifier = Modifier.size(20.dp),
                                color = theme.accentColor,
                                strokeWidth = 2.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Importing books... ${progress.percentage}%",
                                    color = theme.primaryTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${progress.current} of ${progress.total} files",
                                    color = theme.secondaryTextColor,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = "Go to Settings",
                                tint = theme.secondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Show scan status message
            if (scanMessage.isNotEmpty()) {
                item {
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

            if (filteredReadingBooks.isNotEmpty()) {
                item {
                    LibrarySectionHeader(
                        title = "Reading",
                        subtitle = "Last read",
                        theme = theme,
                        isAscending = readingSortAscending,
                        onSortClick = { readingSortAscending = !readingSortAscending },
                        onSeeAllClick = { onSeeAllClick("Reading", filteredReadingBooks) }
                    )
                }
                item {
                    InfiniteHorizontalBookScroll(
                        books = filteredReadingBooks,
                        showProgress = true,
                        theme = theme,
                        searchQuery = searchQuery,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        onBookClick = onBookClick,
                        onStartReading = onStartReading,
                        onMarkCompleted = onMarkCompleted,
                        onMoveToPlanToRead = onMoveToPlanToRead,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags
                    )
                }
            }

            if (filteredPlanToReadBooks.isNotEmpty()) {
                item {
                    LibrarySectionHeader(
                        title = "Plan to read",
                        subtitle = "Title",
                        theme = theme,
                        isAscending = planToReadSortAscending,
                        onSortClick = { planToReadSortAscending = !planToReadSortAscending },
                        onSeeAllClick = { onSeeAllClick("Plan to read", filteredPlanToReadBooks) }
                    )
                }
                item {
                    InfiniteHorizontalBookScroll(
                        books = filteredPlanToReadBooks,
                        showProgress = false,
                        theme = theme,
                        searchQuery = searchQuery,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        onBookClick = onBookClick,
                        onStartReading = onStartReading,
                        onMarkCompleted = onMarkCompleted,
                        onMoveToPlanToRead = onMoveToPlanToRead,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags
                    )
                }
            }

            if (filteredReadBooks.isNotEmpty()) {
                item {
                    LibrarySectionHeader(
                        title = "Read",
                        subtitle = "Title",
                        theme = theme,
                        isAscending = readSortAscending,
                        onSortClick = { readSortAscending = !readSortAscending },
                        onSeeAllClick = { onSeeAllClick("Read", filteredReadBooks) }
                    )
                }
                item {
                    InfiniteHorizontalBookScroll(
                        books = filteredReadBooks,
                        showProgress = false,
                        theme = theme,
                        searchQuery = searchQuery,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        onBookClick = onBookClick,
                        onStartReading = onStartReading,
                        onMarkCompleted = onMarkCompleted,
                        onMoveToPlanToRead = onMoveToPlanToRead,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags
                    )
                }
            }

            if (filteredReadingBooks.isEmpty() && filteredPlanToReadBooks.isEmpty() && filteredReadBooks.isEmpty()) {
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
