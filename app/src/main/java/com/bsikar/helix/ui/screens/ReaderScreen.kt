package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.TextStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.ReadingMode
import com.bsikar.helix.data.TextAlignment
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.SearchUtils
import com.bsikar.helix.ui.components.BookmarkDialog
import com.bsikar.helix.ui.components.ChapterNavigationSheet
import com.bsikar.helix.data.model.Bookmark
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.data.model.ParsedEpub
import com.bsikar.helix.data.EpubParser
import com.bsikar.helix.viewmodels.ReaderViewModel
import org.jsoup.Jsoup
import coil.compose.AsyncImage
import com.bsikar.helix.ui.components.EpubImageComponent
import java.io.File

/**
 * Represents different types of content that can appear in EPUB
 */
sealed class ContentElement {
    data class TextElement(val text: androidx.compose.ui.text.AnnotatedString) : ContentElement()
    data class ImageElement(
        val imagePath: String,
        val alt: String = "",
        val originalSrc: String = ""
    ) : ContentElement()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    book: com.bsikar.helix.data.model.Book,
    theme: AppTheme,
    onBackClick: () -> Unit,
    onUpdateReadingPosition: (String, Int, Int, Int) -> Unit,
    onUpdateBookSettings: (Book) -> Unit = { _ -> },
    preferencesManager: UserPreferencesManager,
    libraryManager: LibraryManager,
    readerViewModel: ReaderViewModel = hiltViewModel()
) {
    var currentPage by remember { mutableIntStateOf(book.currentPage) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // State from ReaderViewModel
    val parsedEpub by readerViewModel.parsedEpub.collectAsState()
    val currentContent by readerViewModel.currentContent.collectAsState()
    val isLoadingContent by readerViewModel.isLoadingContent.collectAsState()
    val isLoadingChapter by readerViewModel.isLoadingChapter.collectAsState()
    val loadingError by readerViewModel.loadingError.collectAsState()
    val currentChapterIndex by readerViewModel.currentChapterIndex.collectAsState()
    
    // Chapter navigation state
    val chapters by readerViewModel.chapters.collectAsState()
    val tableOfContents by readerViewModel.tableOfContents.collectAsState()
    val currentChapter by readerViewModel.currentChapter.collectAsState()
    val readingProgressPercentage by readerViewModel.readingProgressPercentage.collectAsState()
    
    // Use dynamic chapter count for imported EPUBs - ensure non-zero value during loading
    val totalPages = remember(parsedEpub, book.totalPages) {
        if (book.isImported && parsedEpub != null) {
            maxOf(1, parsedEpub!!.chapters.size)
        } else {
            maxOf(1, book.totalPages)
        }
    }
    
    // Get bookmarks for this book
    val bookmarks by remember { derivedStateOf { preferencesManager.getBookmarks(book.id) } }
    val isCurrentPageBookmarked by remember { 
        derivedStateOf { 
            preferencesManager.isPageBookmarked(book.id, currentChapterIndex, currentPage) 
        } 
    }
    
    // Use persistent reader settings from UserPreferencesManager
    val userPreferences by preferencesManager.preferences
    var readerSettings by remember { mutableStateOf(userPreferences.selectedReaderSettings) }
    
    // Update settings when preferences change and invalidate content cache
    LaunchedEffect(userPreferences.selectedReaderSettings) {
        val newSettings = userPreferences.selectedReaderSettings
        if (readerSettings != newSettings) {
            readerSettings = newSettings
            readerViewModel.invalidateContentCache()
        }
    }
    
    // Load book content when book changes
    LaunchedEffect(book.id) {
        readerViewModel.loadBook(book)
    }
    
    // Save reading position when page or chapter changes
    LaunchedEffect(currentPage, currentChapterIndex) {
        onUpdateReadingPosition(book.id, currentPage, currentChapterIndex + 1, 0) // scrollPosition = 0 for now
    }
    
    // Get chapters from parsed EPUB or use sample chapters for non-imported books
    val chapterTitles = remember(parsedEpub) {
        if (book.isImported && parsedEpub != null) {
            parsedEpub!!.chapters.map { it.title }
        } else {
            // Sample chapters for non-imported books
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
    }
    
    // Load chapter content when EPUB is loaded or chapter changes
    LaunchedEffect(parsedEpub, currentChapterIndex) {
        if (book.isImported && parsedEpub != null) {
            readerViewModel.loadChapterContent(parsedEpub!!, currentChapterIndex, context)
        }
    }

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
    
    // Apply brightness to background color with accessibility support
    val baseBackgroundColor = if (theme.isReaderOptimized) {
        theme.readerBackgroundColor
    } else {
        getAccessibleBackgroundColor(readerSettings)
    }
    val adjustedBackgroundColor = applyBrightness(baseBackgroundColor, readerSettings.brightness)

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
                    // Single bookmark icon with smart behavior
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = { 
                                // Short press: add bookmark instantly (regardless of current state)
                                val newBookmark = Bookmark(
                                    bookId = book.id,
                                    bookTitle = book.title,
                                    chapterNumber = currentChapterIndex,
                                    pageNumber = currentPage,
                                    scrollPosition = 0 // Scroll position tracking temporarily disabled
                                )
                                preferencesManager.addBookmark(newBookmark)
                            },
                            onLongClick = {
                                // Long press: always show bookmark management dialog
                                showBookmarkDialog = true
                            }
                        )
                        .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                            Icon(
                                if (isCurrentPageBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (isCurrentPageBookmarked) "Manage bookmarks" else "Add bookmark",
                                tint = if (isCurrentPageBookmarked) theme.accentColor else theme.secondaryTextColor
                            )
                            // Show badge if there are multiple bookmarks
                            if (bookmarks.size > 1) {
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = bookmarks.size.toString(),
                                        fontSize = 10.sp,
                                        color = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            }
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
                currentPage = if (book.isImported) {
                    if (parsedEpub != null) currentChapterIndex + 1 else 1
                } else {
                    currentPage
                },
                totalPages = if (book.isImported) {
                    if (parsedEpub != null) totalPages else 1
                } else {
                    totalPages
                },
                progress = if (book.isImported) {
                    if (parsedEpub != null && totalPages > 0) {
                        (currentChapterIndex + 1).toFloat() / totalPages
                    } else {
                        0f
                    }
                } else {
                    if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
                },
                onPreviousPage = { 
                    if (book.isImported && parsedEpub != null) {
                        // Chapter-based navigation for EPUBs
                        readerViewModel.previousChapter(context)
                    } else {
                        // Page-based navigation for non-imported books
                        if (currentPage > 1) currentPage--
                    }
                },
                onNextPage = { 
                    if (book.isImported && parsedEpub != null) {
                        // Chapter-based navigation for EPUBs
                        readerViewModel.nextChapter(context)
                    } else {
                        // Page-based navigation for non-imported books
                        if (currentPage < totalPages) currentPage++
                    }
                },
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
                progress = { 
                    if (book.isImported) {
                        if (parsedEpub != null && totalPages > 0) {
                            (currentChapterIndex + 1).toFloat() / totalPages
                        } else {
                            0f
                        }
                    } else {
                        if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = theme.accentColor,
                trackColor = theme.secondaryTextColor.copy(alpha = 0.2f)
            )
            
            // Content area
            if (isLoadingContent) {
                // Show loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = theme.accentColor
                    )
                }
            } else if (loadingError != null) {
                // Show error message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Failed to load book content",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = loadingError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else if (!book.isImported) {
                // Show placeholder for non-imported books
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = readerSettings.marginHorizontal.dp,
                            vertical = readerSettings.marginVertical.dp
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val textStyle = createAccessibleTextStyle(readerSettings)
                    Text(
                        text = "This book needs to be imported as an EPUB file to view its content.",
                        style = textStyle,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Create a stable snapshot of content to prevent concurrent modification crashes
                val stableContent = remember(currentContent) { currentContent.toList() }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = readerSettings.marginHorizontal.dp,
                        vertical = readerSettings.marginVertical.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = stableContent.size,
                        key = { index -> "content_$index" }
                    ) { index ->
                        // Safety check to prevent index out of bounds
                        if (index < stableContent.size) {
                            val element = stableContent[index]
                            when (element) {
                                is ContentElement.TextElement -> {
                                    val textStyle = createAccessibleTextStyle(readerSettings)
                                    Text(
                                        text = element.text,
                                        style = textStyle,
                                        textAlign = when (readerSettings.textAlign) {
                                            TextAlignment.LEFT -> TextAlign.Start
                                            TextAlignment.CENTER -> TextAlign.Center
                                            TextAlignment.JUSTIFY -> TextAlign.Justify
                                        }
                                    )
                                }
                                is ContentElement.ImageElement -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Load image directly from EPUB if available
                                        if (book.isImported && parsedEpub != null) {
                                            val epub = parsedEpub!!
                                            EpubImageComponent(
                                                epubFilePath = epub.filePath,
                                                originalUri = book.originalUri,
                                                imageZipPath = element.imagePath,
                                                contentDescription = element.alt.ifEmpty { "EPUB Image" },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp)
                                            )
                                        } else {
                                            // Fallback for extracted files (if any)
                                            AsyncImage(
                                                model = File(element.imagePath),
                                                contentDescription = element.alt.ifEmpty { "EPUB Image" },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Chapter navigation sheet
    if (showChapterDialog) {
        ChapterNavigationSheet(
            chapters = chapters,
            tableOfContents = tableOfContents,
            currentChapter = currentChapter,
            readingProgress = readingProgressPercentage,
            onChapterSelect = { chapter ->
                readerViewModel.navigateToChapter(chapter)
                currentPage = 1 // Reset to first page of chapter
                showChapterDialog = false
            },
            onDismiss = { showChapterDialog = false }
        )
    }
    
    // Bookmark management dialog
    if (showBookmarkDialog) {
        BookmarkDialog(
            bookmarks = bookmarks,
            onDismiss = { showBookmarkDialog = false },
            onBookmarkClick = { bookmark ->
                // Navigate to bookmarked location
                currentPage = bookmark.pageNumber
                // Note: In a real implementation, you'd also update scroll position
                // Note: scroll position restoration temporarily disabled
                showBookmarkDialog = false
            },
            onBookmarkDelete = { bookmarkId ->
                preferencesManager.removeBookmark(bookmarkId)
            },
            onBookmarkEditNote = { bookmarkId, note ->
                preferencesManager.updateBookmarkNote(bookmarkId, note)
            },
            theme = theme,
            currentChapter = currentChapterIndex,
            currentPage = currentPage,
            onQuickBookmark = {
                val newBookmark = Bookmark(
                    bookId = book.id,
                    bookTitle = book.title,
                    chapterNumber = currentChapterIndex,
                    pageNumber = currentPage,
                    scrollPosition = 0 // Scroll position tracking temporarily disabled
                )
                preferencesManager.addBookmark(newBookmark)
            }
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
                                Text(
                                    text = if (searchQuery.isNotEmpty() && searchQuery.isNotBlank()) {
                                        SearchUtils.createHighlightedText(
                                            text = title,
                                            query = searchQuery,
                                            baseColor = if (isCurrentChapter) theme.accentColor else theme.primaryTextColor,
                                            highlightColor = theme.accentColor,
                                            fontSize = 14.sp,
                                            highlightFontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        androidx.compose.ui.text.AnnotatedString(title)
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrentChapter) FontWeight.Medium else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                    color = if (searchQuery.isNotEmpty() && searchQuery.isNotBlank()) Color.Unspecified else if (isCurrentChapter) theme.accentColor else theme.primaryTextColor
                                )
                                
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

/**
 * Converts HTML content to AnnotatedString with proper formatting
 */
fun parseHtmlToAnnotatedString(htmlContent: String, readerSettings: ReaderSettings, images: Map<String, String> = emptyMap()): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        try {
            val doc = org.jsoup.Jsoup.parse(htmlContent)
            
            // Remove script and style elements
            doc.select("script, style").remove()
            
            // Process elements in order to maintain structure
            parseElement(doc.body(), this, readerSettings, images)
            
        } catch (e: Exception) {
            append("Error parsing chapter content: ${e.message}")
        }
    }
}

/**
 * Normalize image paths to match the keys in the images map
 * Converts relative paths like "../Images/cover.jpg" to "Images/cover.jpg"
 */
private fun normalizeImagePath(src: String): String {
    if (src.isBlank()) return src
    
    // Remove leading ../ patterns
    var normalized = src
    while (normalized.startsWith("../")) {
        normalized = normalized.substring(3)
    }
    
    // Remove leading ./ patterns
    while (normalized.startsWith("./")) {
        normalized = normalized.substring(2)
    }
    
    // Remove leading slash
    if (normalized.startsWith("/")) {
        normalized = normalized.substring(1)
    }
    
    return normalized
}

/**
 * Parse HTML content to a list of ContentElement objects for mixed content display
 */
fun parseHtmlToContentElements(
    htmlContent: String,
    readerSettings: ReaderSettings,
    images: Map<String, String>
): List<ContentElement> {
    val doc = Jsoup.parse(htmlContent)
    val elements = mutableListOf<ContentElement>()
    
    try {
        // Remove script and style elements
        doc.select("script, style").remove()
        
        // Process elements and collect content
        parseElementToContentList(doc.body(), elements, readerSettings, images)
        
    } catch (e: Exception) {
        elements.add(ContentElement.TextElement(
            androidx.compose.ui.text.AnnotatedString("Error parsing chapter content: ${e.message}")
        ))
    }
    
    return elements.ifEmpty { 
        listOf(ContentElement.TextElement(androidx.compose.ui.text.AnnotatedString("No content found")))
    }
}

/**
 * Process HTML elements and convert to ContentElement list
 */
private fun parseElementToContentList(
    element: org.jsoup.nodes.Element,
    elements: MutableList<ContentElement>,
    readerSettings: ReaderSettings,
    images: Map<String, String>
) {
    var textBuilder = androidx.compose.ui.text.AnnotatedString.Builder()
    var hasTextContent = false
    
    for (node in element.childNodes()) {
        when (node) {
            is org.jsoup.nodes.TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) {
                    textBuilder.append(text)
                    hasTextContent = true
                }
            }
            is org.jsoup.nodes.Element -> {
                when (node.tagName().lowercase()) {
                    "img" -> {
                        // If we have accumulated text, add it first
                        if (hasTextContent) {
                            elements.add(ContentElement.TextElement(textBuilder.toAnnotatedString()))
                            // Create a new builder for the next text segment
                            textBuilder = androidx.compose.ui.text.AnnotatedString.Builder()
                            hasTextContent = false
                        }
                        
                        // Add the image
                        val src = node.attr("src")
                        val alt = node.attr("alt")
                        val normalizedSrc = normalizeImagePath(src)
                        val imagePath = images[normalizedSrc] ?: images[src]
                        
                        if (imagePath != null) {
                            elements.add(ContentElement.ImageElement(
                                imagePath = imagePath,
                                alt = alt,
                                originalSrc = src
                            ))
                        }
                    }
                    else -> {
                        // Check if this element contains any img tags
                        val nestedImgs = node.select("img")
                        if (nestedImgs.isNotEmpty()) {
                            // If we have accumulated text, add it first
                            if (hasTextContent) {
                                elements.add(ContentElement.TextElement(textBuilder.toAnnotatedString()))
                                textBuilder = androidx.compose.ui.text.AnnotatedString.Builder()
                                hasTextContent = false
                            }
                            
                            // Recursively parse this element for images and text
                            parseElementToContentList(node, elements, readerSettings, images)
                        } else {
                            // No images, just parse for text content
                            parseElementRecursive(node, textBuilder, readerSettings, images)
                            hasTextContent = true
                        }
                    }
                }
            }
        }
    }
    
    // Add any remaining text content
    if (hasTextContent) {
        elements.add(ContentElement.TextElement(textBuilder.toAnnotatedString()))
    }
}

/**
 * Parse HTML elements recursively for text content (similar to existing parseElement but for text builder)
 */
private fun parseElementRecursive(
    node: org.jsoup.nodes.Element,
    builder: androidx.compose.ui.text.AnnotatedString.Builder,
    readerSettings: ReaderSettings,
    images: Map<String, String>
) {
    when (node.tagName().lowercase()) {
        "h1" -> {
            builder.append("\n\n")
            val fontSizeMultiplier = if (readerSettings.useSystemFontSize) 2.0f else 1.8f
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                fontSize = (readerSettings.fontSize * fontSizeMultiplier).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = readerSettings.letterSpacing.sp
            ))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n\n")
        }
        "h2" -> {
            builder.append("\n\n")
            val fontSizeMultiplier = if (readerSettings.useSystemFontSize) 1.8f else 1.6f
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                fontSize = (readerSettings.fontSize * fontSizeMultiplier).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = readerSettings.letterSpacing.sp
            ))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n\n")
        }
        "h3" -> {
            builder.append("\n\n")
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                fontSize = (readerSettings.fontSize * 1.4f).sp,
                fontWeight = FontWeight.Bold
            ))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n\n")
        }
        "h4", "h5", "h6" -> {
            builder.append("\n\n")
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                fontSize = (readerSettings.fontSize * 1.2f).sp,
                fontWeight = FontWeight.Bold
            ))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n\n")
        }
        "p" -> {
            parseTextContent(node, builder, readerSettings, images)
            builder.append("\n\n")
        }
        "br" -> {
            builder.append("\n")
        }
        "strong", "b" -> {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
        }
        "em", "i" -> {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
        }
        "u" -> {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
        }
        "blockquote" -> {
            builder.append("\n\n")
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = readerSettings.readingMode.textColor.copy(alpha = 0.8f)
            ))
            builder.append("\"")
            parseTextContent(node, builder, readerSettings, images)
            builder.append("\"")
            builder.pop()
            builder.append("\n\n")
        }
        "img" -> {
            // Skip images here as they're handled separately
        }
        else -> {
            parseTextContent(node, builder, readerSettings, images)
        }
    }
}

