package com.bsikar.helix.data

import kotlinx.serialization.Serializable
import java.io.File

// Constants for progress calculations
private const val PERCENTAGE_MULTIPLIER = 100
private const val MIN_PERCENTAGE = 0
private const val MAX_PERCENTAGE = 100
private const val NEW_BOOK_ELEMENT_THRESHOLD = 1
private const val FINISHED_THRESHOLD = 0.95f
private const val NEAR_FINISHED_THRESHOLD = 0.9f

/**
 * Represents the user's reading progress for an EPUB file
 */
@Serializable
data class ReadingProgress(
    val epubPath: String,
    val epubFileName: String,
    val lastModified: Long, // EPUB file's last modified time for validation
    val fileSize: Long, // EPUB file size for validation

    // Current position
    val currentChapterIndex: Int = 0,
    val currentElementIndex: Int = 0, // Position within the chapter
    val scrollOffset: Int = 0, // Pixel scroll offset within the element

    // Reading statistics
    val totalChapters: Int,
    val lastReadTimestamp: Long = System.currentTimeMillis(),

    // Progress calculation
    val estimatedProgress: Float = 0f, // 0.0 to 1.0
    val wordsRead: Long = 0L,
    val totalWords: Long = 0L,

    // User preferences at time of reading
    val fontSize: Float = 16f,
    val lineHeight: Float = 1.5f,

    // Bookmarks within this EPUB
    val bookmarks: List<BookmarkEntry> = emptyList()
) {
    /**
     * Check if this progress is still valid for the given EPUB file
     */
    fun isValidFor(epubFile: File): Boolean {
        return epubFile.exists() &&
               epubFile.lastModified() == lastModified &&
               epubFile.length() == fileSize &&
               epubFile.absolutePath == epubPath
    }

    /**
     * Calculate reading progress as percentage
     */
    fun getProgressPercentage(): Int {
        return (estimatedProgress * PERCENTAGE_MULTIPLIER).toInt().coerceIn(MIN_PERCENTAGE, MAX_PERCENTAGE)
    }

    /**
     * Get a human-readable progress description
     */
    fun getProgressDescription(): String {
        val chapter = currentChapterIndex + 1
        return "Chapter $chapter of $totalChapters"
    }

    /**
     * Get a context-aware progress description based on current reading mode
     */
    fun getProgressDescription(chapters: List<Any>?, currentOnlyShowImages: Boolean): String {
        if (chapters == null) {
            return getProgressDescription()
        }

        val chapter = currentChapterIndex + 1

        // Calculate effective total chapters based on current reading mode
        val effectiveTotal = if (currentOnlyShowImages) {
            // Count chapters that have images
            @Suppress("UNCHECKED_CAST")
            val richChapters = chapters as? List<com.bsikar.helix.RichEpubChapter>
            richChapters?.count {
                it.elements.any { element ->
                element is com.bsikar.helix.ContentElement.Image
            }
            } ?: totalChapters
        } else {
            chapters.size
        }

        return "Chapter $chapter of $effectiveTotal"
    }

    /**
     * Check if this is a new book (minimal progress)
     */
    fun isNewBook(): Boolean {
        return currentChapterIndex == 0 &&
               currentElementIndex <= NEW_BOOK_ELEMENT_THRESHOLD
    }

    /**
     * Check if book is finished (near the end)
     */
    fun isFinished(): Boolean {
        return estimatedProgress >= FINISHED_THRESHOLD ||
               (currentChapterIndex >= totalChapters - 1 && estimatedProgress >= NEAR_FINISHED_THRESHOLD)
    }
}

/**
 * Represents a user bookmark within an EPUB
 */
@Serializable
data class BookmarkEntry(
    val chapterIndex: Int,
    val elementIndex: Int,
    val title: String,
    val previewText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
) {
    fun getDisplayTitle(): String {
        return if (title.isNotBlank()) title else "Chapter ${chapterIndex + 1}"
    }
}

/**
 * Reading session for tracking user activity
 */
data class ReadingSession(
    var totalScrollDistance: Int = 0,
    var chaptersVisited: MutableSet<Int> = mutableSetOf()
) {
    /**
     * Update scroll tracking
     */
    fun recordActivity(scrollDelta: Int = 0, chapterIndex: Int = -1) {
        totalScrollDistance += kotlin.math.abs(scrollDelta)
        if (chapterIndex >= 0) {
            chaptersVisited.add(chapterIndex)
        }
    }
}
