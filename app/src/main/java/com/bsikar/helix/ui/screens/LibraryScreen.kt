package com.bsikar.helix.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
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
    onBookClick: (Book) -> Unit = {},
    onSeeAllClick: (String, List<Book>) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }

    var readingSortAscending by remember { mutableStateOf(false) }
    var planToReadSortAscending by remember { mutableStateOf(true) }
    var readSortAscending by remember { mutableStateOf(true) }

    val allRecentBooks = remember {
        listOf(
            Book("Kaguya-sama", "Aka Akasaka", Color(0xFFFF69B4), 0.6f),
            Book("Clockwork Planet", "Yuu Kamiya", Color(0xFFFFD700), 0.8f),
            Book("Akame ga Kill!", "Takahiro", Color(0xFF8B0000), 0.3f),
            Book("Dr. Stone", "Riichiro Inagaki", Color(0xFF00CED1), 0.4f),
            Book("Fire Force", "Atsushi Ohkubo", Color(0xFFDC143C), 0.7f),
            Book("Black Clover", "Yuki Tabata", Color(0xFF2F4F4F), 0.2f)
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
            Book("Bleach", "Tite Kubo", Color(0xFF4B0082), 1.0f),
            Book("Naruto", "Masashi Kishimoto", Color(0xFFFF8C00), 1.0f),
            Book("One Piece", "Eiichiro Oda", Color(0xFF4169E1), 1.0f),
            Book("Dragon Ball", "Akira Toriyama", Color(0xFFDB4437), 1.0f),
            Book("Berserk", "Kentaro Miura", Color(0xFF424242), 1.0f),
            Book("Vagabond", "Takehiko Inoue", Color(0xFF795548), 1.0f),
            Book("Slam Dunk", "Takehiko Inoue", Color(0xFFD32F2F), 1.0f),
            Book("JoJo's Bizarre Adventure", "Hirohiko Araki", Color(0xFFFF6347), 1.0f),
            Book("The Promised Neverland", "Kaiu Shirai", Color(0xFF8FBC8F), 1.0f),
            Book("Haikyuu!!", "Haruichi Furudate", Color(0xFFFF7F50), 1.0f),
            Book("Spy x Family", "Tatsuya Endo", Color(0xFFFF1493), 1.0f),
            Book("Chainsaw Man", "Tatsuki Fujimoto", Color(0xFFFF4500), 1.0f),
            Book("Jujutsu Kaisen", "Gege Akutami", Color(0xFF8A2BE2), 1.0f),
            Book("Vinland Saga", "Makoto Yukimura", Color(0xFFC0C0C0), 1.0f),
            Book("Blue Lock", "Muneyuki Kaneshiro", Color(0xFF0000FF), 1.0f)
        )
    }

    val filteredReadingBooks = remember(searchQuery, allRecentBooks, readingSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            allRecentBooks
        } else {
            allRecentBooks.filter { book ->
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

    val filteredReadBooks = remember(searchQuery, readBooks, readSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            readBooks
        } else {
            readBooks.filter { book ->
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        onBookClick = onBookClick
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        onBookClick = onBookClick
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        onBookClick = onBookClick
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
