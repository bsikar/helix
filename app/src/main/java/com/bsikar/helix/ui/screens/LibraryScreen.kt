package com.bsikar.helix.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.BookCard
import com.bsikar.helix.ui.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    onNavigateToSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {}
) {
    // Step 2: Manage search state here
    var searchQuery by remember { mutableStateOf("") }

    // Step 3: Create filtered lists
    val readingBooks = remember {
        listOf(
            Book("Clockwork Planet", "Yuu Kamiya", Color(0xFFFFD700), 0.8f),
            Book("Akame ga Kill!", "Takahiro", Color(0xFF8B0000), 0.3f),
            Book("Kaguya-sama", "Aka Akasaka", Color(0xFFFF69B4), 0.6f)
        )
    }
    val planToReadBooks = remember {
        listOf(
            Book("Akane-Banashi", "Yuki Suenaga", Color(0xFF4169E1)),
            Book("Dandadan", "Yukinobu Tatsu", Color(0xFF32CD32)),
            Book("Jujutsu Kaisen", "Gege Akutami", Color(0xFF8A2BE2)),
            Book("Chainsaw Man", "Tatsuki Fujimoto", Color(0xFFFF4500)),
            Book("One Punch Man", "ONE", Color(0xFFD4AF37)),
            Book("Vinland Saga", "Makoto Yukimura", Color(0xFFC0C0C0))
        )
    }

    val filteredReadingBooks = remember(searchQuery, readingBooks) {
        if (searchQuery.isBlank()) {
            readingBooks
        } else {
            readingBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredPlanToReadBooks = remember(searchQuery, planToReadBooks) {
        if (searchQuery.isBlank()) {
            planToReadBooks
        } else {
            planToReadBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
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
                // Your NavigationBarItems remain the same...
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

        // This is now the single source of truth for vertical scrolling.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Pass state to the SearchBar
            item {
                SearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    theme = theme
                )
            }

            // Show "Reading" section only if results exist
            if (filteredReadingBooks.isNotEmpty()) {
                item {
                    SectionHeader(title = "Reading", subtitle = "Last read", theme = theme)
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(filteredReadingBooks) { book ->
                            BookCard(
                                book = book, 
                                showProgress = true, 
                                theme = theme,
                                onBookClick = onBookClick
                            )
                        }
                    }
                }
            }

            // Show "Plan to read" section only if results exist
            if (filteredPlanToReadBooks.isNotEmpty()) {
                item {
                    SectionHeader(title = "Plan to read", subtitle = "Title", theme = theme)
                }
                items(filteredPlanToReadBooks.chunked(3)) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (book in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                BookCard(
                                    book = book, 
                                    showProgress = false, 
                                    theme = theme,
                                    onBookClick = onBookClick
                                )
                            }
                        }
                        // This handles cases where the last row isn't full
                        if (rowItems.size < 3) {
                            for (i in 0 until (3 - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            // Optional: Show a message if no results are found at all
            if (filteredReadingBooks.isEmpty() && filteredPlanToReadBooks.isEmpty()) {
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

// A new, simple composable for the section headers to reduce repetition.
@Composable
fun SectionHeader(title: String, subtitle: String, theme: AppTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• $title",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.primaryTextColor
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = theme.secondaryTextColor
            )
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Sort",
                modifier = Modifier.size(16.dp),
                tint = theme.secondaryTextColor
            )
            IconButton(onClick = { }) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(18.dp),
                    tint = theme.secondaryTextColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview() {
    val theme = ThemeManager.lightTheme
    MaterialTheme {
        LibraryScreen(
            selectedTab = 0,
            onTabSelected = { },
            currentTheme = ThemeMode.LIGHT,
            onThemeChange = { },
            theme = theme,
            onNavigateToSettings = { }
        )
    }
}
