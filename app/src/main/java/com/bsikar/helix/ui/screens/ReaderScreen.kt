package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.ReadingMode
import com.bsikar.helix.data.TextAlignment
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.HighlightedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    theme: AppTheme,
    onBackClick: () -> Unit,
    onUpdateReadingPosition: (String, Int, Int, Int) -> Unit,
    preferencesManager: UserPreferencesManager
) {
    var currentPage by remember { mutableIntStateOf(book.currentPage) }
    val totalPages = book.totalPages
    var showSettings by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState(initial = book.scrollPosition)
    
    // Use persistent reader settings from UserPreferencesManager
    val userPreferences by preferencesManager.preferences
    var readerSettings by remember { mutableStateOf(userPreferences.selectedReaderSettings) }
    
    // Update settings when preferences change
    LaunchedEffect(userPreferences.selectedReaderSettings) {
        readerSettings = userPreferences.selectedReaderSettings
    }
    
    // Save reading position when page changes
    LaunchedEffect(currentPage) {
        onUpdateReadingPosition(book.id, currentPage, book.currentChapter, scrollState.value)
    }
    
    // Save scroll position periodically
    LaunchedEffect(scrollState.value) {
        onUpdateReadingPosition(book.id, currentPage, book.currentChapter, scrollState.value)
    }
    
    // Sample chapters
    val sampleChapters = remember {
        listOf(
            "Chapter 1: The Beginning",
            "Chapter 2: The Journey Starts",
            "Chapter 3: First Obstacles",
            "Chapter 4: New Allies",
            "Chapter 5: Dark Revelations",
            "Chapter 6: The Hidden Truth",
            "Chapter 7: Confrontation",
            "Chapter 8: Battle of Wills",
            "Chapter 9: Final Stand",
            "Chapter 10: Resolution"
        )
    }
    
    // Sample book content
    val sampleContent = """
        Chapter 1: The Beginning
        
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
        
        Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        
        Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
        
        Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.
        
        Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.
        
        Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur.
        
        At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident.
    """.trimIndent()

    if (showSettings) {
        ReaderSettingsScreen(
            settings = readerSettings,
            onSettingsChange = { newSettings -> 
                readerSettings = newSettings
                preferencesManager.updateReaderSettings(newSettings)
            },
            theme = theme,
            onBackClick = { showSettings = false },
            preferencesManager = preferencesManager
        )
        return
    }
    
    // Apply brightness to background color
    val adjustedBackgroundColor = applyBrightness(readerSettings.readingMode.backgroundColor, readerSettings.brightness)

    Scaffold(
        containerColor = adjustedBackgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.primaryTextColor,
                ),
                title = {
                    Column {
                        Text(
                            text = book.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.primaryTextColor,
                            maxLines = 1
                        )
                        Text(
                            text = book.author,
                            fontSize = 12.sp,
                            color = theme.secondaryTextColor,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.primaryTextColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Filled.Bookmark,
                            contentDescription = "Bookmark",
                            tint = theme.secondaryTextColor
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Reading Settings",
                            tint = theme.secondaryTextColor
                        )
                    }
                }
            )
        },
        bottomBar = {
            ReaderBottomBar(
                currentPage = currentPage,
                totalPages = totalPages,
                progress = currentPage.toFloat() / totalPages,
                onPreviousPage = { if (currentPage > 1) currentPage-- },
                onNextPage = { if (currentPage < totalPages) currentPage++ },
                onProgressTap = { showChapterDialog = true },
                theme = theme
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { currentPage.toFloat() / totalPages },
                modifier = Modifier.fillMaxWidth(),
                color = theme.accentColor,
                trackColor = theme.secondaryTextColor.copy(alpha = 0.2f)
            )
            
            // Content area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = readerSettings.marginHorizontal.dp,
                        vertical = readerSettings.marginVertical.dp
                    )
            ) {
                Text(
                    text = sampleContent,
                    fontSize = readerSettings.fontSize.sp,
                    lineHeight = (readerSettings.fontSize * readerSettings.lineHeight).sp,
                    color = readerSettings.readingMode.textColor,
                    textAlign = when (readerSettings.textAlign) {
                        TextAlignment.LEFT -> TextAlign.Start
                        TextAlignment.CENTER -> TextAlign.Center
                        TextAlignment.JUSTIFY -> TextAlign.Justify
                    }
                )
            }
        }
    }
    
    // Chapter selection dialog
    if (showChapterDialog) {
        ChapterSelectionDialog(
            chapters = sampleChapters,
            currentChapter = book.currentChapter,
            onChapterSelected = { chapterIndex ->
                // Navigate to selected chapter
                // For demo purposes, just change the page
                currentPage = (chapterIndex + 1) * 15 // Simulate chapter starts
                showChapterDialog = false
            },
            onDismiss = { showChapterDialog = false },
            theme = theme
        )
    }
}

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    progress: Float,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onProgressTap: () -> Unit = {},
    theme: AppTheme
) {
    Surface(
        color = theme.surfaceColor,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous page button
            IconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 1
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Page",
                    tint = if (currentPage > 1) theme.primaryTextColor else theme.secondaryTextColor
                )
            }
            
            // Page info (clickable)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onProgressTap() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$currentPage of $totalPages",
                    fontSize = 14.sp,
                    color = theme.primaryTextColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(progress * 100).toInt()}% complete",
                    fontSize = 12.sp,
                    color = theme.secondaryTextColor
                )
            }
            
            // Next page button
            IconButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Page",
                    tint = if (currentPage < totalPages) theme.primaryTextColor else theme.secondaryTextColor
                )
            }
        }
    }
}

