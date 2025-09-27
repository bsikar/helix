package com.bsikar.helix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.R
import com.bsikar.helix.data.model.EpubChapter
import com.bsikar.helix.data.model.EpubTocEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterNavigationSheet(
    chapters: List<EpubChapter>,
    tableOfContents: List<EpubTocEntry>,
    currentChapter: EpubChapter?,
    readingProgress: Float, // 0.0 to 1.0
    onChapterSelect: (EpubChapter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTableOfContents by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    // Filter chapters and TOC based on search query
    val filteredChapters = remember(chapters, searchQuery) {
        if (searchQuery.isBlank()) {
            chapters
        } else {
            chapters.filter { chapter ->
                chapter.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val filteredTableOfContents = remember(tableOfContents, searchQuery) {
        if (searchQuery.isBlank()) {
            tableOfContents
        } else {
            tableOfContents.filter { tocEntry ->
                tocEntry.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isSearchActive) {
                    Text(
                        text = stringResource(R.string.chapter_navigation),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search chapters")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    }
                } else {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search chapters...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                searchQuery = ""
                                isSearchActive = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        singleLine = true
                    )
                }
            }
            
            // Tab row for switching between TOC and chapter list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                FilterChip(
                    onClick = { showTableOfContents = true },
                    selected = showTableOfContents,
                    label = { Text(stringResource(R.string.table_of_contents)) },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    onClick = { showTableOfContents = false },
                    selected = !showTableOfContents,
                    label = { Text(stringResource(R.string.chapters)) }
                )
            }
            
            // Reading progress indicator
            if (readingProgress > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.reading_progress),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${kotlin.math.round(readingProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        LinearProgressIndicator(
                            progress = { readingProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            
            // Content based on selected tab
            if (showTableOfContents && tableOfContents.isNotEmpty()) {
                if (filteredTableOfContents.isEmpty() && searchQuery.isNotBlank()) {
                    // Show no results message
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No chapters found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    TableOfContentsView(
                        tableOfContents = filteredTableOfContents,
                        currentChapter = currentChapter,
                        onChapterSelect = onChapterSelect,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                if (filteredChapters.isEmpty() && searchQuery.isNotBlank()) {
                    // Show no results message
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No chapters found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    ChapterListView(
                        chapters = filteredChapters,
                        currentChapter = currentChapter,
                        onChapterSelect = onChapterSelect,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Bottom spacing for gesture area
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TableOfContentsView(
    tableOfContents: List<EpubTocEntry>,
    currentChapter: EpubChapter?,
    onChapterSelect: (EpubChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(tableOfContents) { tocEntry ->
            TocEntryItem(
                tocEntry = tocEntry,
                currentChapter = currentChapter,
                onChapterSelect = onChapterSelect,
                level = 0
            )
        }
    }
}

@Composable
private fun TocEntryItem(
    tocEntry: EpubTocEntry,
    currentChapter: EpubChapter?,
    onChapterSelect: (EpubChapter) -> Unit,
    level: Int,
    modifier: Modifier = Modifier
) {
    val isCurrentChapter = currentChapter?.href == tocEntry.href
    val startPadding = (level * 16).dp
    
    Column(modifier = modifier) {
        // Main entry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    // Convert TocEntry to EpubChapter for navigation
                    val chapter = EpubChapter(
                        id = tocEntry.href.hashCode().toString(),
                        title = tocEntry.title,
                        href = tocEntry.href,
                        content = "",
                        order = 0 // Will be resolved by the navigation system
                    )
                    onChapterSelect(chapter)
                }
                .background(
                    if (isCurrentChapter) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else Color.Transparent
                )
                .padding(horizontal = 12.dp + startPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tocEntry.children.isNotEmpty()) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }
            
            Text(
                text = tocEntry.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentChapter) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Children entries
        tocEntry.children.forEach { childEntry ->
            TocEntryItem(
                tocEntry = childEntry,
                currentChapter = currentChapter,
                onChapterSelect = onChapterSelect,
                level = level + 1
            )
        }
    }
}

@Composable
private fun ChapterListView(
    chapters: List<EpubChapter>,
    currentChapter: EpubChapter?,
    onChapterSelect: (EpubChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(chapters.sortedBy { it.order }) { chapter ->
            ChapterItem(
                chapter = chapter,
                isCurrentChapter = currentChapter?.id == chapter.id,
                onChapterSelect = onChapterSelect
            )
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: EpubChapter,
    isCurrentChapter: Boolean,
    onChapterSelect: (EpubChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onChapterSelect(chapter) },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentChapter) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentChapter) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter number
            Text(
                text = "${chapter.order + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentChapter) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .background(
                        color = if (isCurrentChapter) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Chapter title
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentChapter) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            if (isCurrentChapter) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.current_chapter),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}