/**
 * Parse text content from elements, handling child nodes
 */
private fun parseTextContent(
    element: org.jsoup.nodes.Element,
    builder: androidx.compose.ui.text.AnnotatedString.Builder,
    readerSettings: ReaderSettings,
    images: Map<String, String>
) {
    for (child in element.childNodes()) {
        when (child) {
            is org.jsoup.nodes.TextNode -> {
                builder.append(child.text())
            }
            is org.jsoup.nodes.Element -> {
                if (child.tagName().lowercase() != "img") {
                    parseElementRecursive(child, builder, readerSettings, images)
                }
            }
        }
    }
}

/**
 * Recursively parse HTML elements and apply appropriate formatting
 */
private fun parseElement(
    element: org.jsoup.nodes.Element, 
    builder: androidx.compose.ui.text.AnnotatedString.Builder,
    readerSettings: ReaderSettings,
    images: Map<String, String> = emptyMap()
) {
    for (node in element.childNodes()) {
        when (node) {
            is org.jsoup.nodes.TextNode -> {
                // Add text content
                val text = node.text()
                if (text.isNotBlank()) {
                    builder.append(text)
                }
            }
            is org.jsoup.nodes.Element -> {
                when (node.tagName().lowercase()) {
                    "h1" -> {
                        builder.append("\n\n")
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                            fontSize = (readerSettings.fontSize * 1.8f).sp,
                            fontWeight = FontWeight.Bold
                        ))
                        parseElement(node, builder, readerSettings, images)
                        builder.pop()
                        builder.append("\n\n")
                    }
                    "h2" -> {
                        builder.append("\n\n")
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                            fontSize = (readerSettings.fontSize * 1.6f).sp,
                            fontWeight = FontWeight.Bold
                        ))
                        parseElement(node, builder, readerSettings, images)
                        builder.pop()
                        builder.append("\n\n")
                    }
                    "h3" -> {
                        builder.append("\n\n")
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                            fontSize = (readerSettings.fontSize * 1.4f).sp,
                            fontWeight = FontWeight.Bold
                        ))
                        parseElement(node, builder, readerSettings, images)
                        builder.pop()
                        builder.append("\n\n")
                    }
                    "h4", "h5", "h6" -> {
                        builder.append("\n\n")
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                            fontSize = (readerSettings.fontSize * 1.2f).sp,
                            fontWeight = FontWeight.Bold
                        ))
                        parseElement(node, builder, readerSettings, images)
                        builder.pop()
                        builder.append("\n\n")
                    }
                    "p" -> {
                        parseElement(node, builder, readerSettings, images)
                        builder.append("\n\n")
                    }
                    "br" -> {
                        builder.append("\n")
                    }
                    "div" -> {
                        parseElement(node, builder, readerSettings, images)
                        if (node.text().trim().isNotEmpty()) {
                            builder.append("\n")
                        }
                    }
                    "span" -> {
                        // Preserve spans but don't add extra spacing
                        parseElement(node, builder, readerSettings, images)
                    }
                    "center" -> {
                        builder.append("\n\n")
                        // Note: Compose doesn't support center alignment per span, 
                        // but we can add visual indicators
                        builder.append("          ") // Indentation to suggest centering
                        parseElement(node, builder, readerSettings, images)
                        builder.append("\n\n")
                    }
                    "hr" -> {
                        builder.append("\n\n────────────────────────────────\n\n")
                    }
                    "pre", "code" -> {
                        builder.append("\n\n")
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = (readerSettings.fontSize * 0.9f).sp,
                            background = readerSettings.readingMode.textColor.copy(alpha = 0.1f)
                        ))
                        parseElement(node, builder, readerSettings, images)
                        builder.pop()
                        builder.append("\n\n")
                    }
                    "strong", "b" -> {
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                        parseElement(node, builder, readerSettings, images)
                        builder.pop()
                    }
                    "em", "i" -> {
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                        parseElement(node, builder, readerSettings, images)
                        builder.pop()
                    }
                    "u" -> {
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                        parseElement(node, builder, readerSettings, images)
                        builder.pop()
                    }
                    "blockquote" -> {
                        builder.append("\n\n")
                        builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = readerSettings.readingMode.textColor.copy(alpha = 0.8f)
                        ))
                        builder.append("\"")
                        parseElement(node, builder, readerSettings, images)
                        builder.append("\"")
                        builder.pop()
                        builder.append("\n\n")
                    }
                    "li" -> {
                        builder.append("\n  • ")
                        parseElement(node, builder, readerSettings, images)
                    }
                    "ul" -> {
                        builder.append("\n")
                        parseElement(node, builder, readerSettings, images)
                        builder.append("\n")
                    }
                    "ol" -> {
                        builder.append("\n")
                        // For ordered lists, we would need to track the number
                        // For now, just use bullet points
                        parseElement(node, builder, readerSettings, images)
                        builder.append("\n")
                    }
                    "table", "tr", "td", "th" -> {
                        // Simple table handling - just add spacing
                        if (node.tagName() == "table") {
                            builder.append("\n\n")
                        }
                        parseElement(node, builder, readerSettings, images)
                        if (node.tagName() == "tr") {
                            builder.append("\n")
                        } else if (node.tagName() == "td" || node.tagName() == "th") {
                            builder.append("  |  ")
                        }
                        if (node.tagName() == "table") {
                            builder.append("\n\n")
                        }
                    }
                    "img" -> {
                        val src = node.attr("src")
                        val alt = node.attr("alt")
                        
                        // Normalize the image path to match the keys in the images map
                        val normalizedSrc = normalizeImagePath(src)
                        val imagePath = images[normalizedSrc] ?: images[src]
                        
                        if (imagePath != null) {
                            // Display image filename as placeholder with path info
                            val fileName = java.io.File(imagePath).name
                            builder.append("\n\n[Image: $fileName]")
                            if (alt.isNotEmpty()) {
                                builder.append(" ($alt)")
                            }
                            builder.append("\n\n")
                        } else if (alt.isNotEmpty()) {
                            // Show alt text if available
                            builder.append("\n\n[Image: $alt]\n\n")
                        } else if (src.isNotEmpty()) {
                            // Show source if no alt text - this helps debug path issues
                            builder.append("\n\n[Image: $src]\n\n")
                        }
                    }
                    else -> {
                        // Default case: just parse child elements
                        parseElement(node, builder, readerSettings, images)
                    }
                }
            }
        }
    }
}

