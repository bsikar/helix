package com.bsikar.helix.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.bsikar.helix.R
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.Tag
import com.bsikar.helix.data.model.UiState
import com.bsikar.helix.data.model.TagCategory
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.InfiniteHorizontalBookScroll
import com.bsikar.helix.ui.components.SearchBar
import com.bsikar.helix.ui.components.ResponsiveConfig
import com.bsikar.helix.ui.components.ResponsiveBookGrid
import com.bsikar.helix.ui.components.ResponsiveBookCard
import com.bsikar.helix.ui.components.getWindowSizeClass
import com.bsikar.helix.ui.components.WindowSizeClass
import com.bsikar.helix.ui.components.ImportProgressIndicator
import com.bsikar.helix.ui.components.ResponsiveSpacing
import com.bsikar.helix.managers.ImportManager
import com.bsikar.helix.viewmodels.LibraryContentFilter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    readingBooks: List<com.bsikar.helix.data.model.Book>,
    onDeckBooks: List<com.bsikar.helix.data.model.Book>,
    completedBooks: List<com.bsikar.helix.data.model.Book>,
    allBooks: List<com.bsikar.helix.data.model.Book>,
    currentlyPlayingAudiobook: Book? = null,
    contentFilter: LibraryContentFilter,
    onContentFilterChange: (LibraryContentFilter) -> Unit,
    activeSecondaryFilters: Set<String>,
    onToggleSecondaryFilter: (String) -> Unit,
    onClearSecondaryFilters: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProgressSettings: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onSeeAllClick: (String, List<com.bsikar.helix.data.model.Book>) -> Unit = { _, _ -> },
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (com.bsikar.helix.data.model.Book) -> Unit = { _ -> },
    onRemoveFromLibrary: (String) -> Unit = { _ -> },
    libraryManager: LibraryManager? = null,
    libraryState: UiState<List<com.bsikar.helix.data.model.Book>> = UiState.Success(emptyList()),
    errorMessage: String? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    importManager: ImportManager? = null,
    // Sorting states and functions
    readingSortAscending: Boolean = false,
    onDeckSortAscending: Boolean = true,
    completedSortAscending: Boolean = true,
    onToggleReadingSort: () -> Unit = {},
    onToggleOnDeckSort: () -> Unit = {},
    onToggleCompletedSort: () -> Unit = {},
    onRefresh: () -> Unit = {},
    extraBottomPadding: Dp = 0.dp
) {
    // Search query now comes from ViewModel instead of local state
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Import progress state
    val importProgress by importManager?.importProgress?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    // Create mutable internal lists to allow book updates
    // Use keys that include order to ensure recomposition when sorting changes
    val readingBooksKey = remember(readingBooks.size, readingSortAscending) { readingBooks.map { it.id }.joinToString(",") }
    val onDeckBooksKey = remember(onDeckBooks.size, onDeckSortAscending) { onDeckBooks.map { it.id }.joinToString(",") }
    val completedBooksKey = remember(completedBooks.size, completedSortAscending) { completedBooks.map { it.id }.joinToString(",") }
    
    var internalReadingBooks by remember(readingBooksKey) { mutableStateOf(readingBooks) }
    var internalOnDeckBooks by remember(onDeckBooksKey) { mutableStateOf(onDeckBooks) }
    var internalCompletedBooks by remember(completedBooksKey) { mutableStateOf(completedBooks) }
    
    // Sync internal lists when source lists change (including sorting changes)
    LaunchedEffect(readingBooksKey) { internalReadingBooks = readingBooks }
    LaunchedEffect(onDeckBooksKey) { internalOnDeckBooks = onDeckBooks }
    LaunchedEffect(completedBooksKey) { internalCompletedBooks = completedBooks }

    // Function to update a book in the appropriate list
    val updateBookInLists = { updatedBook: com.bsikar.helix.data.model.Book ->
        // Update in reading books
        internalReadingBooks = internalReadingBooks.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        
        // Update in plan to read books
        internalOnDeckBooks = internalOnDeckBooks.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        
        // Update in completed books
        internalCompletedBooks = internalCompletedBooks.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        
        // Also call the parent's update function if provided
        onUpdateBookSettings(updatedBook)
    }
    
    // Import handlers
    val handleCancelImport: (String) -> Unit = { importId ->
        scope.launch {
            importManager?.cancelImport(importId)
        }
    }
    
    val handleRetryImport: (String) -> Unit = { importId ->
        scope.launch {
            importManager?.retryImport(importId)
        }
    }
    
    val handleDismissImport: (String) -> Unit = { importId ->
        // Import will be automatically removed from active list when completed
    }

    // Pull-to-refresh functionality
    
    // Responsive configuration
    val responsiveConfig = ResponsiveConfig.fromScreenWidth()
    val windowSizeClass = getWindowSizeClass()

    val availableTextFilters = remember(allBooks) {
        allBooks
            .filter { !it.isAudiobook() }
            .flatMap { it.getTagsByCategory(TagCategory.FORMAT) }
            .distinctBy { it.id }
            .sortedBy { it.name }
    }

    val availableAudioFilters = remember(allBooks) {
        allBooks
            .filter { it.isAudiobook() }
            .flatMap { book ->
                book.getTagObjects().filter { it.category == TagCategory.GENRE }
            }
            .distinctBy { it.id }
            .sortedBy { it.name }
    }

    val secondaryFilterGroups = remember(contentFilter, availableTextFilters, availableAudioFilters) {
        when (contentFilter) {
            LibraryContentFilter.ALL -> mapOf(
                "Formats" to availableTextFilters,
                "Genres" to availableAudioFilters
            )
            LibraryContentFilter.TEXT_ONLY -> mapOf(
                "Formats" to availableTextFilters
            )
            LibraryContentFilter.AUDIO_ONLY -> mapOf(
                "Genres" to availableAudioFilters
            )
        }
    }
    
    // Function to render books responsively
    @Composable
    fun ResponsiveBookSection(
        books: List<com.bsikar.helix.data.model.Book>,
        showProgress: Boolean
    ) {
        when (windowSizeClass) {
            WindowSizeClass.COMPACT -> {
                // Use horizontal scroll for phones
                InfiniteHorizontalBookScroll(
                    books = books,
                    showProgress = showProgress,
                    theme = theme,
                    searchQuery = searchQuery,
                    contentPadding = ResponsiveSpacing.contentPadding(),
                    onBookClick = onBookClick,
                    onStartReading = onStartReading,
                    onMarkCompleted = onMarkCompleted,
                    onMoveToOnDeck = onMoveToOnDeck,
                    onSetProgress = onSetProgress,
                    onEditTags = onEditTags,
                    onUpdateBookSettings = updateBookInLists,
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
                            showProgress = showProgress,
                            theme = theme,
                            searchQuery = searchQuery,
                            config = responsiveConfig,
                            isBrowseMode = false,
                            onBookClick = onBookClick,
                            onStartReading = onStartReading,
                            onMarkCompleted = onMarkCompleted,
                            onMoveToOnDeck = onMoveToOnDeck,
                            onSetProgress = onSetProgress,
                            onEditTags = onEditTags,
                            onUpdateBookSettings = updateBookInLists,
                            onRemoveFromLibrary = onRemoveFromLibrary
                        )
                    }
                }
            }
        }
    }
    val handleRefresh = {
        isRefreshing = true
        onRefresh() // Call the provided refresh function
        scope.launch {
            kotlinx.coroutines.delay(500) // Brief delay for visual feedback
            isRefreshing = false
        }
    }

    // Sorting is now handled in ViewModel - filtered results come pre-sorted


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
                        stringResource(R.string.library),
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
        }
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // Handle UiState for library operations
        when (libraryState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = theme.accentColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Loading library...",
                            color = theme.secondaryTextColor,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Library Error",
                            color = theme.primaryTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = libraryState.exception.message ?: "An unknown error occurred",
                            color = theme.secondaryTextColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { handleRefresh() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.accentColor
                            )
                        ) {
                            Text(
                                text = "Retry",
                                color = theme.surfaceColor
                            )
                        }
                    }
                }
            }
            is UiState.Success -> {
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = isRefreshing,
                    onRefresh = { handleRefresh() },
                    state = pullToRefreshState
                ) {
                    val combinedPadding = PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        top = innerPadding.calculateTopPadding(),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        bottom = innerPadding.calculateBottomPadding() + extraBottomPadding
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = combinedPadding,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

            item(key = "search_bar") {
                SearchBar(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    theme = theme
                )
            }

            item(key = "content_filters") {
                ContentFilterRow(
                    theme = theme,
                    currentFilter = contentFilter,
                    onFilterSelected = onContentFilterChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ResponsiveSpacing.medium())
                )
            }

            if (secondaryFilterGroups.values.any { it.isNotEmpty() }) {
                item(key = "secondary_filters") {
                    SecondaryFilterSection(
                        theme = theme,
                        filterGroups = secondaryFilterGroups,
                        activeFilters = activeSecondaryFilters,
                        onToggleFilter = onToggleSecondaryFilter,
                        onClearFilters = onClearSecondaryFilters,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ResponsiveSpacing.medium())
                    )
                }
            }

            // Show import progress using the new ImportManager
            if (importProgress.isNotEmpty()) {
                item(key = "import_progress") {
                    ImportProgressIndicator(
                        importProgress = importProgress,
                        onDismiss = handleDismissImport,
                        onCancel = handleCancelImport,
                        onRetry = handleRetryImport,
                        modifier = Modifier.padding(horizontal = ResponsiveSpacing.medium())
                    )
                }
            }
            

            val inProgressTitleRes = when (contentFilter) {
                LibraryContentFilter.TEXT_ONLY -> R.string.section_in_progress_text
                LibraryContentFilter.AUDIO_ONLY -> R.string.section_in_progress_audio
                LibraryContentFilter.ALL -> R.string.section_in_progress_all
            }
            val inProgressSubtitleRes = when (contentFilter) {
                LibraryContentFilter.TEXT_ONLY -> R.string.section_in_progress_subtitle_text
                LibraryContentFilter.AUDIO_ONLY -> R.string.section_in_progress_subtitle_audio
                LibraryContentFilter.ALL -> R.string.section_in_progress_subtitle_all
            }

            val inProgressBooks = if (currentlyPlayingAudiobook != null && contentFilter != LibraryContentFilter.TEXT_ONLY) {
                internalReadingBooks.filterNot { it.id == currentlyPlayingAudiobook.id }
            } else {
                internalReadingBooks
            }

            if (contentFilter != LibraryContentFilter.TEXT_ONLY) {
                currentlyPlayingAudiobook?.let { book ->
                    item(key = "currently_playing") {
                        CurrentlyPlayingCard(
                            book = book,
                            theme = theme,
                            onBookClick = onBookClick,
                            modifier = Modifier.padding(horizontal = ResponsiveSpacing.medium(), vertical = ResponsiveSpacing.small())
                        )
                    }
                }
            }

            if (inProgressBooks.isNotEmpty()) {
                val sectionKey = "in_progress_books_${inProgressBooks.size}"
                item(key = "in_progress_header") {
                    val inProgressTitleString = stringResource(id = inProgressTitleRes)
                    LibrarySectionHeader(
                        title = inProgressTitleString,
                        subtitle = stringResource(id = inProgressSubtitleRes),
                        theme = theme,
                        isAscending = readingSortAscending,
                        onSortClick = onToggleReadingSort,
                        onSeeAllClick = { onSeeAllClick(inProgressTitleString, inProgressBooks) }
                    )
                }
                item(key = sectionKey) {
                    ResponsiveBookSection(
                        books = inProgressBooks,
                        showProgress = true
                    )
                }
            }

            if (internalOnDeckBooks.isNotEmpty()) {
                val onDeckTitleRes = when (contentFilter) {
                    LibraryContentFilter.AUDIO_ONLY -> R.string.section_on_deck_audio
                    else -> R.string.section_on_deck_all
                }
                item(key = "plan_to_read_header") {
                    val onDeckTitleString = stringResource(id = onDeckTitleRes)
                    LibrarySectionHeader(
                        title = onDeckTitleString,
                        subtitle = "Title",
                        theme = theme,
                        isAscending = onDeckSortAscending,
                        onSortClick = onToggleOnDeckSort,
                        onSeeAllClick = { onSeeAllClick(onDeckTitleString, internalOnDeckBooks) }
                    )
                }
                item(key = "on_deck_books_${internalOnDeckBooks.size}") {
                    ResponsiveBookSection(
                        books = internalOnDeckBooks,
                        showProgress = false
                    )
                }
            }

            if (internalCompletedBooks.isNotEmpty()) {
                val completedTitleRes = when (contentFilter) {
                    LibraryContentFilter.AUDIO_ONLY -> R.string.section_completed_audio
                    else -> R.string.section_completed_all
                }
                item(key = "read_header") {
                    val completedTitleString = stringResource(id = completedTitleRes)
                    LibrarySectionHeader(
                        title = completedTitleString,
                        subtitle = "Title",
                        theme = theme,
                        isAscending = completedSortAscending,
                        onSortClick = onToggleCompletedSort,
                        onSeeAllClick = { onSeeAllClick(completedTitleString, internalCompletedBooks) }
                    )
                }
                item(key = "read_books_${internalCompletedBooks.size}") {
                    ResponsiveBookSection(
                        books = internalCompletedBooks,
                        showProgress = false
                    )
                }
            }

            // Show overall empty state only if no books exist at all
            if (allBooks.isEmpty()) {
                item(key = "no_books_found") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = "Empty library",
                            modifier = Modifier.size(64.dp),
                            tint = theme.secondaryTextColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your library is empty",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.primaryTextColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import your books and audiobooks to get started.\nSupported formats: EPUB, M4B",
                            fontSize = 14.sp,
                            color = theme.secondaryTextColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
                    }
                }
            }
        }
        
        // Show error message overlay if present
        errorMessage?.let { message ->
            LaunchedEffect(message) {
                // Here you could show a Snackbar if you have a SnackbarHost
                // For now, the error will be handled by the UiState.Error case above
            }
        }
    }
}

