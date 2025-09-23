package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.source.entities.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId ORDER BY lastUpdated DESC")
    fun getProgressForBookFlow(bookId: String): Flow<List<ReadingProgressEntity>>
    
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId ORDER BY lastUpdated DESC")
    suspend fun getProgressForBook(bookId: String): List<ReadingProgressEntity>
    
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getLatestProgressForBook(bookId: String): ReadingProgressEntity?
    
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId ORDER BY lastUpdated DESC LIMIT 1")
    fun getLatestProgressForBookFlow(bookId: String): Flow<ReadingProgressEntity?>
    
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId AND chapterIndex = :chapterIndex ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getProgressForChapter(bookId: String, chapterIndex: Int): ReadingProgressEntity?
    
    @Query("SELECT * FROM reading_progress WHERE id = :id")
    suspend fun getProgressById(id: String): ReadingProgressEntity?
    
    @Query("SELECT * FROM reading_progress ORDER BY lastUpdated DESC")
    fun getAllProgressFlow(): Flow<List<ReadingProgressEntity>>
    
    @Query("SELECT * FROM reading_progress ORDER BY lastUpdated DESC")
    suspend fun getAllProgress(): List<ReadingProgressEntity>
    
    @Query("SELECT * FROM reading_progress ORDER BY lastUpdated DESC LIMIT :limit")
    suspend fun getRecentProgress(limit: Int = 10): List<ReadingProgressEntity>
    
    @Query("SELECT * FROM reading_progress WHERE isChapterCompleted = 1 ORDER BY lastUpdated DESC")
    suspend fun getCompletedChapters(): List<ReadingProgressEntity>
    
    @Query("SELECT COUNT(*) FROM reading_progress WHERE bookId = :bookId")
    suspend fun getProgressCountForBook(bookId: String): Int
    
    @Query("SELECT SUM(readingTimeSeconds) FROM reading_progress WHERE bookId = :bookId")
    suspend fun getTotalReadingTimeForBook(bookId: String): Long?
    
    @Query("SELECT AVG(readingSpeedWpm) FROM reading_progress WHERE bookId = :bookId AND readingSpeedWpm IS NOT NULL")
    suspend fun getAverageReadingSpeedForBook(bookId: String): Float?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ReadingProgressEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<ReadingProgressEntity>)
    
    @Update
    suspend fun updateProgress(progress: ReadingProgressEntity)
    
    @Delete
    suspend fun deleteProgress(progress: ReadingProgressEntity)
    
    @Query("DELETE FROM reading_progress WHERE id = :id")
    suspend fun deleteProgressById(id: String)
    
    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteProgressForBook(bookId: String)
    
    @Query("DELETE FROM reading_progress WHERE bookId = :bookId AND chapterIndex = :chapterIndex")
    suspend fun deleteProgressForChapter(bookId: String, chapterIndex: Int)
    
    @Query("DELETE FROM reading_progress")
    suspend fun deleteAllProgress()
    
    @Query("DELETE FROM reading_progress WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldProgress(cutoffTime: Long)
    
    /**
     * Upsert operation: update if exists, insert if not
     * This is useful for updating reading progress where we want to maintain
     * one progress entry per chapter, or create a new one if none exists
     */
    @Transaction
    suspend fun upsertProgressForChapter(progress: ReadingProgressEntity) {
        val existing = getProgressForChapter(progress.bookId, progress.chapterIndex)
        if (existing != null) {
            // Update existing progress with new data but keep the original ID
            updateProgress(progress.copy(id = existing.id))
        } else {
            // Insert new progress entry
            insertProgress(progress)
        }
    }
    
    /**
     * Get reading statistics for a book
     */
    @Query("""
        SELECT 
            COUNT(*) as totalSessions,
            SUM(readingTimeSeconds) as totalTime,
            AVG(readingSpeedWpm) as avgSpeed,
            MAX(chapterIndex) as furthestChapter,
            COUNT(CASE WHEN isChapterCompleted = 1 THEN 1 END) as completedChapters
        FROM reading_progress 
        WHERE bookId = :bookId
    """)
    suspend fun getReadingStats(bookId: String): ReadingStats?
    
    /**
     * Get progress entries for a specific time range
     */
    @Query("SELECT * FROM reading_progress WHERE lastUpdated >= :startTime AND lastUpdated <= :endTime ORDER BY lastUpdated DESC")
    suspend fun getProgressInTimeRange(startTime: Long, endTime: Long): List<ReadingProgressEntity>
    
    /**
     * Get most active reading days (for analytics)
     */
    @Query("""
        SELECT DATE(lastUpdated/1000, 'unixepoch') as date, 
               SUM(readingTimeSeconds) as totalTime,
               COUNT(*) as sessions
        FROM reading_progress 
        WHERE lastUpdated >= :startTime 
        GROUP BY DATE(lastUpdated/1000, 'unixepoch')
        ORDER BY totalTime DESC
        LIMIT :limit
    """)
    suspend fun getMostActiveReadingDays(startTime: Long, limit: Int = 7): List<ReadingDay>
}

/**
 * Data class for reading statistics
 */
data class ReadingStats(
    val totalSessions: Int,
    val totalTime: Long,
    val avgSpeed: Float?,
    val furthestChapter: Int,
    val completedChapters: Int
)

/**
 * Data class for reading day analytics
 */
data class ReadingDay(
    val date: String,
    val totalTime: Long,
    val sessions: Int
)