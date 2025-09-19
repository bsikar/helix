package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.bsikar.helix.data.RecentBook
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    selectedTab: Int = 1,
    onTabSelected: (Int) -> Unit = {},
    theme: AppTheme,
    onNavigateToSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Recent") }
    
    // Sample recent books data
    val recentBooks = remember {
        val currentTime = System.currentTimeMillis()
        listOf(
            RecentBook(
                book = Book("Clockwork Planet", "Yuu Kamiya", Color(0xFFFFD700), 0.8f),
                lastAccessTime = currentTime - (2 * 60 * 1000), // 2 minutes ago
                lastReadPage = 120,
                totalPages = 150
            ),
            RecentBook(
                book = Book("Akame ga Kill!", "Takahiro", Color(0xFF8B0000), 0.3f),
                lastAccessTime = currentTime - (1 * 60 * 60 * 1000), // 1 hour ago
                lastReadPage = 45,
                totalPages = 150
            ),
            RecentBook(
                book = Book("Death Note", "Tsugumi Ohba", Color(0xFF000000), 0.6f),
                lastAccessTime = currentTime - (3 * 60 * 60 * 1000), // 3 hours ago
                lastReadPage = 90,
                totalPages = 150
            ),
            RecentBook(
                book = Book("One Piece", "Eiichiro Oda", Color(0xFF4169E1), 0.2f),
                lastAccessTime = currentTime - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                lastReadPage = 30,
                totalPages = 150
            ),
            RecentBook(
                book = Book("Demon Slayer", "Koyoharu Gotouge", Color(0xFF2E8B57), 0.9f),
                lastAccessTime = currentTime - (2 * 24 * 60 * 60 * 1000), // 2 days ago
                lastReadPage = 135,
                totalPages = 150
            ),
            RecentBook(
                book = Book("Spy x Family", "Tatsuya Endo", Color(0xFFFF1493), 0.4f),
                lastAccessTime = currentTime - (5 * 24 * 60 * 60 * 1000), // 5 days ago
                lastReadPage = 60,
                totalPages = 150
            )
        )
    }
    
    // Filter and sort recent books
    val filteredRecentBooks = remember(searchQuery, sortBy, recentBooks) {
        val filtered = if (searchQuery.isBlank()) {
            recentBooks
        } else {
            recentBooks.filter { recentBook ->
                recentBook.book.title.contains(searchQuery, ignoreCase = true) ||
                        recentBook.book.author.contains(searchQuery, ignoreCase = true)
            }
        }
        
        when (sortBy) {
            "Recent" -> filtered.sortedByDescending { it.lastAccessTime }
            "Title" -> filtered.sortedBy { it.book.title }
            "Progress" -> filtered.sortedByDescending { it.book.progress }
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
                        indicatorColor = theme.accentColor.copy(alpha = 0.1f)
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
                        indicatorColor = theme.accentColor.copy(alpha = 0.1f)
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
                        indicatorColor = theme.accentColor.copy(alpha = 0.1f)
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
                items(filteredRecentBooks) { recentBook ->
                    RecentBookItem(
                        recentBook = recentBook,
                        theme = theme,
                        onBookClick = { onBookClick(recentBook.book) }
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

@Composable
fun RecentBookItem(
    recentBook: RecentBook,
    theme: AppTheme,
    onBookClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onBookClick() },
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
                    .background(recentBook.book.coverColor)
            )
            
            // Book info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = recentBook.book.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primaryTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = recentBook.book.author,
                        fontSize = 13.sp,
                        color = theme.secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress and time info
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${recentBook.lastReadPage} of ${recentBook.totalPages}",
                            fontSize = 12.sp,
                            color = theme.secondaryTextColor
                        )
                        Text(
                            text = "${recentBook.getProgressPercentage()}%",
                            fontSize = 12.sp,
                            color = theme.accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LinearProgressIndicator(
                        progress = { recentBook.book.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = theme.accentColor,
                        trackColor = theme.secondaryTextColor.copy(alpha = 0.2f)
                    )
                }
            }
            
            // Last accessed time
            Text(
                text = recentBook.getTimeAgoText(),
                fontSize = 11.sp,
                color = theme.secondaryTextColor,
                modifier = Modifier.align(Alignment.Top)
            )
        }
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
            onNavigateToSettings = { },
            onBookClick = { }
        )
    }
}