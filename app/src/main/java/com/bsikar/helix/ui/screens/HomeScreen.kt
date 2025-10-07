package com.bsikar.helix.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ImportProgress
import com.bsikar.helix.data.model.TagCategory
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.SearchBar
import com.bsikar.helix.ui.components.SearchUtils
import com.bsikar.helix.ui.components.CompactImportProgress

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    theme: AppTheme,
    currentTheme: ThemeMode,
    allBooks: List<Book>,
    readingBooks: List<Book>,
    audiobooks: List<Book>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBookSelected: (Book) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    onRescanLibrary: () -> Unit,
    isRefreshing: Boolean,
    importProgress: List<ImportProgress>,
    snackbarHostState: SnackbarHostState,
    scanMessage: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val filteredBooks = remember(allBooks, searchQuery) {
        if (searchQuery.isBlank()) allBooks
        else SearchUtils.fuzzySearch(
            items = allBooks,
            query = searchQuery,
            getText = { it.title },
            getSecondaryText = { it.author },
            threshold = 0.2
        ).map { it.item }
    }

    LaunchedEffect(scanMessage) {
        if (scanMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(scanMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Helix",
                            style = MaterialTheme.typography.headlineMedium,
                            color = theme.primaryTextColor
                        )
                        Text(
                            text = "${allBooks.size} items in your library",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.secondaryTextColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (currentTheme == ThemeMode.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = theme.primaryTextColor
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = theme.primaryTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.primaryTextColor
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRescanLibrary,
                expanded = true,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CloudSync,
                        contentDescription = null
                    )
                },
                text = {
                    Text(if (isRefreshing) "Scanning..." else "Scan library")
                },
                containerColor = theme.accentColor,
                contentColor = Color.White
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = theme.backgroundColor
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                SearchBar(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    theme = theme
                )
            }

            if (importProgress.isNotEmpty()) {
                item {
                    CompactImportProgress(importProgress)
                }
            }

            if (readingBooks.isNotEmpty()) {
                item {
                    SectionTitle("Continue reading", theme)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        readingBooks.forEach { book ->
                            FeaturedBookCard(
                                book = book,
                                theme = theme,
                                onClick = { onBookSelected(book) }
                            )
                        }
                    }
                }
            }

            if (audiobooks.isNotEmpty()) {
                item {
                    SectionTitle("Dive into audiobooks", theme)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        audiobooks.forEach { book ->
                            FeaturedBookCard(
                                book = book,
                                theme = theme,
                                onClick = { onBookSelected(book) }
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("All titles", theme)
                if (filteredBooks.isEmpty()) {
                    EmptyState(theme)
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        filteredBooks.forEach { book ->
                            LibraryBookCard(
                                book = book,
                                theme = theme,
                                onClick = { onBookSelected(book) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(label: String, theme: AppTheme) {
    Text(
        text = label,
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = theme.primaryTextColor
    )
}

@Composable
private fun EmptyState(theme: AppTheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(min = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your library is waiting",
            style = MaterialTheme.typography.titleMedium,
            color = theme.primaryTextColor
        )
        Text(
            text = "Import EPUBs and M4B audiobooks to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.secondaryTextColor
        )
    }
}

@Composable
private fun FeaturedBookCard(
    book: Book,
    theme: AppTheme,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = theme.surfaceColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .heightIn(min = 220.dp)
            .fillMaxWidth(0.5f)
    ) {
        Box {
            if (book.shouldShowCoverArt()) {
                AsyncImage(
                    model = book.coverImagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000))
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp)
                        .background(book.getEffectiveCoverColor())
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryBookCard(
    book: Book,
    theme: AppTheme,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = theme.surfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                if (book.shouldShowCoverArt()) {
                    AsyncImage(
                        model = book.coverImagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(book.getEffectiveCoverColor())
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = theme.primaryTextColor
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ProgressPill(book)

            val moodTags = remember(book.tags) {
                book.getTagsByCategory(TagCategory.THEME)
            }

            if (moodTags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    moodTags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag.name) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = theme.accentColor.copy(alpha = 0.12f),
                                labelColor = theme.accentColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressPill(book: Book) {
    val progress = remember(book.progress) { book.progress.coerceIn(0f, 1f) }
    val label = when {
        book.isAudiobook() -> {
            if (book.durationMs > 0L) {
                val hours = book.durationMs / (1000 * 60 * 60)
                val minutes = (book.durationMs / (1000 * 60)) % 60
                "${book.getAudioProgress() * 100f.toInt()}% • ${hours}h ${minutes}m"
            } else {
                "${(progress * 100).toInt()}%"
            }
        }
        else -> "${(progress * 100).toInt()}% completed"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
