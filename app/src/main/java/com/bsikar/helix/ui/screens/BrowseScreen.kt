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
import com.bsikar.helix.data.Tag
import com.bsikar.helix.data.TagCategory
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
    onSeeAllClick: (String, List<Book>) -> Unit = { _, _ -> },
    allBooks: List<Book> = emptyList(),
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedCategory by remember { mutableStateOf<TagCategory?>(null) }
    
    // Organize books by categories based on tags
    val featuredBooks = remember(allBooks) {
        allBooks.filter { book -> 
            book.hasAnyTag(listOf("shounen", "seinen")) || book.progress > 0.5f
        }.take(6)
    }
    
    val popularBooks = remember(allBooks) {
        allBooks.filter { book -> 
            book.hasAnyTag(listOf("action", "adventure", "fantasy", "supernatural"))
        }.take(12)
    }
    
    val newReleases = remember(allBooks) {
        allBooks.filter { book -> 
            book.hasAnyTag(listOf("ongoing", "romance", "comedy"))
        }.take(6)
    }

    // Get all unique tags that exist in the library, organized by all categories
    val availableTagsByCategory = remember(allBooks) {
        val allTagsInLibrary = allBooks.flatMap { it.getTagObjects() }.distinctBy { it.id }
        TagCategory.values().filter { it != TagCategory.CUSTOM }.associateWith { category ->
            allTagsInLibrary.filter { it.category == category }
        }.filterValues { it.isNotEmpty() }
    }

    // Calculate which tags are available given current filter selection
    val availableTagsForCurrentFilters = remember(allBooks, selectedTags) {
        if (selectedTags.isEmpty()) {
            availableTagsByCategory
        } else {
            // Find books that match current filters
            val filteredBooks = allBooks.filter { book ->
                selectedTags.all { tag -> book.hasTag(tag) }
            }
            
            // Get tags available in filtered books
            val availableTagsInFilteredBooks = filteredBooks.flatMap { it.getTagObjects() }.distinctBy { it.id }
            
            TagCategory.values().filter { it != TagCategory.CUSTOM }.associateWith { category ->
                availableTagsInFilteredBooks.filter { it.category == category }
            }.filterValues { it.isNotEmpty() }
        }
    }

    // Filter books based on search query and selected tags (multiple)
    val filteredFeatured = remember(searchQuery, selectedTags, featuredBooks) {
        filterBooksByTags(featuredBooks, searchQuery, selectedTags)
    }
    
    val filteredPopular = remember(searchQuery, selectedTags, popularBooks) {
        filterBooksByTags(popularBooks, searchQuery, selectedTags)
    }
    
    val filteredNewReleases = remember(searchQuery, selectedTags, newReleases) {
        filterBooksByTags(newReleases, searchQuery, selectedTags)
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

            // Collapsible Tag Filters (All Categories) with Clear Filters
            if (availableTagsByCategory.isNotEmpty()) {
                item {
                    CollapsibleAllCategoriesFilter(
                        availableTagsByCategory = availableTagsByCategory,
                        availableTagsForCurrentFilters = availableTagsForCurrentFilters,
                        selectedTags = selectedTags,
                        expandedCategory = expandedCategory,
                        onTagToggled = { tagId ->
                            selectedTags = if (selectedTags.contains(tagId)) {
                                selectedTags - tagId
                            } else {
                                selectedTags + tagId
                            }
                        },
                        onClearFilters = { selectedTags = emptySet() },
                        onCategoryToggled = { category ->
                            expandedCategory = if (expandedCategory == category) null else category
                        },
                        theme = theme
                    )
                }
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
                        searchQuery = searchQuery,
                        onBookClick = onBookClick,
                        onStartReading = onStartReading,
                        onMarkCompleted = onMarkCompleted,
                        onMoveToPlanToRead = onMoveToPlanToRead,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags
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
                        searchQuery = searchQuery,
                        onBookClick = onBookClick,
                        onStartReading = onStartReading,
                        onMarkCompleted = onMarkCompleted,
                        onMoveToPlanToRead = onMoveToPlanToRead,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags
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
                        searchQuery = searchQuery,
                        onBookClick = onBookClick,
                        onStartReading = onStartReading,
                        onMarkCompleted = onMarkCompleted,
                        onMoveToPlanToRead = onMoveToPlanToRead,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags
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
    searchQuery: String = "",
    onBookClick: (Book) -> Unit,
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> }
) {
    // Use the new circular implementation from BookSection
    InfiniteHorizontalBookScroll(
        books = books,
        showProgress = false,
        theme = theme,
        searchQuery = searchQuery,
        contentPadding = PaddingValues(horizontal = 16.dp),
        onBookClick = onBookClick,
        onStartReading = onStartReading,
        onMarkCompleted = onMarkCompleted,
        onMoveToPlanToRead = onMoveToPlanToRead,
        onSetProgress = onSetProgress,
        onEditTags = onEditTags
    )
}

