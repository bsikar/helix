package com.bsikar.helix.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.R
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.repository.BookRepository
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.HelixBottomNavigation
import com.bsikar.helix.ui.components.ResponsiveSpacing
import com.bsikar.helix.ui.components.SearchBar
import com.bsikar.helix.ui.components.SearchUtils
import com.bsikar.helix.ui.components.TagEditorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    selectedTab: MainTab = MainTab.Home,
    onTabSelected: (MainTab) -> Unit = {},
    isPlayingVisible: Boolean = false,
    isPlayingActive: Boolean = false,
    onPlayingClick: () -> Unit = {},
    playingLabel: String? = null,
    theme: AppTheme,
    recentBooks: List<com.bsikar.helix.data.model.Book>,
    onNavigateToSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Recent") }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Filter and sort recent books
    val filteredRecentBooks = remember(searchQuery, sortBy, recentBooks) {
        val filtered = if (searchQuery.isBlank()) {
            recentBooks.map { SearchUtils.SearchResult(it, 1.0) }
        } else {
            SearchUtils.fuzzySearch(
                items = recentBooks,
                query = searchQuery,
                getText = { it.title },
                getSecondaryText = { it.author },
                threshold = 0.3
            )
        }
        
        val books = filtered.map { it.item }
        when (sortBy) {
            "Recent" -> books.sortedByDescending { it.lastReadTimestamp }
            "Title" -> books.sortedBy { it.title }
            "Progress" -> books.sortedByDescending { if (it.isAudiobook()) it.getAudioProgress() else it.progress }
            else -> books
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
                        stringResource(R.string.recents),
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
            HelixBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                isPlayingVisible = isPlayingVisible,
                isPlayingActive = isPlayingActive,
                onPlayingClick = onPlayingClick,
                playingTitle = playingLabel,
                theme = theme
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = isRefreshing,
            onRefresh = { 
                isRefreshing = true
                onRefresh()
                isRefreshing = false
            },
            state = pullToRefreshState
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // Search Bar
            item {
                SearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    theme = theme
                )
            }

            // Sort options
            item {
                SortOptionsRow(
                    sortBy = sortBy,
                    onSortChange = { sortBy = it },
                    theme = theme
                )
            }

            // Recent books list
            if (filteredRecentBooks.isNotEmpty()) {
                items(filteredRecentBooks, key = { it.id }) { book ->
                    RecentBookItem(
                        book = book,
                        theme = theme,
                        searchQuery = searchQuery,
                        onBookClick = { onBookClick(book) },
                        onStartReading = onStartReading,
                        onMarkCompleted = onMarkCompleted,
                        onMoveToOnDeck = onMoveToOnDeck,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags
                    )
                }
            } else {
                item {
                    Text(
                        text = if (searchQuery.isBlank()) "No recent books" else "No books found",
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
fun SortOptionsRow(
    sortBy: String,
    onSortChange: (String) -> Unit,
    theme: AppTheme
) {
    val sortOptions = listOf("Recent", "Title", "Progress")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ResponsiveSpacing.medium()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Sort by:",
            fontSize = 14.sp,
            color = theme.secondaryTextColor,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        
        sortOptions.forEach { option ->
            FilterChip(
                onClick = { onSortChange(option) },
                label = {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        color = if (sortBy == option) {
                            theme.surfaceColor
                        } else {
                            theme.primaryTextColor
                        }
                    )
                },
                selected = sortBy == option,
                enabled = true,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = theme.accentColor,
                    containerColor = theme.surfaceColor,
                    selectedLabelColor = theme.surfaceColor,
                    labelColor = theme.primaryTextColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = theme.secondaryTextColor.copy(alpha = 0.3f),
                    selectedBorderColor = theme.accentColor,
                    enabled = true,
                    selected = sortBy == option
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentBookItem(
    book: com.bsikar.helix.data.model.Book,
    theme: AppTheme,
    searchQuery: String = "",
    onBookClick: () -> Unit,
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> }
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ResponsiveSpacing.medium())
            .combinedClickable(
                onClick = { onBookClick() },
                onLongClick = { showContextMenu = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = theme.surfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Book cover
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(book.getEffectiveCoverColor())
            ) {
                // Display cover art if available and display mode allows it
                if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(book.coverImagePath),
                        contentDescription = "Book cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        fallback = null, // Fall back to background color when image fails
                        error = null // Show background color on error
                    )
                }
            }
            
            // Book info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = SearchUtils.createHighlightedText(
                            text = book.title,
                            query = searchQuery,
                            baseColor = theme.primaryTextColor,
                            highlightColor = theme.accentColor,
                            fontSize = 16.sp,
                            highlightFontWeight = FontWeight.Bold
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = SearchUtils.createHighlightedText(
                            text = book.author,
                            query = searchQuery,
                            baseColor = theme.secondaryTextColor,
                            highlightColor = theme.accentColor,
                            fontSize = 13.sp,
                            highlightFontWeight = FontWeight.Bold
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress and time info
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format("%.2f", (if (book.isAudiobook()) book.getAudioProgress() else book.progress) * 100)}%",
                            fontSize = 12.sp,
                            color = theme.accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Custom progress bar using Box to avoid LinearProgressIndicator artifacts
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                color = theme.secondaryTextColor.copy(alpha = theme.alphaSubtle),
                                shape = RoundedCornerShape(2.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((if (book.isAudiobook()) book.getAudioProgress() else book.progress).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    color = theme.accentColor,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            
            // Last accessed time
            Text(
                text = book.getTimeAgoText(),
                fontSize = 11.sp,
                color = theme.secondaryTextColor,
                modifier = Modifier.align(Alignment.Top)
            )
        }
    }
    
    if (showContextMenu) {
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.background(theme.surfaceColor)
        ) {
            when (book.readingStatus) {
                ReadingStatus.UNREAD -> {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Add to Plan to Read",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMoveToOnDeck(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = theme.secondaryTextColor
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Start Reading",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onStartReading(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = theme.accentColor
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Mark as Read",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMarkCompleted(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = theme.successColor
                            )
                        }
                    )
                }
                ReadingStatus.PLAN_TO_READ -> {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Start Reading",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onStartReading(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = theme.accentColor
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Mark as Read",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMarkCompleted(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = theme.successColor
                            )
                        }
                    )
                }
                ReadingStatus.PLAN_TO_LISTEN -> {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Start Listening",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onStartReading(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = theme.accentColor
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Mark as Finished",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMarkCompleted(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = theme.successColor
                            )
                        }
                    )
                }
                ReadingStatus.LISTENING -> {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Mark as Completed",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMarkCompleted(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = theme.accentColor
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Move to Plan to Listen",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMoveToOnDeck(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = theme.secondaryTextColor
                            )
                        }
                    )
                }
                ReadingStatus.READING -> {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Mark as Completed",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMarkCompleted(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = theme.accentColor
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Move to Plan to Read",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMoveToOnDeck(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = theme.secondaryTextColor
                            )
                        }
                    )
                }
                ReadingStatus.COMPLETED -> {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Move to Plan to Read",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onMoveToOnDeck(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = theme.secondaryTextColor
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Mark as Reading",
                                color = theme.primaryTextColor
                            )
                        },
                        onClick = {
                            onStartReading(book.id)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Book,
                                contentDescription = null,
                                tint = theme.accentColor
                            )
                        }
                    )
                }
            }
            
            // Add "Edit Tags" option for all books
            DropdownMenuItem(
                text = { 
                    Text(
                        "Edit Tags",
                        color = theme.primaryTextColor
                    )
                },
                onClick = {
                    showTagEditor = true
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = theme.accentColor
                    )
                }
            )
        }
    }
    
    // Tag Editor Dialog
    if (showTagEditor) {
        TagEditorDialog(
            book = book,
            theme = theme,
            onDismiss = { showTagEditor = false },
            onTagsUpdated = { newTags: List<String> ->
                onEditTags(book.id, newTags)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RecentsScreenPreview() {
    val theme = ThemeManager.lightTheme
    MaterialTheme {
        RecentsScreen(
            selectedTab = MainTab.Home,
            theme = theme,
            recentBooks = emptyList(),
            onNavigateToSettings = { },
            onBookClick = { }
        )
    }
}