// Helper function to apply brightness to colors
@Composable
fun applyBrightness(color: Color, brightness: Float): Color {
    val argb = color.toArgb()
    val alpha = (argb shr 24) and 0xFF
    val red = ((argb shr 16) and 0xFF) * brightness
    val green = ((argb shr 8) and 0xFF) * brightness
    val blue = (argb and 0xFF) * brightness
    
    return Color(
        red = (red.coerceIn(0f, 255f) / 255f),
        green = (green.coerceIn(0f, 255f) / 255f),
        blue = (blue.coerceIn(0f, 255f) / 255f),
        alpha = alpha / 255f
    )
}

@Composable
fun ChapterSelectionDialog(
    chapters: List<String>,
    currentChapter: Int,
    onChapterSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    theme: AppTheme
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter chapters based on search query
    val filteredChapters = remember(chapters, searchQuery) {
        if (searchQuery.isBlank()) {
            chapters.mapIndexed { index, title -> index to title }
        } else {
            chapters.mapIndexed { index, title -> index to title }
                .filter { (index, title) ->
                    title.contains(searchQuery, ignoreCase = true) ||
                    "${index + 1}".contains(searchQuery) // Search by chapter number too
                }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Select Chapter",
                    color = theme.primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = {
                        Text(
                            text = "Search chapters...",
                            color = theme.secondaryTextColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = theme.secondaryTextColor
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    tint = theme.secondaryTextColor
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.secondaryTextColor.copy(alpha = 0.3f),
                        focusedLabelColor = theme.accentColor,
                        unfocusedLabelColor = theme.secondaryTextColor,
                        cursorColor = theme.accentColor,
                        focusedTextColor = theme.primaryTextColor,
                        unfocusedTextColor = theme.primaryTextColor
                    ),
                    singleLine = true
                )
            }
        },
        text = {
            Column {
                // Results count
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "${filteredChapters.size} chapter${if (filteredChapters.size != 1) "s" else ""} found",
                        fontSize = 12.sp,
                        color = theme.secondaryTextColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // Chapter list
                LazyColumn(
                    modifier = Modifier.height(350.dp)
                ) {
                    if (filteredChapters.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "No results",
                                    tint = theme.secondaryTextColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No chapters found",
                                    fontSize = 16.sp,
                                    color = theme.secondaryTextColor,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Try a different search term",
                                    fontSize = 14.sp,
                                    color = theme.secondaryTextColor.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(count = filteredChapters.size) { listIndex ->
                            val (originalIndex, title) = filteredChapters[listIndex]
                            val isCurrentChapter = (originalIndex + 1) == currentChapter
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChapterSelected(originalIndex) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                                    .background(
                                        color = if (isCurrentChapter) theme.accentColor.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chapter number
                                Text(
                                    text = "${originalIndex + 1}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentChapter) theme.accentColor else theme.secondaryTextColor,
                                    modifier = Modifier.width(32.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Chapter title with search highlighting
                                if (searchQuery.isNotEmpty() && searchQuery.isNotBlank()) {
                                    HighlightedText(
                                        text = title,
                                        searchQuery = searchQuery,
                                        normalColor = if (isCurrentChapter) theme.accentColor else theme.primaryTextColor,
                                        highlightColor = theme.accentColor,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrentChapter) FontWeight.Medium else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        color = if (isCurrentChapter) theme.accentColor else theme.primaryTextColor,
                                        fontWeight = if (isCurrentChapter) FontWeight.Medium else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                // Current indicator
                                if (isCurrentChapter) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Currently reading",
                                        tint = theme.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = theme.accentColor)
            }
        },
        containerColor = theme.surfaceColor
    )
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    val theme = ThemeManager.lightTheme
    val sampleBook = Book(
        title = "Clockwork Planet",
        author = "Yuu Kamiya",
        coverColor = androidx.compose.ui.graphics.Color(0xFFFFD700),
        progress = 0.3f
    )
    
    MaterialTheme {
        ReaderScreen(
            book = sampleBook,
            theme = theme,
            onBackClick = { },
            onUpdateReadingPosition = { _, _, _, _ -> },
            preferencesManager = UserPreferencesManager(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}