package com.bsikar.helix.data.repository

import android.util.Log
import com.bsikar.helix.data.model.ReadingProgress
import com.bsikar.helix.data.source.dao.ReadingProgressDao
import com.bsikar.helix.data.source.dao.ReadingStats
import com.bsikar.helix.data.source.entities.toEntity
import com.bsikar.helix.data.source.entities.toReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingProgressRepository @Inject constructor(
    private val readingProgressDao: ReadingProgressDao
) {
    private val TAG = "ReadingProgressRepository"
    
    /**
     * Get all reading progress for a book as Flow
     */
    fun getProgressForBookFlow(bookId: String): Flow<List<ReadingProgress>> {
        return readingProgressDao.getProgressForBookFlow(bookId).map { entities ->
            entities.map { it.toReadingProgress() }
        }
    }
    
    /**
     * Get latest reading progress for a book as Flow
     */
    fun getLatestProgressForBookFlow(bookId: String): Flow<ReadingProgress?> {
        return readingProgressDao.getLatestProgressForBookFlow(bookId).map { entity ->
            entity?.toReadingProgress()
        }
    }
    
    /**
     * Get latest reading progress for a book
     */
    suspend fun getLatestProgressForBook(bookId: String): ReadingProgress? {
        return readingProgressDao.getLatestProgressForBook(bookId)?.toReadingProgress()
    }
    
    /**
     * Get reading progress for a specific chapter
     */
    suspend fun getProgressForChapter(bookId: String, chapterIndex: Int): ReadingProgress? {
        return readingProgressDao.getProgressForChapter(bookId, chapterIndex)?.toReadingProgress()
    }
    
    /**
     * Get all reading progress entries
     */
    fun getAllProgressFlow(): Flow<List<ReadingProgress>> {
        return readingProgressDao.getAllProgressFlow().map { entities ->
            entities.map { it.toReadingProgress() }
        }
    }
    
    /**
     * Get recent reading progress entries
     */
    suspend fun getRecentProgress(limit: Int = 10): List<ReadingProgress> {
        return readingProgressDao.getRecentProgress(limit).map { it.toReadingProgress() }
    }
    
    /**
     * Save reading progress
     * This will create a new progress entry while keeping historical data
     */
    suspend fun saveProgress(progress: ReadingProgress) {
        try {
            Log.d(TAG, "Saving reading progress for book ${progress.bookId}, chapter ${progress.chapterIndex}")
            readingProgressDao.insertProgress(progress.toEntity())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save reading progress: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Update or insert progress for a specific chapter
     * This maintains one progress entry per chapter
     */
    suspend fun upsertProgressForChapter(progress: ReadingProgress) {
        try {
            Log.d(TAG, "Upserting reading progress for book ${progress.bookId}, chapter ${progress.chapterIndex}")
            readingProgressDao.upsertProgressForChapter(progress.toEntity())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upsert reading progress: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Update existing progress entry
     */
    suspend fun updateProgress(progress: ReadingProgress) {
        try {
            readingProgressDao.updateProgress(progress.toEntity())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update reading progress: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Delete specific progress entry
     */
    suspend fun deleteProgress(progressId: String) {
        try {
            readingProgressDao.deleteProgressById(progressId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete reading progress: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Delete all progress for a book
     */
    suspend fun deleteProgressForBook(bookId: String) {
        try {
            Log.d(TAG, "Deleting all reading progress for book $bookId")
            readingProgressDao.deleteProgressForBook(bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete progress for book: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Delete progress for a specific chapter
     */
    suspend fun deleteProgressForChapter(bookId: String, chapterIndex: Int) {
        try {
            readingProgressDao.deleteProgressForChapter(bookId, chapterIndex)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete progress for chapter: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Get reading statistics for a book
     */
    suspend fun getReadingStatsForBook(bookId: String): ReadingStats? {
        return try {
            readingProgressDao.getReadingStats(bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get reading stats: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get total reading time for a book
     */
    suspend fun getTotalReadingTimeForBook(bookId: String): Long {
        return try {
            readingProgressDao.getTotalReadingTimeForBook(bookId) ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get total reading time: ${e.message}", e)
            0L
        }
    }
    
    /**
     * Get average reading speed for a book
     */
    suspend fun getAverageReadingSpeedForBook(bookId: String): Float? {
        return try {
            readingProgressDao.getAverageReadingSpeedForBook(bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get average reading speed: ${e.message}", e)
            null
        }
    }
    
    /**
     * Clean up old progress entries
     * Keeps only the most recent entry per chapter and deletes older ones
     */
    suspend fun cleanupOldProgress(bookId: String) {
        try {
            Log.d(TAG, "Cleaning up old progress entries for book $bookId")
            val allProgress = readingProgressDao.getProgressForBook(bookId)
            
            // Group by chapter and keep only the most recent for each
            val progressByChapter = allProgress.groupBy { it.chapterIndex }
            
            progressByChapter.values.forEach { chapterProgress ->
                if (chapterProgress.size > 1) {
                    // Sort by lastUpdated descending and keep only the first (most recent)
                    val sorted = chapterProgress.sortedByDescending { it.lastUpdated }
                    val toDelete = sorted.drop(1) // All except the most recent
                    
                    toDelete.forEach { progress ->
                        readingProgressDao.deleteProgressById(progress.id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old progress: ${e.message}", e)
        }
    }
    
    /**
     * Delete very old progress entries (older than specified days)
     */
    suspend fun deleteOldProgress(olderThanDays: Int = 90) {
        try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            readingProgressDao.deleteOldProgress(cutoffTime)
            Log.d(TAG, "Deleted progress entries older than $olderThanDays days")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete old progress: ${e.message}", e)
        }
    }
    
    /**
     * Get progress entries within a specific time range
     */
    suspend fun getProgressInTimeRange(startTime: Long, endTime: Long): List<ReadingProgress> {
        return try {
            readingProgressDao.getProgressInTimeRange(startTime, endTime).map { it.toReadingProgress() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get progress in time range: ${e.message}", e)
            emptyList()
        }
    }
}