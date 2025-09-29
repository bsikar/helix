package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.Tag
import com.bsikar.helix.data.model.TagCategory
import com.bsikar.helix.data.model.PresetTags
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.BookCard
import com.bsikar.helix.ui.components.InfiniteHorizontalBookScroll
import com.bsikar.helix.ui.components.SearchBar
import com.bsikar.helix.ui.components.SearchUtils
import com.bsikar.helix.ui.components.ResponsiveConfig
import com.bsikar.helix.ui.components.ResponsiveBookGrid
import com.bsikar.helix.ui.components.ResponsiveBookCard
import com.bsikar.helix.ui.components.getWindowSizeClass
import com.bsikar.helix.ui.components.WindowSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    selectedTab: Int = 2,
    onTabSelected: (Int) -> Unit = {},
    theme: AppTheme,
    onNavigateToSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onSeeAllClick: (String, List<com.bsikar.helix.data.model.Book>) -> Unit = { _, _ -> },
    allBooks: List<com.bsikar.helix.data.model.Book> = emptyList(),
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (com.bsikar.helix.data.model.Book) -> Unit = { _ -> },
    onRemoveFromLibrary: (String) -> Unit = { _ -> },
    onRefresh: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedCategory by remember { mutableStateOf<TagCategory?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Create mutable internal list to allow book updates
    // Use stable key based on book count to avoid resetting on metadata changes
    val allBooksKey = remember(allBooks.size) { allBooks.map { it.id }.sorted().joinToString(",") }
    var internalAllBooks by remember(allBooksKey) { mutableStateOf(allBooks) }
    
    // Update internal list when parent list changes, but preserve local modifications
    LaunchedEffect(allBooks) {
        // Only update books that aren't being locally modified
        internalAllBooks = allBooks
        Log.d("BrowseScreen", "Updated book list: ${allBooks.size} total books")
        Log.d("BrowseScreen", "Audiobooks: ${allBooks.count { it.isAudiobook() }}")
        Log.d("BrowseScreen", "Regular books: ${allBooks.count { !it.isAudiobook() }}")
    }
    
    // Function to update a book in the internal list
    val updateBookInList = { updatedBook: com.bsikar.helix.data.model.Book ->
        internalAllBooks = internalAllBooks.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        
        // Also call the parent's update function if provided
        onUpdateBookSettings(updatedBook)
    }
    
    // Responsive configuration
    val responsiveConfig = ResponsiveConfig.fromScreenWidth()
    val windowSizeClass = getWindowSizeClass()
    
    // Function to render books responsively
    @Composable
    fun ResponsiveBrowseSection(
        books: List<com.bsikar.helix.data.model.Book>
    ) {
        when (windowSizeClass) {
            WindowSizeClass.COMPACT -> {
                // Use horizontal scroll for phones
                InfiniteHorizontalBookScroll(
                    books = books,
                    showProgress = false,
                    theme = theme,
                    searchQuery = searchQuery,
                    isBrowseMode = true,
                    onBookClick = onBookClick,
                    onStartReading = onStartReading,
                    onMarkCompleted = onMarkCompleted,
                    onMoveToOnDeck = onMoveToOnDeck,
                    onSetProgress = onSetProgress,
                    onEditTags = onEditTags,
                    onUpdateBookSettings = updateBookInList,
                    onRemoveFromLibrary = onRemoveFromLibrary
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
                            showProgress = false,
                            theme = theme,
                            searchQuery = searchQuery,
                            config = responsiveConfig,
                            isBrowseMode = true,
                            onBookClick = onBookClick,
                            onStartReading = onStartReading,
                            onMarkCompleted = onMarkCompleted,
                            onMoveToOnDeck = onMoveToOnDeck,
                            onSetProgress = onSetProgress,
                            onEditTags = onEditTags,
                            onUpdateBookSettings = updateBookInList,
                            onRemoveFromLibrary = onRemoveFromLibrary
                        )
                    }
                }
            }
        }
    }
    
    // Organize books by meaningful categories for personal library
    val recentlyAdded = remember(internalAllBooks) {
        // Recently imported books (all types including audiobooks)
        internalAllBooks.filter { it.isImported }
            .sortedByDescending { it.dateAdded }
            .take(8)
    }
    
    val currentlyReading = remember(internalAllBooks) {
        // Books currently being read (progress between 0 and 1, excluding completed and audiobooks)
        internalAllBooks.filter { !it.isAudiobook() && it.progress > 0f && it.progress < 1f }
            .sortedByDescending { it.lastReadTimestamp }
            .take(6)
    }
    
    val unreadBooks = remember(internalAllBooks) {
        // Books that haven't been started yet (exclude audiobooks)
        internalAllBooks.filter { !it.isAudiobook() && it.progress == 0f }
            .sortedBy { it.title }
            .take(10)
    }
    
    // Separate section for audiobooks
    val currentlyListening = remember(internalAllBooks) {
        // Audiobooks currently being listened to
        internalAllBooks.filter { it.isAudiobook() && it.progress > 0f && it.progress < 1f }
            .sortedByDescending { it.lastReadTimestamp }
            .take(8)
    }
    
    val audiobooksToListen = remember(internalAllBooks) {
        // Unplayed audiobooks
        internalAllBooks.filter { it.isAudiobook() && it.progress == 0f }
            .sortedByDescending { it.dateAdded }
            .take(8)
    }
    
    val byAuthor = remember(internalAllBooks) {
        // Group by author, showing authors with multiple books
        internalAllBooks.groupBy { it.author }
            .filter { (_, books) -> books.size > 1 }
            .toList()
            .sortedByDescending { (_, books) -> books.size }
            .take(5)
    }

    // Get only tags that actually exist in the media library
    val availableTagsByCategory = remember(internalAllBooks) {
        // Get all tags from books in the library
        val tagsInLibrary = internalAllBooks.flatMap { it.getTagObjects() }.distinctBy { it.id }
        
        // Group by category, only showing categories that have tags
        TagCategory.values().filter { it != TagCategory.CUSTOM }.associateWith { category ->
            tagsInLibrary.filter { it.category == category }
        }.filterValues { it.isNotEmpty() }
    }

    // Calculate which tags are available given current filter selection
    val availableTagsForCurrentFilters = remember(internalAllBooks, selectedTags) {
        if (selectedTags.isEmpty()) {
            availableTagsByCategory
        } else {
            // Find books that match current filters
            val filteredBooks = internalAllBooks.filter { book ->
                selectedTags.all { tag -> book.hasTag(tag) }
            }
            
            // Get tags available in filtered books
            val availableTagsInFilteredBooks = filteredBooks.flatMap { it.getTagObjects() }.distinctBy { it.id }
            
            TagCategory.values().filter { it != TagCategory.CUSTOM }.associateWith { category ->
                availableTagsInFilteredBooks.filter { it.category == category }
            }.filterValues { it.isNotEmpty() }
        }
    }

    // Filter books based on search query and selected tags
    val filteredRecentlyAdded = remember(searchQuery, selectedTags, recentlyAdded) {
        filterBooksByTags(recentlyAdded, searchQuery, selectedTags)
    }
    
    val filteredCurrentlyReading = remember(searchQuery, selectedTags, currentlyReading) {
        filterBooksByTags(currentlyReading, searchQuery, selectedTags)
    }
    
    val filteredUnreadBooks = remember(searchQuery, selectedTags, unreadBooks) {
        filterBooksByTags(unreadBooks, searchQuery, selectedTags)
    }
    
    val filteredCurrentlyListening = remember(searchQuery, selectedTags, currentlyListening) {
        filterBooksByTags(currentlyListening, searchQuery, selectedTags)
    }
    
    val filteredAudiobooksToListen = remember(searchQuery, selectedTags, audiobooksToListen) {
        filterBooksByTags(audiobooksToListen, searchQuery, selectedTags)
    }
    
    // Filter authors who have books matching the current filters
    val filteredByAuthor = remember(searchQuery, selectedTags, byAuthor) {
        byAuthor.mapNotNull { (author, books) ->
            val filteredBooks = filterBooksByTags(books, searchQuery, selectedTags)
            if (filteredBooks.isNotEmpty()) {
                author to filteredBooks
            } else null
        }
    }
    
    // All books (for when no specific sections apply or for general browsing)
    val allFilteredBooks = remember(searchQuery, selectedTags, internalAllBooks) {
        filterBooksByTags(internalAllBooks, searchQuery, selectedTags)
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                onRefresh()
                isRefreshing = false
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
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

            // Currently Listening Section (Audiobooks)
            if (filteredCurrentlyListening.isNotEmpty()) {
                item {
                    BrowseSectionHeader(
                        title = "Currently Listening", 
                        theme = theme,
                        icon = Icons.Filled.Headphones,
                        onSeeAllClick = { onSeeAllClick("Currently Listening", filteredCurrentlyListening) }
                    )
                }
                item {
                    ResponsiveBrowseSection(
                        books = filteredCurrentlyListening
                    )
                }
            }
            
            // Currently Reading Section (Regular books)
            if (filteredCurrentlyReading.isNotEmpty()) {
                item {
                    BrowseSectionHeader(
                        title = "Continue Reading", 
                        theme = theme,
                        onSeeAllClick = { onSeeAllClick("Continue Reading", filteredCurrentlyReading) }
                    )
                }
                item {
                    ResponsiveBrowseSection(
                        books = filteredCurrentlyReading
                    )
                }
            }

            // Recently Added Section
            if (filteredRecentlyAdded.isNotEmpty()) {
                item {
                    BrowseSectionHeader(
                        title = "Recently Added", 
                        theme = theme,
                        onSeeAllClick = { onSeeAllClick("Recently Added", filteredRecentlyAdded) }
                    )
                }
                item {
                    ResponsiveBrowseSection(
                        books = filteredRecentlyAdded
                    )
                }
            }

            // Audiobooks to Listen Section
            if (filteredAudiobooksToListen.isNotEmpty()) {
                item {
                    BrowseSectionHeader(
                        title = "Audiobooks", 
                        theme = theme,
                        onSeeAllClick = { onSeeAllClick("Audiobooks", filteredAudiobooksToListen) }
                    )
                }
                item {
                    ResponsiveBrowseSection(
                        books = filteredAudiobooksToListen
                    )
                }
            }
            
            // Unread Books Section
            if (filteredUnreadBooks.isNotEmpty()) {
                item {
                    BrowseSectionHeader(
                        title = "Unread Books", 
                        theme = theme,
                        onSeeAllClick = { onSeeAllClick("Unread Books", filteredUnreadBooks) }
                    )
                }
                item {
                    ResponsiveBrowseSection(
                        books = filteredUnreadBooks
                    )
                }
            }

            // By Author Sections
            if (filteredByAuthor.isNotEmpty()) {
                items(filteredByAuthor.take(3)) { (author, books) ->
                    BrowseSectionHeader(
                        title = "More by $author", 
                        theme = theme,
                        onSeeAllClick = { onSeeAllClick("Books by $author", books) }
                    )
                    ResponsiveBrowseSection(
                        books = books.take(6)
                    )
                }
            }

            // All Books Section (fallback when specific sections are empty or for comprehensive browsing)
            if (searchQuery.isNotEmpty() || selectedTags.isNotEmpty()) {
                if (allFilteredBooks.isNotEmpty()) {
                    item {
                        BrowseSectionHeader(
                            title = "Search Results (${allFilteredBooks.size})", 
                            theme = theme,
                            onSeeAllClick = { onSeeAllClick("All Results", allFilteredBooks) }
                        )
                    }
                    item {
                        ResponsiveBrowseSection(
                            books = allFilteredBooks.take(12)
                        )
                    }
                }
            } else if (filteredCurrentlyReading.isEmpty() && filteredRecentlyAdded.isEmpty() && 
                       filteredUnreadBooks.isEmpty() && filteredByAuthor.isEmpty()) {
                // Show all books when no specific sections have content
                if (allFilteredBooks.isNotEmpty()) {
                    item {
                        BrowseSectionHeader(
                            title = "All Books (${allFilteredBooks.size})", 
                            theme = theme,
                            onSeeAllClick = { onSeeAllClick("All Books", allFilteredBooks) }
                        )
                    }
                    item {
                        ResponsiveBrowseSection(
                            books = allFilteredBooks.take(12)
                        )
                    }
                }
            }

            // No results message
            if (allFilteredBooks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = theme.secondaryTextColor.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedTags.isNotEmpty()) {
                                "No books match your search criteria"
                            } else {
                                "No books in your library yet"
                            },
                            textAlign = TextAlign.Center,
                            color = theme.primaryTextColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (searchQuery.isEmpty() && selectedTags.isEmpty()) {
                            Text(
                                text = "Import some EPUB files to get started!",
                                textAlign = TextAlign.Center,
                                color = theme.secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            }
        }
    }
}


