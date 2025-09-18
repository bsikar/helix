@file:Suppress("TooGenericExceptionCaught")

package com.bsikar.helix

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bsikar.helix.data.ReadingProgress
import com.bsikar.helix.data.ReadingProgressRepository
import com.bsikar.helix.data.ReadingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File

/**
 * Composable that tracks reading progress and automatically saves user position
 */
@Composable
@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
fun ReadingProgressTracker(
    epubFile: File,
    chapters: List<RichEpubChapter>,
    scrollState: LazyListState,
    repository: ReadingProgressRepository,
    onlyShowImages: Boolean = false,
    onProgressLoaded: (ReadingProgress?) -> Unit = {}
) {
    var currentSession by remember { mutableStateOf<ReadingSession?>(null) }
    var lastSavedPosition by remember { mutableStateOf<ProgressPosition?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Load initial progress
    LaunchedEffect(epubFile) {
        val progress = repository.loadProgress(epubFile)
        if (progress == null) {
            // Create initial progress for new book
            val initialProgress = repository.createInitialProgress(epubFile, chapters.size)
            repository.saveProgress(initialProgress)
            onProgressLoaded(initialProgress)
        } else {
            onProgressLoaded(progress)
        }

        // Start reading session
        currentSession = ReadingSession()
    }

    // Track scroll position changes
    LaunchedEffect(scrollState) {
        snapshotFlow {
            ProgressPosition(
                firstVisibleItemIndex = scrollState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = scrollState.firstVisibleItemScrollOffset,
                totalItemsCount = scrollState.layoutInfo.totalItemsCount
            )
        }
        .distinctUntilChanged()
        .filter { it.totalItemsCount > 0 } // Only track when content is loaded
        .collect { position ->
            val currentChapter = calculateCurrentChapter(position, chapters, onlyShowImages)
            val elementIndex = calculateElementIndex(position, chapters, currentChapter, onlyShowImages)
            val progress = calculateReadingProgress(position, chapters, onlyShowImages)

            // Update session activity
            currentSession?.recordActivity(
                scrollDelta = kotlin.math.abs(
                    (lastSavedPosition?.firstVisibleItemScrollOffset ?: 0) -
                    position.firstVisibleItemScrollOffset
                ),
                chapterIndex = currentChapter
            )

            // Save progress periodically (not on every scroll)
            if (shouldSaveProgress(lastSavedPosition, position)) {
                repository.updatePosition(
                    epubFile = epubFile,
                    chapterIndex = currentChapter,
                    elementIndex = elementIndex,
                    scrollOffset = position.firstVisibleItemScrollOffset,
                    estimatedProgress = progress
                )
                lastSavedPosition = position
            }
        }
    }

    // Handle app lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Save progress when app goes to background
                    saveCurrentProgress(
                        epubFile = epubFile,
                        scrollState = scrollState,
                        chapters = chapters,
                        repository = repository,
                        onlyShowImages = onlyShowImages
                    )
                }
                Lifecycle.Event.ON_STOP -> {
                    // End reading session
                    currentSession = null
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Restart reading session if needed
                    if (currentSession == null) {
                        currentSession = ReadingSession()
                    }
                }
                else -> { /* no-op */ }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            // Final save when composable is disposed
            saveCurrentProgress(
                epubFile = epubFile,
                scrollState = scrollState,
                chapters = chapters,
                repository = repository,
                onlyShowImages = onlyShowImages
            )
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Restore scroll position from reading progress
 */
@Composable
@Suppress("FunctionNaming")
fun RestoreReadingPosition(
    scrollState: LazyListState,
    progress: ReadingProgress?,
    chapters: List<RichEpubChapter>,
    onlyShowImages: Boolean = false
) {
    LaunchedEffect(progress, chapters) {
        if (progress != null && chapters.isNotEmpty()) {
            try {
                val targetIndex = calculateAbsoluteItemIndex(
                    chapterIndex = progress.currentChapterIndex,
                    elementIndex = progress.currentElementIndex,
                    chapters = chapters,
                    onlyShowImages = onlyShowImages
                )

                if (targetIndex < scrollState.layoutInfo.totalItemsCount) {
                    scrollState.scrollToItem(
                        index = targetIndex,
                        scrollOffset = progress.scrollOffset
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            }
        }
    }
}

private data class ProgressPosition(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val totalItemsCount: Int
)

private fun calculateCurrentChapter(
    position: ProgressPosition,
    chapters: List<RichEpubChapter>,
    onlyShowImages: Boolean = false
): Int {
    var itemCount = 0
    for ((chapterIndex, chapter) in chapters.withIndex()) {
        val chapterSize = if (onlyShowImages) {
            chapter.elements.count { it is ContentElement.Image }
        } else {
            chapter.elements.size
        }
        if (position.firstVisibleItemIndex < itemCount + chapterSize) {
            return chapterIndex
        }
        itemCount += chapterSize
    }
    return (chapters.size - 1).coerceAtLeast(0)
}

private fun calculateElementIndex(
    position: ProgressPosition,
    chapters: List<RichEpubChapter>,
    chapterIndex: Int,
    onlyShowImages: Boolean = false
): Int {
    var itemCount = 0
    for (i in 0 until chapterIndex) {
        itemCount += if (onlyShowImages) {
            chapters[i].elements.count { it is ContentElement.Image }
        } else {
            chapters[i].elements.size
        }
    }
    return (position.firstVisibleItemIndex - itemCount).coerceAtLeast(0)
}

@Suppress("ReturnCount")
private fun calculateReadingProgress(
    position: ProgressPosition,
    chapters: List<RichEpubChapter>,
    onlyShowImages: Boolean = false
): Float {
    if (position.totalItemsCount == 0) return 0f

    val totalElements = if (onlyShowImages) {
        chapters.sumOf { chapter ->
            chapter.elements.count { it is ContentElement.Image }
        }
    } else {
        chapters.sumOf { it.elements.size }
    }
    if (totalElements == 0) return 0f

    return (position.firstVisibleItemIndex.toFloat() / totalElements).coerceIn(0f, 1f)
}

private fun calculateAbsoluteItemIndex(
    chapterIndex: Int,
    elementIndex: Int,
    chapters: List<RichEpubChapter>,
    onlyShowImages: Boolean = false
): Int {
    var itemCount = 0
    for (i in 0 until chapterIndex.coerceAtMost(chapters.size - 1)) {
        itemCount += if (onlyShowImages) {
            chapters[i].elements.count { it is ContentElement.Image }
        } else {
            chapters[i].elements.size
        }
    }
    val maxElementIndex = if (chapterIndex < chapters.size) {
        if (onlyShowImages) {
            chapters[chapterIndex].elements.count { it is ContentElement.Image } - 1
        } else {
            chapters[chapterIndex].elements.size - 1
        }
    } else {
        0
    }
    return itemCount + elementIndex.coerceAtMost(maxElementIndex)
}

private fun shouldSaveProgress(
    lastSaved: ProgressPosition?,
    current: ProgressPosition
): Boolean {
    if (lastSaved == null) return true

    // Save if user scrolled significantly or changed items
    val scrollDelta = kotlin.math.abs(lastSaved.firstVisibleItemScrollOffset - current.firstVisibleItemScrollOffset)
    val itemDelta = kotlin.math.abs(lastSaved.firstVisibleItemIndex - current.firstVisibleItemIndex)

    return scrollDelta > SCROLL_SAVE_THRESHOLD || itemDelta > ITEM_SAVE_THRESHOLD
}

@Suppress("LongParameterList")
private fun saveCurrentProgress(
    epubFile: File,
    scrollState: LazyListState,
    chapters: List<RichEpubChapter>,
    repository: ReadingProgressRepository,
    onlyShowImages: Boolean = false
) {
    try {
        val position = ProgressPosition(
            firstVisibleItemIndex = scrollState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = scrollState.firstVisibleItemScrollOffset,
            totalItemsCount = scrollState.layoutInfo.totalItemsCount
        )

        val currentChapter = calculateCurrentChapter(position, chapters, onlyShowImages)
        val elementIndex = calculateElementIndex(position, chapters, currentChapter, onlyShowImages)
        val progress = calculateReadingProgress(position, chapters, onlyShowImages)

        CoroutineScope(Dispatchers.IO).launch {
            repository.updatePosition(
                epubFile = epubFile,
                chapterIndex = currentChapter,
                elementIndex = elementIndex,
                scrollOffset = position.firstVisibleItemScrollOffset,
                estimatedProgress = progress
            )
        }
    } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
    }
}

private const val SCROLL_SAVE_THRESHOLD = 200 // pixels
private const val ITEM_SAVE_THRESHOLD = 1 // elements