/**
 * Creates an accessible text style based on reader settings and system preferences
 */
@Composable
private fun createAccessibleTextStyle(
    readerSettings: ReaderSettings,
    baseSize: Float = 1.0f
): TextStyle {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Calculate effective font size
    val effectiveFontSize = if (readerSettings.useSystemFontSize) {
        // Use system font scale
        val systemFontScale = configuration.fontScale
        (readerSettings.fontSize * baseSize * systemFontScale).sp
    } else {
        (readerSettings.fontSize * baseSize).sp
    }
    
    // Choose font family
    val fontFamily = when (readerSettings.fontFamily) {
        "OpenDyslexic" -> FontFamily.Serif // Fallback until OpenDyslexic is added
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }
    
    // Calculate letter spacing
    val letterSpacing = readerSettings.letterSpacing
    
    // Determine text color based on high contrast settings
    val textColor = if (readerSettings.highContrast) {
        when (readerSettings.readingMode) {
            ReadingMode.LIGHT, ReadingMode.SEPIA -> Color.Black
            ReadingMode.DARK, ReadingMode.BLACK -> Color.White
            ReadingMode.HIGH_CONTRAST_LIGHT -> Color.Black
            ReadingMode.HIGH_CONTRAST_DARK -> Color.White
            ReadingMode.HIGH_CONTRAST_YELLOW -> Color.Black
        }
    } else {
        readerSettings.readingMode.textColor
    }
    
    return TextStyle(
        fontSize = effectiveFontSize,
        lineHeight = (effectiveFontSize.value * readerSettings.lineHeight).sp,
        fontFamily = fontFamily,
        color = textColor,
        letterSpacing = letterSpacing.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.None
        )
    )
}