@Composable
fun BrowseSectionHeader(
    title: String, 
    theme: AppTheme,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onSeeAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = theme.accentColor
                )
            }
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryTextColor
            )
        }
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
    books: List<com.bsikar.helix.data.model.Book>,
    theme: AppTheme,
    searchQuery: String = "",
    onBookClick: (Book) -> Unit,
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (com.bsikar.helix.data.model.Book) -> Unit = { _ -> },
    onRemoveFromLibrary: (String) -> Unit = { _ -> }
) {
    // Use the new circular implementation from BookSection
    InfiniteHorizontalBookScroll(
        books = books,
        showProgress = false,
        theme = theme,
        searchQuery = searchQuery,
        contentPadding = PaddingValues(horizontal = 16.dp),
        isBrowseMode = true,
        onBookClick = onBookClick,
        onStartReading = onStartReading,
        onMarkCompleted = onMarkCompleted,
        onMoveToOnDeck = onMoveToOnDeck,
        onSetProgress = onSetProgress,
        onEditTags = onEditTags,
        onUpdateBookSettings = onUpdateBookSettings,
        onRemoveFromLibrary = onRemoveFromLibrary
    )
}

// Helper function to filter books by multiple tags
private fun filterBooksByTags(books: List<com.bsikar.helix.data.model.Book>, searchQuery: String, selectedTags: Set<String>): List<com.bsikar.helix.data.model.Book> {
    // First filter by tags
    val tagFiltered = if (selectedTags.isEmpty()) {
        books
    } else {
        books.filter { book ->
            selectedTags.all { tag -> book.hasTag(tag) }
        }
    }
    
    // Then apply fuzzy search if query exists
    return if (searchQuery.isBlank()) {
        tagFiltered
    } else {
        val searchResults = SearchUtils.fuzzySearch(
            items = tagFiltered,
            query = searchQuery,
            getText = { it.title },
            getSecondaryText = { it.author },
            threshold = 0.3
        )
        searchResults.map { it.item }
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