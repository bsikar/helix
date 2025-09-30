package com.bsikar.helix.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bsikar.helix.data.ImportProgress
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.model.Tag
import com.bsikar.helix.data.model.TagCategory
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.ui.components.CompactImportProgress
import com.bsikar.helix.ui.components.ResponsiveSpacing
import com.bsikar.helix.ui.components.SearchBar
import com.bsikar.helix.ui.components.SearchUtils
import com.bsikar.helix.viewmodels.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedLibraryScreen(
    theme: AppTheme,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    contentFilter: LibraryViewModel.LibraryContentFilter,
    onContentFilterChange: (LibraryViewModel.LibraryContentFilter) -> Unit,
    activeTagFilters: Set<String>,
    onToggleTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    books: List<Book>,
    allBooks: List<Book>,
    nowPlayingBookId: String?,
    onBookClick: (Book) -> Unit,
    onStartReading: (String) -> Unit,
    onMarkCompleted: (String) -> Unit,
    onMoveToOnDeck: (String) -> Unit,
    onRemoveFromOnDeck: (String) -> Unit,
    onRefresh: () -> Unit,
    importProgress: List<ImportProgress>,
    isRefreshing: Boolean,
    scanMessage: String,
    onOpenSettings: () -> Unit,
    onOpenProgressSettings: () -> Unit,
    showNowPlayingBarPadding: Boolean,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val relevantBooks = remember(allBooks, contentFilter) {
        when (contentFilter) {
            LibraryViewModel.LibraryContentFilter.ALL -> allBooks
            LibraryViewModel.LibraryContentFilter.TEXT_ONLY -> allBooks.filter { !it.isAudiobook() }
            LibraryViewModel.LibraryContentFilter.AUDIO_ONLY -> allBooks.filter { it.isAudiobook() }
        }
    }

    val availableTagFilters = remember(relevantBooks) {
        val usedTags = relevantBooks
            .flatMap { it.getTagObjects() }
            .distinctBy { it.id }

        val allowedCategories = when (contentFilter) {
            LibraryViewModel.LibraryContentFilter.ALL -> setOf(
                TagCategory.FORMAT,
                TagCategory.GENRE,
                TagCategory.DEMOGRAPHIC,
                TagCategory.THEME,
                TagCategory.STATUS
            )
            LibraryViewModel.LibraryContentFilter.TEXT_ONLY -> setOf(
                TagCategory.FORMAT,
                TagCategory.GENRE,
                TagCategory.DEMOGRAPHIC,
                TagCategory.THEME,
                TagCategory.STATUS
            )
            LibraryViewModel.LibraryContentFilter.AUDIO_ONLY -> setOf(
                TagCategory.GENRE,
                TagCategory.THEME,
                TagCategory.STATUS
            )
        }

        usedTags
            .filter { it.category in allowedCategories }
            .groupBy { it.category }
            .mapValues { (_, value) -> value.sortedBy { it.displayName.lowercase() } }
    }

    val inProgressBooks = remember(books) {
        books.filter { it.readingStatus == ReadingStatus.READING || it.readingStatus == ReadingStatus.LISTENING }
            .sortedByDescending { it.lastReadTimestamp }
    }

    val onDeckBooks = remember(books) {
        books.filter { it.readingStatus == ReadingStatus.PLAN_TO_READ || it.readingStatus == ReadingStatus.PLAN_TO_LISTEN }
            .sortedBy { it.title }
    }

    val completedBooks = remember(books) {
        books.filter { it.readingStatus == ReadingStatus.COMPLETED }
            .sortedByDescending { it.lastReadTimestamp }
    }

    val backlogBooks = remember(books) {
        books.filter { it.readingStatus == ReadingStatus.UNREAD }
            .sortedBy { it.title }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = theme.backgroundColor,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = "Library",
                        color = theme.primaryTextColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.surfaceColor,
                    titleContentColor = theme.primaryTextColor
                ),
                actions = {
                    IconButton(onClick = onOpenProgressSettings) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Progress Settings",
                            tint = theme.primaryTextColor
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = theme.primaryTextColor
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize(),
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ResponsiveSpacing.medium(),
                    end = ResponsiveSpacing.medium(),
                    top = innerPadding.calculateTopPadding() + ResponsiveSpacing.medium(),
                    bottom = if (showNowPlayingBarPadding) 128.dp else ResponsiveSpacing.large()
                ),
                verticalArrangement = Arrangement.spacedBy(ResponsiveSpacing.medium())
            ) {
                item {
                    SearchBar(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        theme = theme
                    )
                }

                item {
                    ContentFilterRow(
                        theme = theme,
                        currentFilter = contentFilter,
                        onFilterSelected = onContentFilterChange
                    )
                }

                if (availableTagFilters.isNotEmpty()) {
                    item {
                        SecondaryFilterSection(
                            theme = theme,
                            categories = availableTagFilters,
                            activeTagFilters = activeTagFilters,
                            onToggleTag = onToggleTagFilter,
                            onClearFilters = onClearTagFilters
                        )
                    }
                }

                if (scanMessage.isNotBlank()) {
                    item {
                        InfoBanner(
                            message = scanMessage,
                            theme = theme
                        )
                    }
                }

                if (importProgress.isNotEmpty()) {
                    item {
                        CompactImportProgress(
                            activeImports = importProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (books.isEmpty()) {
                    item {
                        EmptyState(theme = theme)
                    }
                } else {
                    if (inProgressBooks.isNotEmpty()) {
                        item { LibrarySectionHeader("In Progress", theme) }
                        items(inProgressBooks, key = { it.id }) { book ->
                            LibraryListItem(
                                book = book,
                                theme = theme,
                                searchQuery = searchQuery,
                                nowPlayingBookId = nowPlayingBookId,
                                onBookClick = onBookClick,
                                onStartReading = onStartReading,
                                onMarkCompleted = onMarkCompleted,
                                onMoveToOnDeck = onMoveToOnDeck,
                                onRemoveFromOnDeck = onRemoveFromOnDeck
                            )
                        }
                    }

                    if (onDeckBooks.isNotEmpty()) {
                        item { LibrarySectionHeader("On Deck", theme) }
                        items(onDeckBooks, key = { it.id }) { book ->
                            LibraryListItem(
                                book = book,
                                theme = theme,
                                searchQuery = searchQuery,
                                nowPlayingBookId = nowPlayingBookId,
                                onBookClick = onBookClick,
                                onStartReading = onStartReading,
                                onMarkCompleted = onMarkCompleted,
                                onMoveToOnDeck = onMoveToOnDeck,
                                onRemoveFromOnDeck = onRemoveFromOnDeck
                            )
                        }
                    }

                    if (backlogBooks.isNotEmpty()) {
                        item { LibrarySectionHeader("Up Next", theme) }
                        items(backlogBooks, key = { it.id }) { book ->
                            LibraryListItem(
                                book = book,
                                theme = theme,
                                searchQuery = searchQuery,
                                nowPlayingBookId = nowPlayingBookId,
                                onBookClick = onBookClick,
                                onStartReading = onStartReading,
                                onMarkCompleted = onMarkCompleted,
                                onMoveToOnDeck = onMoveToOnDeck,
                                onRemoveFromOnDeck = onRemoveFromOnDeck
                            )
                        }
                    }

                    if (completedBooks.isNotEmpty()) {
                        item { LibrarySectionHeader("Completed", theme) }
                        items(completedBooks, key = { it.id }) { book ->
                            LibraryListItem(
                                book = book,
                                theme = theme,
                                searchQuery = searchQuery,
                                nowPlayingBookId = nowPlayingBookId,
                                onBookClick = onBookClick,
                                onStartReading = onStartReading,
                                onMarkCompleted = onMarkCompleted,
                                onMoveToOnDeck = onMoveToOnDeck,
                                onRemoveFromOnDeck = onRemoveFromOnDeck
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentFilterRow(
    theme: AppTheme,
    currentFilter: LibraryViewModel.LibraryContentFilter,
    onFilterSelected: (LibraryViewModel.LibraryContentFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ResponsiveSpacing.small()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == LibraryViewModel.LibraryContentFilter.ALL,
            onClick = { onFilterSelected(LibraryViewModel.LibraryContentFilter.ALL) },
            label = { Text("All") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.AllInclusive,
                    contentDescription = null
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = theme.surfaceColor,
                selectedContainerColor = theme.accentColor.copy(alpha = theme.alphaSubtle),
                labelColor = theme.primaryTextColor,
                selectedLabelColor = theme.accentColor
            )
        )

        FilterChip(
            selected = currentFilter == LibraryViewModel.LibraryContentFilter.TEXT_ONLY,
            onClick = { onFilterSelected(LibraryViewModel.LibraryContentFilter.TEXT_ONLY) },
            label = { Text("Text") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = null
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = theme.surfaceColor,
                selectedContainerColor = theme.accentColor.copy(alpha = theme.alphaSubtle),
                labelColor = theme.primaryTextColor,
                selectedLabelColor = theme.accentColor
            )
        )

        FilterChip(
            selected = currentFilter == LibraryViewModel.LibraryContentFilter.AUDIO_ONLY,
            onClick = { onFilterSelected(LibraryViewModel.LibraryContentFilter.AUDIO_ONLY) },
            label = { Text("Audio") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = theme.surfaceColor,
                selectedContainerColor = theme.accentColor.copy(alpha = theme.alphaSubtle),
                labelColor = theme.primaryTextColor,
                selectedLabelColor = theme.accentColor
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecondaryFilterSection(
    theme: AppTheme,
    categories: Map<TagCategory, List<Tag>>,
    activeTagFilters: Set<String>,
    onToggleTag: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.surfaceColor.copy(alpha = 0.7f))
            .padding(ResponsiveSpacing.medium())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Refine",
                style = MaterialTheme.typography.titleSmall,
                color = theme.primaryTextColor,
                fontWeight = FontWeight.Medium
            )

            if (activeTagFilters.isNotEmpty()) {
                AssistChip(
                    onClick = onClearFilters,
                    label = { Text("Clear") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = theme.surfaceColor,
                        labelColor = theme.secondaryTextColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        categories.forEach { (category, tags) ->
            if (tags.isNotEmpty()) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.secondaryTextColor,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        val selected = activeTagFilters.contains(tag.id)
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleTag(tag.id) },
                            label = { Text(tag.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = theme.surfaceColor,
                                selectedContainerColor = theme.accentColor.copy(alpha = theme.alphaSubtle),
                                labelColor = if (selected) theme.accentColor else theme.primaryTextColor,
                                selectedLabelColor = theme.accentColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LibrarySectionHeader(title: String, theme: AppTheme) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = theme.primaryTextColor,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = ResponsiveSpacing.small())
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryListItem(
    book: Book,
    theme: AppTheme,
    searchQuery: String,
    nowPlayingBookId: String?,
    onBookClick: (Book) -> Unit,
    onStartReading: (String) -> Unit,
    onMarkCompleted: (String) -> Unit,
    onMoveToOnDeck: (String) -> Unit,
    onRemoveFromOnDeck: (String) -> Unit
) {
    val progress = if (book.isAudiobook()) book.getAudioProgress() else book.progress
    val statusLabel = when (book.readingStatus) {
        ReadingStatus.READING -> if (book.isAudiobook()) "Listening" else "Reading"
        ReadingStatus.LISTENING -> "Listening"
        ReadingStatus.PLAN_TO_READ -> "On Deck"
        ReadingStatus.PLAN_TO_LISTEN -> "On Deck"
        ReadingStatus.COMPLETED -> "Completed"
        ReadingStatus.UNREAD -> "Not Started"
    }

    val statusColor = when (book.readingStatus) {
        ReadingStatus.COMPLETED -> theme.successColor
        ReadingStatus.READING, ReadingStatus.LISTENING -> theme.accentColor
        ReadingStatus.PLAN_TO_READ, ReadingStatus.PLAN_TO_LISTEN -> theme.warningColor
        ReadingStatus.UNREAD -> theme.secondaryTextColor
    }

    val quickActions = remember(book) {
        buildList {
            if (book.readingStatus != ReadingStatus.COMPLETED) {
                add(
                    QuickAction(
                        label = if (book.readingStatus == ReadingStatus.READING || book.readingStatus == ReadingStatus.LISTENING) {
                            "Resume"
                        } else if (book.isAudiobook()) {
                            "Start Listening"
                        } else {
                            "Start Reading"
                        },
                        icon = Icons.Filled.PlayArrow,
                        onClick = { onStartReading(book.id) }
                    )
                )
            } else {
                add(
                    QuickAction(
                        label = "Restart",
                        icon = Icons.Filled.Replay,
                        onClick = { onStartReading(book.id) }
                    )
                )
            }

            when (book.readingStatus) {
                ReadingStatus.PLAN_TO_READ, ReadingStatus.PLAN_TO_LISTEN -> {
                    add(
                        QuickAction(
                            label = "Remove On Deck",
                            icon = Icons.Filled.Close,
                            onClick = { onRemoveFromOnDeck(book.id) }
                        )
                    )
                }
                ReadingStatus.UNREAD, ReadingStatus.COMPLETED -> {
                    add(
                        QuickAction(
                            label = "Add to On Deck",
                            icon = Icons.Filled.Schedule,
                            onClick = { onMoveToOnDeck(book.id) }
                        )
                    )
                }
                else -> Unit
            }

            if (book.readingStatus != ReadingStatus.COMPLETED) {
                add(
                    QuickAction(
                        label = "Mark Complete",
                        icon = Icons.Filled.CheckCircle,
                        onClick = { onMarkCompleted(book.id) }
                    )
                )
            }
        }
    }

    Card(
        onClick = { onBookClick(book) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.surfaceColor
        ),
        border = if (nowPlayingBookId != null && nowPlayingBookId == book.id) {
            BorderStroke(1.5.dp, theme.accentColor)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(book.getEffectiveCoverColor()),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = book.coverImagePath,
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = theme.primaryTextColor.copy(alpha = theme.alphaMedium)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = SearchUtils.createHighlightedText(
                            text = book.title,
                            query = searchQuery,
                            baseColor = theme.primaryTextColor,
                            highlightColor = theme.accentColor
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (book.author.isNotBlank()) {
                        Text(
                            text = SearchUtils.createHighlightedText(
                                text = book.author,
                                query = searchQuery,
                                baseColor = theme.secondaryTextColor,
                                highlightColor = theme.accentColor,
                                fontSize = 12.sp
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = theme.alphaSubtle))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (progress > 0f && progress < 1f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            color = theme.accentColor,
                            trackColor = theme.accentColor.copy(alpha = theme.alphaSubtle)
                        )
                    }
                }
            }

            if (quickActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickActions.forEach { action ->
                        AssistChip(
                            onClick = action.onClick,
                            label = { Text(action.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.label
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
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
private fun InfoBanner(message: String, theme: AppTheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.accentColor.copy(alpha = theme.alphaSubtle))
            .padding(16.dp)
    ) {
        Text(
            text = message,
            color = theme.accentColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState(theme: AppTheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.surfaceColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No items match your filters yet.",
            color = theme.primaryTextColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adjusting the filters or importing new books to get started.",
            color = theme.secondaryTextColor,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

private data class QuickAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)