// Helper function to filter books by multiple tags
private fun filterBooksByTags(books: List<Book>, searchQuery: String, selectedTags: Set<String>): List<Book> {
    return books.filter { book ->
        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            book.title.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true)
        }
        
        val matchesTags = if (selectedTags.isEmpty()) {
            true
        } else {
            selectedTags.all { tag -> book.hasTag(tag) }
        }
        
        matchesSearch && matchesTags
    }
}

@Composable
fun CollapsibleAllCategoriesFilter(
    availableTagsByCategory: Map<TagCategory, List<Tag>>,
    availableTagsForCurrentFilters: Map<TagCategory, List<Tag>>,
    selectedTags: Set<String>,
    expandedCategory: TagCategory?,
    onTagToggled: (String) -> Unit,
    onClearFilters: () -> Unit,
    onCategoryToggled: (TagCategory) -> Unit,
    theme: AppTheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active Filters and Clear Filters Row
        if (selectedTags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Filters (${selectedTags.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.primaryTextColor
                )
                TextButton(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = theme.accentColor
                    )
                ) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Clear filters",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear All",
                        fontSize = 12.sp
                    )
                }
            }
            
            // Show active filters as chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedTags.toList()) { tagId ->
                    // Find the tag object to get its name and color
                    val tag = availableTagsByCategory.values.flatten().find { it.id == tagId }
                    tag?.let {
                        FilterChip(
                            onClick = { onTagToggled(tagId) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = it.name,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove filter",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                }
                            },
                            selected = true,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = it.color,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
        
        // Category Selection Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Available categories
            items(availableTagsByCategory.keys.toList()) { category ->
                val hasAvailableTags = availableTagsForCurrentFilters[category]?.isNotEmpty() == true
                val categoryHasSelectedTags = availableTagsByCategory[category]?.any { selectedTags.contains(it.id) } == true
                
                FilterChip(
                    onClick = { onCategoryToggled(category) },
                    enabled = hasAvailableTags || categoryHasSelectedTags,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = category.displayName,
                                fontSize = 12.sp,
                                color = when {
                                    !hasAvailableTags && !categoryHasSelectedTags -> theme.secondaryTextColor.copy(alpha = 0.5f)
                                    expandedCategory == category -> theme.primaryTextColor
                                    else -> theme.primaryTextColor
                                }
                            )
                            
                            // Show count of selected tags in this category
                            val selectedInCategory = availableTagsByCategory[category]?.count { selectedTags.contains(it.id) } ?: 0
                            if (selectedInCategory > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            theme.accentColor,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedInCategory.toString(),
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Icon(
                                if (expandedCategory == category) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = when {
                                    !hasAvailableTags && !categoryHasSelectedTags -> theme.secondaryTextColor.copy(alpha = 0.5f)
                                    else -> theme.primaryTextColor
                                }
                            )
                        }
                    },
                    selected = expandedCategory == category,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = theme.accentColor.copy(alpha = 0.2f),
                        containerColor = theme.surfaceColor,
                        disabledContainerColor = theme.surfaceColor.copy(alpha = 0.5f)
                    )
                )
            }
        }
        
        // Expanded Tag Selection
        expandedCategory?.let { category ->
            availableTagsByCategory[category]?.let { allTagsInCategory ->
                val availableTagsInCategory = availableTagsForCurrentFilters[category] ?: emptyList()
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTagsInCategory) { tag ->
                        val isSelected = selectedTags.contains(tag.id)
                        val isAvailable = availableTagsInCategory.contains(tag) || isSelected
                        
                        FilterChip(
                            onClick = { onTagToggled(tag.id) },
                            enabled = isAvailable,
                            label = {
                                Text(
                                    text = tag.name,
                                    fontSize = 12.sp,
                                    color = when {
                                        isSelected -> Color.White
                                        !isAvailable -> theme.secondaryTextColor.copy(alpha = 0.5f)
                                        else -> tag.color
                                    }
                                )
                            },
                            selected = isSelected,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tag.color,
                                containerColor = tag.color.copy(alpha = 0.1f),
                                selectedLabelColor = Color.White,
                                labelColor = tag.color,
                                disabledContainerColor = theme.surfaceColor.copy(alpha = 0.3f),
                                disabledLabelColor = theme.secondaryTextColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
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