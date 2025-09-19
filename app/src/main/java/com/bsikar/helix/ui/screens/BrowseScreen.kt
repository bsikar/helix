package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
fun BrowseScreen(
    selectedTab: Int = 2,
    onTabSelected: (Int) -> Unit = {},
    theme: AppTheme,
    onNavigateToSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onSeeAllClick: (String, List<Book>) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }

    // Sample browse data organized by categories
    val genres = listOf("All", "Manga", "Light Novel", "Manhwa", "Manhua", "Classic")
    
    val featuredBooks = remember {
        listOf(
            Book("Attack on Titan", "Hajime Isayama", Color(0xFF8B4513)),
            Book("Death Note", "Tsugumi Ohba", Color(0xFF000000)),
            Book("One Piece", "Eiichiro Oda", Color(0xFF4169E1)),
            Book("Naruto", "Masashi Kishimoto", Color(0xFFFF8C00))
        )
    }
    
    val popularBooks = remember {
        listOf(
            Book("Demon Slayer", "Koyoharu Gotouge", Color(0xFF2E8B57)),
            Book("My Hero Academia", "Kohei Horikoshi", Color(0xFF32CD32)),
            Book("Tokyo Ghoul", "Sui Ishida", Color(0xFF8B0000)),
            Book("Fullmetal Alchemist", "Hiromu Arakawa", Color(0xFFFFD700)),
            Book("Hunter x Hunter", "Yoshihiro Togashi", Color(0xFF228B22)),
            Book("Bleach", "Tite Kubo", Color(0xFF4B0082)),
            Book("Mob Psycho 100", "ONE", Color(0xFF9932CC)),
            Book("Dr. Stone", "Riichiro Inagaki", Color(0xFF00CED1)),
            Book("JoJo's Bizarre Adventure", "Hirohiko Araki", Color(0xFFFF6347)),
            Book("Black Clover", "Yuki Tabata", Color(0xFF2F4F4F)),
            Book("Fire Force", "Atsushi Ohkubo", Color(0xFFDC143C)),
            Book("The Promised Neverland", "Kaiu Shirai", Color(0xFF8FBC8F)),
            Book("Haikyuu!!", "Haruichi Furudate", Color(0xFFFF7F50)),
            Book("One Piece", "Eiichiro Oda", Color(0xFF4169E1))
        )
    }
    
    val newReleases = remember {
        listOf(
            Book("Spy x Family", "Tatsuya Endo", Color(0xFFFF1493)),
            Book("Hell's Paradise", "Yuji Kaku", Color(0xFF8B008B)),
            Book("Kaiju No. 8", "Naoya Matsumoto", Color(0xFF00CED1)),
            Book("Blue Lock", "Muneyuki Kaneshiro", Color(0xFF0000FF))
        )
    }

    // Filter books based on search query and selected genre
    val filteredFeatured = remember(searchQuery, selectedGenre, featuredBooks) {
        filterBooks(featuredBooks, searchQuery, selectedGenre)
    }
    
    val filteredPopular = remember(searchQuery, selectedGenre, popularBooks) {
        filterBooks(popularBooks, searchQuery, selectedGenre)
    }
    
    val filteredNewReleases = remember(searchQuery, selectedGenre, newReleases) {
        filterBooks(newReleases, searchQuery, selectedGenre)
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
                        "Browse",
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
            // Search Bar
            item {
                SearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    theme = theme
                )
            }

            // Genre Filter
            item {
                GenreFilterRow(
                    genres = genres,
                    selectedGenre = selectedGenre,
                    onGenreSelected = { selectedGenre = it },
                    theme = theme
                )
            }

            // Featured Section
            if (filteredFeatured.isNotEmpty()) {
                item {
                    BrowseSectionHeader(
                        title = "Featured", 
                        theme = theme,
                        onSeeAllClick = { onSeeAllClick("Featured", filteredFeatured) }
                    )
                }
                item {
                    InfiniteHorizontalScroll(
                        books = filteredFeatured,
                        theme = theme,
                        onBookClick = onBookClick
                    )
                }
            }

            // Popular Section
            if (filteredPopular.isNotEmpty()) {
                item {
                    BrowseSectionHeader(
                        title = "Popular", 
                        theme = theme,
                        onSeeAllClick = { onSeeAllClick("Popular", filteredPopular) }
                    )
                }
                item {
                    InfiniteHorizontalScroll(
                        books = filteredPopular,
                        theme = theme,
                        onBookClick = onBookClick
                    )
                }
            }

            // New Releases Section
            if (filteredNewReleases.isNotEmpty()) {
                item {
                    BrowseSectionHeader(
                        title = "New Releases", 
                        theme = theme,
                        onSeeAllClick = { onSeeAllClick("New Releases", filteredNewReleases) }
                    )
                }
                item {
                    InfiniteHorizontalScroll(
                        books = filteredNewReleases,
                        theme = theme,
                        onBookClick = onBookClick
                    )
                }
            }

            // No results message
            if (filteredFeatured.isEmpty() && filteredPopular.isEmpty() && filteredNewReleases.isEmpty()) {
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
fun GenreFilterRow(
    genres: List<String>,
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    theme: AppTheme
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(genres) { genre ->
            FilterChip(
                onClick = { onGenreSelected(genre) },
                label = {
                    Text(
                        text = genre,
                        fontSize = 13.sp,
                        color = if (selectedGenre == genre) {
                            theme.surfaceColor
                        } else {
                            theme.primaryTextColor
                        }
                    )
                },
                selected = selectedGenre == genre,
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
                    selected = selectedGenre == genre
                )
            )
        }
    }
}

@Composable
fun BrowseSectionHeader(
    title: String, 
    theme: AppTheme,
    onSeeAllClick: () -> Unit = {}
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
        TextButton(
            onClick = onSeeAllClick
        ) {
            Text(
                text = "See All",
                fontSize = 13.sp,
                color = theme.accentColor
            )
        }
    }
}

@Composable
fun InfiniteHorizontalScroll(
    books: List<Book>,
    theme: AppTheme,
    onBookClick: (Book) -> Unit
) {
    // Use the new circular implementation from BookSection
    InfiniteHorizontalBookScroll(
        books = books,
        showProgress = false,
        theme = theme,
        contentPadding = PaddingValues(horizontal = 16.dp),
        onBookClick = onBookClick
    )
}

// Helper function to filter books
private fun filterBooks(books: List<Book>, searchQuery: String, selectedGenre: String): List<Book> {
    return books.filter { book ->
        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            book.title.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true)
        }
        
        // For now, treat all books as "Manga" genre since we don't have genre data
        val matchesGenre = selectedGenre == "All" || selectedGenre == "Manga"
        
        matchesSearch && matchesGenre
    }
}

@Preview(showBackground = true)
@Composable
fun BrowseScreenPreview() {
    val theme = ThemeManager.lightTheme
    MaterialTheme {
        BrowseScreen(
            theme = theme,
            onNavigateToSettings = { }
        )
    }
}