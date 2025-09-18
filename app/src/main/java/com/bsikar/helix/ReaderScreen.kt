package com.bsikar.helix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bsikar.helix.data.ReadingProgress
import com.bsikar.helix.data.ReadingProgressRepository
import com.bsikar.helix.data.UserPreferences
import java.io.File
import java.net.URLDecoder
import kotlinx.coroutines.launch

private const val PRELOAD_CHAPTERS = 3
private const val CHAPTER_PRELOAD_THRESHOLD = 10
private const val SCROLL_JUMP_THRESHOLD = 5
private const val TEXT_KEY_LENGTH = 50
private const val PERCENTAGE_MULTIPLIER = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod")
fun ReaderScreen(bookPath: String, navController: NavController, userPreferences: UserPreferences) {
    val fontSize by userPreferences.fontSize.collectAsState()
    val lineHeight by userPreferences.lineHeight.collectAsState()
    val onlyShowImages by userPreferences.onlyShowImages.collectAsState()

    val context = LocalContext.current
    val progressRepository = remember { ReadingProgressRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val richEpubContent = remember { mutableStateOf<RichEpubContent?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val bookTitle = remember { mutableStateOf("") }
    val epubFile = remember { mutableStateOf<File?>(null) }
    val scrollState = rememberLazyListState()
    var readingProgress by remember { mutableStateOf<ReadingProgress?>(null) }
    var currentChapterIndex by remember { mutableStateOf(0) }

    var scrollStabilizationEnabled by remember { mutableStateOf(true) }
    var lastScrollPosition by remember { mutableStateOf(0) }

    val currentChapterInfo by remember {
        derivedStateOf {
            richEpubContent.value?.let { content ->
                var itemCount = 0
                var calculatedChapterIndex = 0
                for ((chapterIndex, chapter) in content.chapters.withIndex()) {
                    val chapterSize = if (onlyShowImages) {
                        chapter.elements.count { it is ContentElement.Image }
                    } else {
                        chapter.elements.size
                    }
                    if (scrollState.firstVisibleItemIndex < itemCount + chapterSize) {
                        calculatedChapterIndex = chapterIndex
                        return@derivedStateOf Triple(chapterIndex, chapter.title, content.chapters.size)
                    }
                    itemCount += chapterSize
                }
                calculatedChapterIndex = content.chapters.size - 1
                Triple(calculatedChapterIndex, content.chapters.lastOrNull()?.title ?: "", content.chapters.size)
            } ?: Triple(0, "", 0)
        }
    }

    // Update currentChapterIndex and trigger preloading based on scroll position
    LaunchedEffect(currentChapterInfo, richEpubContent.value) {
        val (chapterIndex, _, _) = currentChapterInfo
        currentChapterIndex = chapterIndex

        // Trigger preloading when user gets close to next chapter
        richEpubContent.value?.let { content ->
            var itemCount = 0
            for (i in 0 until chapterIndex) {
                itemCount += if (onlyShowImages) {
                    content.chapters[i].elements.count { it is ContentElement.Image }
                } else {
                    content.chapters[i].elements.size
                }
            }
            val currentChapterSize = if (onlyShowImages) {
                content.chapters[chapterIndex].elements.count { it is ContentElement.Image }
            } else {
                content.chapters[chapterIndex].elements.size
            }
            val elementsFromChapterEnd = (itemCount + currentChapterSize) - scrollState.firstVisibleItemIndex

            if (elementsFromChapterEnd <= CHAPTER_PRELOAD_THRESHOLD &&
                chapterIndex < content.chapters.size - 1 &&
                epubFile.value != null
            ) {
                val preloader = ImagePreloader.getInstance()
                preloader.preloadUpcomingImages(
                    epubFile.value!!,
                    chapterIndex,
                    content.chapters,
                    PRELOAD_CHAPTERS + 1
                )
            }
        }
    }

    LaunchedEffect(bookPath) {
        try {
            val decodedPath = URLDecoder.decode(bookPath, "UTF-8")
            val file = File(decodedPath)
            bookTitle.value = file.name.removeSuffix(".epub")
            epubFile.value = file

            if (!file.exists()) {
                richEpubContent.value = RichEpubContent(
                    metadata = "Error: File does not exist at path: $decodedPath",
                    chapters = listOf(
                        RichEpubChapter(
                        title = "Error",
                        elements = listOf(ContentElement.TextParagraph("File not found"))
                    )
                    )
                )
                isLoading.value = false
                return@LaunchedEffect
            }

            val parser = EpubParser()
            val content = parser.parseRichContent(file)

            richEpubContent.value = content
            isLoading.value = false

            // Start aggressive preloading images in the background
            if (content.chapters.isNotEmpty()) {
                val preloader = ImagePreloader.getInstance()
                // First preload upcoming chapters
                preloader.preloadUpcomingImages(file, 0, content.chapters, PRELOAD_CHAPTERS)
                // Then start full preload in background (low priority)
                preloader.preloadAllImages(file)
            }
        } catch (e: java.io.IOException) {
            richEpubContent.value = RichEpubContent(
                metadata = "Error parsing EPUB: ${e.message}",
                chapters = listOf(
                    RichEpubChapter(
                    title = "Error",
                    elements = listOf(ContentElement.TextParagraph("Path: $bookPath"))
                )
                )
            )
            isLoading.value = false
        }
    }

    // Chapter navigation functions
    val navigateToChapter: (Int) -> Unit = { targetChapterIndex ->
        richEpubContent.value?.let { content ->
            if (targetChapterIndex in 0 until content.chapters.size) {
                var itemCount = 0
                for (i in 0 until targetChapterIndex) {
                    itemCount += content.chapters[i].elements.size
                }
                coroutineScope.launch {
                    scrollState.animateScrollToItem(itemCount)
                }
            }
        }
    }

    val navigateToPrevChapter: () -> Unit = {
        if (currentChapterIndex > 0) {
            navigateToChapter(currentChapterIndex - 1)
        }
    }

    val navigateToNextChapter: () -> Unit = {
        richEpubContent.value?.let { content ->
            if (currentChapterIndex < content.chapters.size - 1) {
                navigateToChapter(currentChapterIndex + 1)
            }
        }
    }

    // Progress tracking and restoration
    epubFile.value?.let { file ->
        richEpubContent.value?.let { content ->
            ReadingProgressTracker(
                epubFile = file,
                chapters = content.chapters,
                scrollState = scrollState,
                repository = progressRepository,
                onlyShowImages = onlyShowImages,
                onProgressLoaded = { progress ->
                    readingProgress = progress
                }
            )

            RestoreReadingPosition(
                scrollState = scrollState,
                progress = readingProgress,
                chapters = content.chapters,
                onlyShowImages = onlyShowImages
            )
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            TopAppBar(
                title = {
                    if (!onlyShowImages) {
                        Text(
                            text = bookTitle.value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            if (!isLoading.value && richEpubContent.value != null) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(56.dp) // Make it more compact
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Chapter Button
                        IconButton(
                            onClick = navigateToPrevChapter,
                            enabled = currentChapterIndex > 0,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Chapter",
                                tint = if (currentChapterIndex > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                        }

                        // Progress Information
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val (chapterIndex, chapterTitle, totalChapters) = currentChapterInfo

                            // Show current scroll position for chapter
                            Text(
                                text = "Chapter ${chapterIndex + 1} of $totalChapters",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Calculate current reading percentage based on current position
                            val currentProgress = if (totalChapters > 0) {
                                ((chapterIndex.toFloat() / totalChapters.toFloat()) * PERCENTAGE_MULTIPLIER).toInt()
                            } else 0

                            Text(
                                text = "$currentProgress% complete",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        // Next Chapter Button
                        IconButton(
                            onClick = navigateToNextChapter,
                            enabled = richEpubContent.value?.let {
                                currentChapterIndex < it.chapters.size - 1
                            } ?: false,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Chapter",
                                tint = if (richEpubContent.value?.let {
                                    currentChapterIndex < it.chapters.size - 1
                                } == true
                                ) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading.value) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 64.dp)
                    )
                    Text(
                        text = "Loading book content...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                richEpubContent.value?.let { content ->

                    val allElements = remember(content, onlyShowImages) {
                        content.chapters.flatMap { chapter ->
                            if (onlyShowImages) {
                                chapter.elements.filter { it is ContentElement.Image }
                            } else {
                                chapter.elements
                            }
                        }
                    }

                    // Monitor scroll changes to detect when stabilization is needed
                    LaunchedEffect(scrollState.firstVisibleItemIndex) {
                        // Check for unexpected large jumps in scroll position
                        val currentPosition = scrollState.firstVisibleItemIndex
                        if (lastScrollPosition > 0 && scrollStabilizationEnabled) {
                            val scrollDelta = kotlin.math.abs(currentPosition - lastScrollPosition)
                            // If there's a large unexpected jump (likely due to image loading)
                            if (scrollDelta > SCROLL_JUMP_THRESHOLD && !scrollState.isScrollInProgress) {
                                // Attempt to restore previous position
                                try {
                                    scrollState.animateScrollToItem(lastScrollPosition)
                                } catch (ignored: IllegalArgumentException) {
                                }
                            }
                        }
                        // Only update last position if user is actively scrolling
                        if (scrollState.isScrollInProgress) {
                            lastScrollPosition = currentPosition
                        }
                    }

                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(
                            items = allElements,
                            key = { element ->
                                when (element) {
                                    is ContentElement.Image -> "img_${element.src.hashCode()}"
                                    is ContentElement.TextParagraph ->
                                        "text_${element.text.take(TEXT_KEY_LENGTH).hashCode()}"
                                    is ContentElement.Heading -> "heading_${element.text.hashCode()}_${element.level}"
                                    is ContentElement.Quote -> "quote_${element.text.hashCode()}"
                                    is ContentElement.List -> "list_${element.items.hashCode()}"
                                    is ContentElement.Divider -> "divider_${element.hashCode()}"
                                    else -> "element_${element.hashCode()}"
                                }
                            },
                            contentType = { element ->
                                when (element) {
                                    is ContentElement.Image -> "image"
                                    is ContentElement.TextParagraph -> "text"
                                    is ContentElement.Heading -> "heading"
                                    is ContentElement.Quote -> "quote"
                                    is ContentElement.List -> "list"
                                    is ContentElement.Divider -> "divider"
                                    else -> "unknown"
                                }
                            }
                        ) { element ->
                            renderContentElement(
                                element = element,
                                config = RenderConfig(
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    epubFile = epubFile.value,
                                    onlyShowImages = onlyShowImages
                                )
                            )
                        }

                        item {
                            Box(modifier = Modifier.padding(bottom = 32.dp))
                        }
                    }
                }
            }
        }
    }
}