@Composable
private fun ContentFilterRow(
    theme: AppTheme,
    currentFilter: LibraryContentFilter,
    onFilterSelected: (LibraryContentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        Triple(LibraryContentFilter.ALL, Icons.AutoMirrored.Filled.LibraryBooks, stringResource(R.string.filter_all_content)),
        Triple(LibraryContentFilter.TEXT_ONLY, Icons.Filled.MenuBook, stringResource(R.string.filter_text_only)),
        Triple(LibraryContentFilter.AUDIO_ONLY, Icons.Filled.Headphones, stringResource(R.string.filter_audio_only))
    )

    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ResponsiveSpacing.small()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { (filter, icon, label) ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = label,
                            color = if (currentFilter == filter) theme.accentColor else theme.primaryTextColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (currentFilter == filter) theme.accentColor else theme.secondaryTextColor
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = theme.accentColor.copy(alpha = 0.15f),
                        selectedLabelColor = theme.accentColor,
                        containerColor = theme.surfaceColor,
                        labelColor = theme.primaryTextColor
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecondaryFilterSection(
    theme: AppTheme,
    filterGroups: Map<String, List<Tag>>,
    activeFilters: Set<String>,
    onToggleFilter: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = ResponsiveSpacing.small())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.filter_refine_results),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryTextColor
            )
            if (activeFilters.isNotEmpty()) {
                TextButton(onClick = onClearFilters) {
                    Text(
                        text = stringResource(R.string.filter_clear),
                        color = theme.accentColor,
                        fontSize = 13.sp
                    )
                }
            }
        }

        filterGroups.forEach { (title, tags) ->
            if (tags.isNotEmpty()) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.secondaryTextColor,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        val isSelected = activeFilters.contains(tag.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleFilter(tag.id) },
                            label = {
                                Text(
                                    text = tag.name,
                                    color = if (isSelected) theme.accentColor else theme.primaryTextColor
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.accentColor.copy(alpha = 0.15f),
                                selectedLabelColor = theme.accentColor,
                                containerColor = theme.surfaceColor,
                                labelColor = theme.primaryTextColor
                            )
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
            .padding(start = ResponsiveSpacing.medium(), end = ResponsiveSpacing.small()),
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

@Composable
fun CurrentlyPlayingCard(
    book: Book,
    theme: AppTheme,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onBookClick(book) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.accentColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover art
            Card(
                modifier = Modifier
                    .size(60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                        Color.Transparent
                    } else {
                        book.getEffectiveCoverColor()
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = book.coverImagePath,
                            contentDescription = "Cover",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Book info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Now Playing",
                    fontSize = 12.sp,
                    color = theme.accentColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    fontSize = 14.sp,
                    color = theme.secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Play indicator
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Playing",
                tint = theme.accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}