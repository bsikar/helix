package com.bsikar.helix.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.bsikar.helix.data.source.entities.BookEntity
import kotlinx.serialization.Serializable

/**
 * Entity representing a reading session for analytics tracking
 */
@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"])
    ]
)
@Serializable
data class ReadingSession(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val startTime: Long, // Timestamp when reading session started
    val endTime: Long? = null, // Timestamp when reading session ended (null if ongoing)
    val startChapter: Int,
    val endChapter: Int? = null,
    val startPage: Int,
    val endPage: Int? = null,
    val startScrollPosition: Int = 0,
    val endScrollPosition: Int? = null,
    val wordsRead: Int = 0, // Estimated words read during this session
    val charactersRead: Int = 0, // Characters read during this session
    val sessionType: ReadingSessionType = ReadingSessionType.NORMAL,
    val deviceType: String = "mobile", // Device type for analytics
    val appVersion: String = "1.0.0" // App version for tracking
) {
    /**
     * Calculate session duration in milliseconds
     */
    val durationMs: Long
        get() = if (endTime != null) endTime - startTime else 0L

    /**
     * Calculate session duration in minutes
     */
    val durationMinutes: Double
        get() = durationMs / (1000.0 * 60.0)

    /**
     * Calculate reading speed in words per minute
     */
    val wordsPerMinute: Double
        get() = if (durationMinutes > 0) wordsRead / durationMinutes else 0.0

    /**
     * Calculate reading speed in characters per minute
     */
    val charactersPerMinute: Double
        get() = if (durationMinutes > 0) charactersRead / durationMinutes else 0.0

    /**
     * Check if this session is currently active (ongoing)
     */
    val isActive: Boolean
        get() = endTime == null

    /**
     * Calculate pages read during this session
     */
    val pagesRead: Int
        get() = if (endPage != null) (endPage - startPage).coerceAtLeast(0) else 0

    /**
     * Calculate chapters read during this session
     */
    val chaptersRead: Int
        get() = if (endChapter != null) (endChapter - startChapter).coerceAtLeast(0) else 0
}

/**
 * Types of reading sessions for categorization
 */
@Serializable
enum class ReadingSessionType {
    NORMAL,          // Regular reading session
    SPEED_READING,   // Fast reading session
    REVIEW,          // Re-reading/reviewing content
    SKIMMING,        // Quick browsing/skimming
    STUDY,           // Academic/study reading
    LEISURE          // Casual reading
}

/**
 * Data class for reading analytics aggregation
 */
@Serializable
data class ReadingAnalytics(
    val bookId: String,
    val bookTitle: String,
    val totalReadingTime: Long, // Total time spent reading this book (ms)
    val totalSessions: Int,
    val averageSessionLength: Double, // Average session length in minutes
    val totalWordsRead: Int,
    val totalCharactersRead: Int,
    val averageReadingSpeed: Double, // Words per minute
    val totalPagesRead: Int,
    val totalChaptersRead: Int,
    val firstReadingDate: Long,
    val lastReadingDate: Long,
    val completionPercentage: Float,
    val readingStreak: Int, // Days of consecutive reading
    val longestSession: Long, // Longest session duration in ms
    val shortestSession: Long, // Shortest session duration in ms
    val favoriteReadingTime: String, // Most common reading time (morning, afternoon, evening, night)
    val readingConsistency: Double, // Measure of how regularly the user reads (0.0 - 1.0)
    val estimatedTimeToCompletion: Long // Estimated time to finish the book in ms
) {
    /**
     * Format total reading time as human-readable string
     */
    fun getFormattedTotalTime(): String {
        val hours = totalReadingTime / (1000 * 60 * 60)
        val minutes = (totalReadingTime % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    /**
     * Get reading pace description
     */
    fun getReadingPaceDescription(): String {
        return when {
            averageReadingSpeed >= 300 -> "Fast Reader"
            averageReadingSpeed >= 200 -> "Average Reader"
            averageReadingSpeed >= 100 -> "Careful Reader"
            else -> "Slow Reader"
        }
    }

    /**
     * Get reading consistency description
     */
    fun getConsistencyDescription(): String {
        return when {
            readingConsistency >= 0.8 -> "Very Consistent"
            readingConsistency >= 0.6 -> "Consistent"
            readingConsistency >= 0.4 -> "Somewhat Consistent"
            readingConsistency >= 0.2 -> "Inconsistent"
            else -> "Very Inconsistent"
        }
    }
}

/**
 * Global reading statistics across all books
 */
@Serializable
data class GlobalReadingStats(
    val totalBooksRead: Int,
    val totalBooksCompleted: Int,
    val totalReadingTime: Long, // Total time across all books in ms
    val totalSessions: Int,
    val averageReadingSpeed: Double, // Overall WPM
    val totalWordsRead: Int,
    val totalPagesRead: Int,
    val currentStreak: Int, // Current consecutive reading days
    val longestStreak: Int, // Longest consecutive reading streak
    val booksStartedThisMonth: Int,
    val booksCompletedThisMonth: Int,
    val averageBooksPerMonth: Double,
    val favoriteGenres: List<String>, // Based on most read tags
    val readingGoalProgress: ReadingGoalProgress?,
    val monthlyStats: List<MonthlyReadingStats>
) {
    /**
     * Calculate completion rate
     */
    val completionRate: Double
        get() = if (totalBooksRead > 0) totalBooksCompleted.toDouble() / totalBooksRead else 0.0

    /**
     * Get formatted total reading time
     */
    fun getFormattedTotalTime(): String {
        val days = totalReadingTime / (1000 * 60 * 60 * 24)
        val hours = (totalReadingTime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h"
            else -> "< 1h"
        }
    }
}

/**
 * Reading goal progress tracking
 */
@Serializable
data class ReadingGoalProgress(
    val goalType: ReadingGoalType,
    val targetValue: Int,
    val currentValue: Int,
    val deadline: Long, // Timestamp
    val isAchieved: Boolean = false
) {
    val progressPercentage: Float
        get() = if (targetValue > 0) (currentValue.toFloat() / targetValue * 100).coerceAtMost(100f) else 0f
}

@Serializable
enum class ReadingGoalType {
    BOOKS_PER_YEAR,
    BOOKS_PER_MONTH,
    HOURS_PER_WEEK,
    PAGES_PER_DAY
}

/**
 * Monthly reading statistics
 */
@Serializable
data class MonthlyReadingStats(
    val year: Int,
    val month: Int, // 1-12
    val booksStarted: Int,
    val booksCompleted: Int,
    val totalReadingTime: Long,
    val totalSessions: Int,
    val averageSessionLength: Double,
    val totalPagesRead: Int
)