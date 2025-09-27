package com.bsikar.helix.data.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class ReadingProgress(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val chapterIndex: Int,
    val chapterTitle: String? = null,
    val scrollPosition: Int = 0,
    val scrollItemIndex: Int = 0, // Exact item index in LazyColumn
    val scrollItemOffset: Int = 0, // Exact offset within the item
    val pageNumber: Int = 1,
    val totalPagesInChapter: Int = 1,
    val readingTimeSeconds: Long = 0, // Time spent reading this session
    val lastUpdated: Long = System.currentTimeMillis(),
    val isChapterCompleted: Boolean = false,
    val readingSpeedWpm: Float? = null, // Words per minute for this session
    val notes: String? = null // Optional reading notes for this position
) {
    /**
     * Calculate the overall progress percentage within the current chapter
     */
    fun getChapterProgress(): Float {
        if (totalPagesInChapter <= 0) return 0f
        return (pageNumber.toFloat() / totalPagesInChapter.toFloat()).coerceIn(0f, 1f)
    }
    
    /**
     * Get a human-readable description of the reading position
     */
    fun getPositionDescription(): String {
        val chapterName = chapterTitle ?: "Chapter ${chapterIndex + 1}"
        return if (totalPagesInChapter > 1) {
            "$chapterName - Page $pageNumber of $totalPagesInChapter"
        } else {
            chapterName
        }
    }
    
    /**
     * Get time ago text for when this progress was last updated
     */
    fun getTimeAgoText(): String {
        val now = System.currentTimeMillis()
        val diff = now - lastUpdated
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }
    
    /**
     * Get reading time in a human-readable format
     */
    fun getReadingTimeText(): String {
        if (readingTimeSeconds <= 0) return "0 min"
        
        val minutes = readingTimeSeconds / 60
        val hours = minutes / 60
        
        return when {
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h ${minutes % 60}m"
            else -> "${hours / 24}d ${hours % 24}h"
        }
    }
    
    /**
     * Check if this progress entry is recent (within the last hour)
     */
    fun isRecent(): Boolean {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        return lastUpdated > oneHourAgo
    }
    
    /**
     * Create a new progress entry with updated position
     */
    fun withUpdatedPosition(
        newChapterIndex: Int = chapterIndex,
        newScrollPosition: Int = scrollPosition,
        newScrollItemIndex: Int = scrollItemIndex,
        newScrollItemOffset: Int = scrollItemOffset,
        newPageNumber: Int = pageNumber,
        additionalReadingTime: Long = 0
    ): ReadingProgress {
        return copy(
            chapterIndex = newChapterIndex,
            scrollPosition = newScrollPosition,
            scrollItemIndex = newScrollItemIndex,
            scrollItemOffset = newScrollItemOffset,
            pageNumber = newPageNumber,
            readingTimeSeconds = readingTimeSeconds + additionalReadingTime,
            lastUpdated = System.currentTimeMillis(),
            isChapterCompleted = newPageNumber >= totalPagesInChapter
        )
    }
}