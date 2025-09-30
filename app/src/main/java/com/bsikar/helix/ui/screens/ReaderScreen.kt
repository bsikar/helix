package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.ReadingMode
import com.bsikar.helix.data.TextAlignment
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import com.bsikar.helix.ui.components.SearchUtils
import com.bsikar.helix.ui.components.ChapterNavigationSheet
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.data.model.ParsedEpub
import com.bsikar.helix.data.EpubParser
import com.bsikar.helix.viewmodels.ReaderViewModel
import org.jsoup.Jsoup
import coil.compose.AsyncImage
import com.bsikar.helix.ui.components.EpubImageComponent
import java.io.File
import kotlin.math.abs

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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
    
    // Reading progress tracking
    val currentReadingProgress by readerViewModel.currentReadingProgress.collectAsState()
    
    // Scroll state for LazyColumn
    val lazyListState = rememberLazyListState()
    var horizontalDragAmount by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { density.run { 72.dp.toPx() } }
    
    // Track if we've already restored scroll position for this book
    var hasRestoredScrollPosition by remember { mutableStateOf(false) }
    
    // Debug when hasRestoredScrollPosition changes
    LaunchedEffect(hasRestoredScrollPosition) {
        android.util.Log.d("ReaderProgress", "hasRestoredScrollPosition changed to: $hasRestoredScrollPosition")
    }
    
    // Calculate real-time progress using derivedStateOf for immediate updates
    val currentScrollProgress by remember {
        derivedStateOf {
            android.util.Log.d("ReaderProgress", "derivedStateOf TRIGGERED - Starting calculation")
            android.util.Log.d("ReaderProgress", "Book: ${book.id}, isImported: ${book.isImported}")
            android.util.Log.d("ReaderProgress", "ParsedEpub: ${parsedEpub != null}, chapters: ${parsedEpub?.chapters?.size}")
            android.util.Log.d("ReaderProgress", "CurrentContent size: ${currentContent.size}")
            android.util.Log.d("ReaderProgress", "HasRestoredScrollPosition: $hasRestoredScrollPosition")
            android.util.Log.d("ReaderProgress", "LazyListState - firstVisibleItemIndex: ${lazyListState.firstVisibleItemIndex}")
            android.util.Log.d("ReaderProgress", "LazyListState - firstVisibleItemScrollOffset: ${lazyListState.firstVisibleItemScrollOffset}")
            
            val progress = if (book.isImported && parsedEpub != null && currentContent.isNotEmpty() && hasRestoredScrollPosition) {
                android.util.Log.d("ReaderProgress", "Conditions met - calculating progress")
                val totalChapters = parsedEpub!!.chapters.size
                if (totalChapters > 0) {
                    val chapterProgress = currentChapterIndex.toFloat() / totalChapters.toFloat()
                    
                    // Calculate progress within current chapter based on scroll position and layoutInfo
                    val layoutInfo = lazyListState.layoutInfo
                    val itemProgress = if (layoutInfo.totalItemsCount > 0) {
                        val visibleItemInfo = layoutInfo.visibleItemsInfo.firstOrNull()
                        if (visibleItemInfo != null) {
                            // Calculate the scroll progress through the chapter
                            // Using the item height and scroll offset for more accurate progress
                            val itemHeight = visibleItemInfo.size
                            val totalScrollableHeight = itemHeight - layoutInfo.viewportSize.height
                            
                            if (totalScrollableHeight > 0) {
                                // Calculate how far we've scrolled through this item
                                val scrollProgress = (-visibleItemInfo.offset).toFloat() / totalScrollableHeight.toFloat()
                                // This is progress through the chapter, scale it relative to total chapters
                                val chapterContribution = scrollProgress.coerceIn(0f, 1f) / totalChapters.toFloat()
                                
                                android.util.Log.d("ReaderProgress", "SCROLL CALC: itemHeight=$itemHeight, viewport=${layoutInfo.viewportSize.height}, scrollable=$totalScrollableHeight")
                                android.util.Log.d("ReaderProgress", "SCROLL CALC: offset=${visibleItemInfo.offset}, scrollProgress=$scrollProgress, contribution=$chapterContribution")
                                
                                chapterContribution
                            } else {
                                // Item fits entirely in viewport, no scroll progress
                                0f
                            }
                        } else {
                            0f
                        }
                    } else 0f
                    
                    val newProgress = (chapterProgress + itemProgress).coerceIn(0f, 1f)
                    
                    android.util.Log.d("ReaderProgress", "CALCULATED: Chapter: $currentChapterIndex/$totalChapters ($chapterProgress), IntraChapter: $itemProgress, TOTAL: $newProgress")
                    
                    newProgress
                } else {
                    android.util.Log.d("ReaderProgress", "No chapters found, using book progress: ${book.progress}")
                    book.progress
                }
            } else {
                android.util.Log.d("ReaderProgress", "Conditions NOT met, using book progress: ${book.progress}")
                book.progress
            }
            android.util.Log.d("ReaderProgress", "derivedStateOf RESULT: $progress")
            progress
        }
    }
    
    // Track scroll position changes (keep existing functionality)
    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset, hasRestoredScrollPosition) {
        val currentItem = lazyListState.firstVisibleItemIndex
        val itemOffset = lazyListState.firstVisibleItemScrollOffset
        
        android.util.Log.d("ReaderScroll", "LaunchedEffect TRIGGERED - item: $currentItem, offset: $itemOffset, hasRestored: $hasRestoredScrollPosition")
        
        // Only track scroll changes after we've restored the initial position
        if (hasRestoredScrollPosition) {
            android.util.Log.d("ReaderScroll", "Saving scroll: item=$currentItem, offset=$itemOffset")
            readerViewModel.updateScrollPosition(currentItem, itemOffset)
        } else {
            android.util.Log.d("ReaderScroll", "NOT saving scroll - position not restored yet")
        }
    }
    
    // Update book progress in library when currentScrollProgress changes (debounced)
    LaunchedEffect(currentScrollProgress) {
        android.util.Log.d("ReaderProgressUpdate", "LaunchedEffect triggered with progress: $currentScrollProgress")
        android.util.Log.d("ReaderProgressUpdate", "hasRestored: $hasRestoredScrollPosition, isImported: ${book.isImported}")
        
        if (hasRestoredScrollPosition && book.isImported) {
            android.util.Log.d("ReaderProgressUpdate", "Conditions met - will update after delay")
            // Add a small delay to debounce frequent updates
            kotlinx.coroutines.delay(200)
            
            // Calculate current page for chapter-based navigation
            val newCurrentPage = currentChapterIndex + 1
            val scrollPosition = try {
                lazyListState.firstVisibleItemIndex * 1000 + lazyListState.firstVisibleItemScrollOffset
            } catch (e: Exception) {
                0
            }
            
            // Update reading position with the new progress
            onUpdateReadingPosition(book.id, newCurrentPage, currentChapterIndex + 1, scrollPosition)
            
            // Also directly update the book's progress for real-time updates in library/recents
            libraryManager.updateBookProgressDirect(book.id, currentScrollProgress)
            
            android.util.Log.d("ReaderProgressUpdate", "UPDATED book progress to: $currentScrollProgress (scroll pos: $scrollPosition)")
        } else {
            android.util.Log.d("ReaderProgressUpdate", "Conditions NOT met - skipping update")
        }
    }
    
    // Save progress when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            // Ensure we save the current position when the screen is disposed
            if (hasRestoredScrollPosition) {
                try {
                    val currentItem = lazyListState.firstVisibleItemIndex
                    val itemOffset = lazyListState.firstVisibleItemScrollOffset
                    android.util.Log.d("ReaderScreen", "Disposing - saving final scroll position: item=$currentItem, offset=$itemOffset")
                    readerViewModel.updateScrollPosition(currentItem, itemOffset)
                } catch (e: Exception) {
                    android.util.Log.w("ReaderScreen", "Failed to save scroll position on dispose", e)
                }
                readerViewModel.endReadingSession()
            }
        }
    }
    
    // Reset restoration flag when book changes
    LaunchedEffect(book.id) {
        hasRestoredScrollPosition = false
    }
    
    // Restore scroll position when reading progress is loaded (only once per book)
    LaunchedEffect(currentReadingProgress, hasRestoredScrollPosition, currentContent.size, currentChapterIndex) {
        if (!hasRestoredScrollPosition && currentContent.isNotEmpty()) {
            val progress = currentReadingProgress
            if (progress != null && progress.chapterIndex == currentChapterIndex) {
                // Only restore if we're on the same chapter as the saved progress
                val targetItem = progress.scrollItemIndex
                val targetOffset = progress.scrollItemOffset
                
                // Ensure target item is within bounds
                val maxItem = (currentContent.size - 1).coerceAtLeast(0)
                val safeTargetItem = targetItem.coerceIn(0, maxItem)
                val safeTargetOffset = targetOffset.coerceAtLeast(0)
                
                android.util.Log.d("ReaderScreen", "Restoring scroll: item=$safeTargetItem/$maxItem, offset=$safeTargetOffset")
                
                scope.launch {
                    try {
                        // Add a small delay to ensure LazyColumn is fully composed
                        kotlinx.coroutines.delay(200)
                        lazyListState.animateScrollToItem(safeTargetItem, safeTargetOffset)
                        hasRestoredScrollPosition = true
                        android.util.Log.d("ReaderScreen", "Successfully restored scroll position")
                    } catch (e: Exception) {
                        // Fallback to top if scroll position is invalid
                        android.util.Log.w("ReaderScreen", "Failed to restore scroll position, falling back to top", e)
                        try {
                            lazyListState.scrollToItem(0, 0)
                        } catch (fallbackException: Exception) {
                            android.util.Log.w("ReaderScreen", "Even fallback scroll failed", fallbackException)
                        }
                        hasRestoredScrollPosition = true
                    }
                }
            } else {
                // Different chapter or no progress, start from top
                android.util.Log.d("ReaderScreen", "Starting from top - different chapter or no progress")
                hasRestoredScrollPosition = true
            }
        } else if (!hasRestoredScrollPosition && currentContent.isEmpty()) {
            // If no content yet, wait for it to load
            android.util.Log.d("ReaderScreen", "Waiting for content to load...")
        } else if (!hasRestoredScrollPosition) {
            // Fallback case
            hasRestoredScrollPosition = true
        }
    }
    
    // Use dynamic chapter count for imported EPUBs - ensure non-zero value during loading
    val totalPages = remember(parsedEpub, book.totalPages) {
        if (book.isImported && parsedEpub != null) {
            maxOf(1, parsedEpub!!.chapters.size)
        } else {
            maxOf(1, book.totalPages)
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
        
        // Auto-start reading if book is in "Plan to Read" status
        if (book.readingStatus == ReadingStatus.PLAN_TO_READ) {
            onUpdateReadingPosition(book.id, maxOf(1, book.currentPage), maxOf(1, book.currentChapter), book.scrollPosition)
        }
    }
    
    // Save reading position when page or chapter changes
    LaunchedEffect(currentPage, currentChapterIndex) {
        // Get current scroll position from LazyListState
        val currentScrollPosition = try {
            lazyListState.firstVisibleItemIndex * 1000 + lazyListState.firstVisibleItemScrollOffset
        } catch (e: Exception) {
            0 // Fallback to 0 if LazyListState is not initialized
        }
        onUpdateReadingPosition(book.id, currentPage, currentChapterIndex + 1, currentScrollPosition)
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
                    if (book.isImported) {
                        IconButton(onClick = { showChapterDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Chapters",
                                tint = theme.secondaryTextColor
                            )
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
                progress = currentScrollProgress, // Use calculated real-time progress
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
                    android.util.Log.d("ReaderProgressBar", "========== PROGRESS BAR CALLED ==========")
                    android.util.Log.d("ReaderProgressBar", "Progress bar showing: $currentScrollProgress")
                    android.util.Log.d("ReaderProgressBar", "Book progress: ${book.progress}")
                    android.util.Log.d("ReaderProgressBar", "==========================================")
                    currentScrollProgress 
                }, // Use calculated real-time progress
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
                
                // Only show LazyColumn if we have content and it's stable
                if (stableContent.isNotEmpty()) {
                    android.util.Log.d("ReaderContent", "Rendering LazyColumn with ${stableContent.size} items")
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(book.id, parsedEpub, currentPage, currentChapterIndex) {
                                detectHorizontalDragGestures(
                                    onDragStart = { horizontalDragAmount = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        horizontalDragAmount += dragAmount
                                    },
                                    onDragEnd = {
                                        if (abs(horizontalDragAmount) > swipeThresholdPx) {
                                            if (horizontalDragAmount < 0f) {
                                                if (book.isImported && parsedEpub != null) {
                                                    readerViewModel.nextChapter(context)
                                                } else if (currentPage < totalPages) {
                                                    currentPage++
                                                }
                                            } else {
                                                if (book.isImported && parsedEpub != null) {
                                                    readerViewModel.previousChapter(context)
                                                } else if (currentPage > 1) {
                                                    currentPage--
                                                }
                                            }
                                        }
                                        horizontalDragAmount = 0f
                                    },
                                    onDragCancel = { horizontalDragAmount = 0f }
                                )
                            },
                        contentPadding = PaddingValues(
                            horizontal = readerSettings.marginHorizontal.dp,
                            vertical = readerSettings.marginVertical.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = stableContent.size,
                            key = { index -> "${currentChapterIndex}_content_$index" }
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
                } else {
                    // Show loading or empty state when content is empty
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingChapter) {
                            CircularProgressIndicator(
                                color = theme.accentColor
                            )
                        } else {
                            Text(
                                text = "No content available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.secondaryTextColor
                            )
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
            onChapterSelect = { selectedChapter ->
                // Find the index of the selected chapter
                val chapterIndex = chapters.indexOfFirst { it.id == selectedChapter.id }
                if (chapterIndex != -1) {
                    readerViewModel.navigateToChapter(chapterIndex, context)
                    currentPage = 1 // Reset to first page of chapter
                }
                showChapterDialog = false
            },
            onDismiss = { showChapterDialog = false }
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$currentPage of $totalPages",
                    fontSize = 14.sp,
                    color = theme.primaryTextColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format("%.2f", progress * 100)}% complete",
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
    
    val indexedChapters = remember(chapters) {
        chapters.mapIndexed { index, title -> index to title }
    }

    // Filter chapters based on search query using fuzzy search
    val filteredChapters = remember(indexedChapters, searchQuery) {
        if (searchQuery.isBlank()) {
            indexedChapters
        } else {
            SearchUtils.fuzzySearch(
                items = indexedChapters,
                query = searchQuery,
                getText = { it.second },
                getSecondaryText = { "${it.first + 1}" },
                threshold = 0.3
            ).map { it.item }
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
            ReadingMode.LIGHT, ReadingMode.SEPIA, ReadingMode.SYSTEM -> Color.Black
            ReadingMode.DARK, ReadingMode.BLACK -> Color.White
            ReadingMode.HIGH_CONTRAST_LIGHT, ReadingMode.HIGH_CONTRAST_YELLOW -> Color.Black
            ReadingMode.HIGH_CONTRAST_DARK -> Color.White
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
            ReadingMode.LIGHT, ReadingMode.SEPIA, ReadingMode.SYSTEM -> Color.White
            ReadingMode.DARK, ReadingMode.BLACK -> Color.Black
            ReadingMode.HIGH_CONTRAST_LIGHT -> Color.White
            ReadingMode.HIGH_CONTRAST_DARK -> Color.Black
            ReadingMode.HIGH_CONTRAST_YELLOW -> Color.Yellow
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