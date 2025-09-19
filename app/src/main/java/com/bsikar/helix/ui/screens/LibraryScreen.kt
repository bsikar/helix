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
import com.bsikar.helix.ui.components.InfiniteHorizontalBookScroll
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
    
    // Sort states
    var readingSortAscending by remember { mutableStateOf(false) }
    var planToReadSortAscending by remember { mutableStateOf(true) }
    var readSortAscending by remember { mutableStateOf(true) }

    // Step 3: Create filtered lists with sort applied (from recents - most recent to least)
    val allRecentBooks = remember {
        listOf(
            Book("Kaguya-sama", "Aka Akasaka", Color(0xFFFF69B4), 0.6f), // Most recent
            Book("Clockwork Planet", "Yuu Kamiya", Color(0xFFFFD700), 0.8f),
            Book("Akame ga Kill!", "Takahiro", Color(0xFF8B0000), 0.3f),
            Book("Dr. Stone", "Riichiro Inagaki", Color(0xFF00CED1), 0.4f),
            Book("Fire Force", "Atsushi Ohkubo", Color(0xFFDC143C), 0.7f),
            Book("Black Clover", "Yuki Tabata", Color(0xFF2F4F4F), 0.2f) // Least recent
        )
    }
    
    // Calculate how many books fit on screen for Reading section
    val booksVisibleOnScreen = 4 // Conservative estimate for most screens
    
    // Take only the books that fit on screen (most recent first)
    val readingBooks = remember(allRecentBooks) {
        allRecentBooks.take(booksVisibleOnScreen)
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
    val readBooks = remember {
        listOf(
            Book("Attack on Titan", "Hajime Isayama", Color(0xFF8B4513), 1.0f),
            Book("Death Note", "Tsugumi Ohba", Color(0xFF000000), 1.0f),
            Book("Fullmetal Alchemist", "Hiromu Arakawa", Color(0xFFFFD700), 1.0f),
            Book("Hunter x Hunter", "Yoshihiro Togashi", Color(0xFF228B22), 1.0f),
            Book("Tokyo Ghoul", "Sui Ishida", Color(0xFF8B0000), 1.0f),
            Book("Mob Psycho 100", "ONE", Color(0xFF9932CC), 1.0f),
            Book("Demon Slayer", "Koyoharu Gotouge", Color(0xFF2E8B57), 1.0f),
            Book("My Hero Academia", "Kohei Horikoshi", Color(0xFF32CD32), 1.0f),
            Book("Bleach", "Tite Kubo", Color(0xFF4B0082), 1.0f)
        )
    }

    val filteredReadingBooks = remember(searchQuery, readingBooks, readingSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            readingBooks
        } else {
            readingBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        }
        // Sort by recency (books are already ordered by most recent first in allRecentBooks)
        if (readingSortAscending) {
            filtered.reversed() // Least recent first
        } else {
            filtered // Most recent first (default order)
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
        // Sort by title
        if (planToReadSortAscending) {
            filtered.sortedBy { it.title }
        } else {
            filtered.sortedByDescending { it.title }
        }
    }

    val filteredReadBooks = remember(searchQuery, readBooks, readSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            readBooks
        } else {
            readBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        }
        // Sort by title
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
                    SectionHeader(
                        title = "Reading", 
                        subtitle = "Last read", 
                        theme = theme,
                        isAscending = readingSortAscending,
                        onSortClick = { readingSortAscending = !readingSortAscending }
                    )
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
                    SectionHeader(
                        title = "Plan to read", 
                        subtitle = "Title", 
                        theme = theme,
                        isAscending = planToReadSortAscending,
                        onSortClick = { planToReadSortAscending = !planToReadSortAscending }
                    )
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
            
            // Show "Read" section only if results exist
            if (filteredReadBooks.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Read", 
                        subtitle = "Title", 
                        theme = theme,
                        isAscending = readSortAscending,
                        onSortClick = { readSortAscending = !readSortAscending }
                    )
                }
                items(filteredReadBooks.chunked(3)) { rowItems ->
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

// A new, simple composable for the section headers to reduce repetition.
@Composable
fun SectionHeader(
    title: String, 
    subtitle: String, 
    theme: AppTheme,
    isAscending: Boolean = true,
    onSortClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
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
            IconButton(
                onClick = onSortClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    if (isAscending) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Sort ${if (isAscending) "ascending" else "descending"}",
                    modifier = Modifier.size(16.dp),
                    tint = theme.secondaryTextColor
                )
            }
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