/**
 * Gets the background color for accessibility modes
 */
@Composable
private fun getAccessibleBackgroundColor(readerSettings: ReaderSettings): Color {
    return if (readerSettings.highContrast) {
        when (readerSettings.readingMode) {
            ReadingMode.LIGHT, ReadingMode.SEPIA -> Color.White
            ReadingMode.DARK, ReadingMode.BLACK -> Color.Black
            ReadingMode.HIGH_CONTRAST_LIGHT -> Color.White
            ReadingMode.HIGH_CONTRAST_DARK -> Color.Black
            ReadingMode.HIGH_CONTRAST_YELLOW -> Color(0xFFFFFF00)
        }
    } else {
        readerSettings.readingMode.backgroundColor
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    val theme = ThemeManager.lightTheme
    val sampleBook = Book(
        title = "Clockwork Planet",
        author = "Yuu Kamiya",
        coverColor = 0xFFFFD700,
        progress = 0.3f
    )
    
    MaterialTheme {
        ReaderScreen(
            book = sampleBook,
            theme = theme,
            onBackClick = { },
            onUpdateReadingPosition = { _, _, _, _ -> },
            onUpdateBookSettings = { _ -> },
            preferencesManager = TODO("Preview requires dependency injection"),
            libraryManager = TODO("Preview requires dependency injection")
        )
    }
}