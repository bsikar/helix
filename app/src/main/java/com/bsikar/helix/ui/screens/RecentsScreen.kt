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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.BookRepository
import com.bsikar.helix.data.ReadingStatus
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.SearchBar
import com.bsikar.helix.ui.components.TagEditorDialog
import com.bsikar.helix.ui.components.HighlightedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    selectedTab: Int = 1,
    onTabSelected: (Int) -> Unit = {},
    theme: AppTheme,
    recentBooks: List<Book>,
    onNavigateToSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Recent") }
    
    // Filter and sort recent books
    val filteredRecentBooks = remember(searchQuery, sortBy, recentBooks) {
        val filtered = if (searchQuery.isBlank()) {
            recentBooks
        } else {
            recentBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        }
        
        when (sortBy) {
            "Recent" -> filtered.sortedByDescending { it.lastReadTimestamp }
            "Title" -> filtered.sortedBy { it.title }
            "Progress" -> filtered.sortedByDescending { it.progress }
            else -> filtered
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
                        "Recents",
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
                        onMoveToPlanToRead = onMoveToPlanToRead,
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
            .padding(horizontal = 16.dp),
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
    book: Book,
    theme: AppTheme,
    searchQuery: String = "",
    onBookClick: () -> Unit,
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> }
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
                    .background(book.coverColor)
            )
            
            // Book info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    HighlightedText(
                        text = book.title,
                        searchQuery = searchQuery,
                        normalColor = theme.primaryTextColor,
                        highlightColor = theme.accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    HighlightedText(
                        text = book.author,
                        searchQuery = searchQuery,
                        normalColor = theme.secondaryTextColor,
                        highlightColor = theme.accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
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
                            text = "${(book.progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = theme.accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = theme.accentColor,
                        trackColor = theme.secondaryTextColor.copy(alpha = 0.2f)
                    )
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
                            onMoveToPlanToRead(book.id)
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
                            onMoveToPlanToRead(book.id)
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
            selectedTab = 1,
            theme = theme,
            recentBooks = BookRepository.getRecentBooks(),
            onNavigateToSettings = { },
            onBookClick = { }
        )
    }
}