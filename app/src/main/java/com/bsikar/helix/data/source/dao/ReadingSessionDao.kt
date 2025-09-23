package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.model.ReadingSession
import com.bsikar.helix.data.model.ReadingSessionType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reading sessions
 */
@Dao
interface ReadingSessionDao {

    /**
     * Insert a new reading session
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSession)

    /**
     * Update an existing reading session
     */
    @Update
    suspend fun updateSession(session: ReadingSession)

    /**
     * Delete a reading session
     */
    @Delete
    suspend fun deleteSession(session: ReadingSession)

    /**
     * Delete sessions by book ID
     */
    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    suspend fun deleteSessionsByBookId(bookId: String)

    /**
     * Get all reading sessions
     */
    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ReadingSession>>

    /**
     * Get reading sessions for a specific book
     */
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun getSessionsForBook(bookId: String): Flow<List<ReadingSession>>

    /**
     * Get the currently active session (if any)
     */
    @Query("SELECT * FROM reading_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(): ReadingSession?

    /**
     * Get active session for a specific book
     */
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveSessionForBook(bookId: String): ReadingSession?

    /**
     * Get recent reading sessions (last 7 days)
     */
    @Query("""
        SELECT * FROM reading_sessions 
        WHERE startTime >= :sevenDaysAgo 
        ORDER BY startTime DESC
    """)
    fun getRecentSessions(sevenDaysAgo: Long): Flow<List<ReadingSession>>

    /**
     * Get reading sessions within a date range
     */
    @Query("""
        SELECT * FROM reading_sessions 
        WHERE startTime >= :startDate AND startTime <= :endDate 
        ORDER BY startTime DESC
    """)
    fun getSessionsInDateRange(startDate: Long, endDate: Long): Flow<List<ReadingSession>>

    /**
     * Get total reading time for a book
     */
    @Query("""
        SELECT COALESCE(SUM(endTime - startTime), 0) as totalTime 
        FROM reading_sessions 
        WHERE bookId = :bookId AND endTime IS NOT NULL
    """)
    suspend fun getTotalReadingTimeForBook(bookId: String): Long

    /**
     * Get total reading time across all books
     */
    @Query("""
        SELECT COALESCE(SUM(endTime - startTime), 0) as totalTime 
        FROM reading_sessions 
        WHERE endTime IS NOT NULL
    """)
    suspend fun getTotalReadingTime(): Long

    /**
     * Get reading session count for a book
     */
    @Query("SELECT COUNT(*) FROM reading_sessions WHERE bookId = :bookId")
    suspend fun getSessionCountForBook(bookId: String): Int

    /**
     * Get total session count
     */
    @Query("SELECT COUNT(*) FROM reading_sessions")
    suspend fun getTotalSessionCount(): Int

    /**
     * Get average session length for a book
     */
    @Query("""
        SELECT AVG(endTime - startTime) 
        FROM reading_sessions 
        WHERE bookId = :bookId AND endTime IS NOT NULL
    """)
    suspend fun getAverageSessionLengthForBook(bookId: String): Double?

    /**
     * Get total words read for a book
     */
    @Query("SELECT COALESCE(SUM(wordsRead), 0) FROM reading_sessions WHERE bookId = :bookId")
    suspend fun getTotalWordsReadForBook(bookId: String): Int

    /**
     * Get total words read across all books
     */
    @Query("SELECT COALESCE(SUM(wordsRead), 0) FROM reading_sessions")
    suspend fun getTotalWordsRead(): Int

    /**
     * Get average reading speed for a book (words per minute)
     */
    @Query("""
        SELECT 
            CASE 
                WHEN SUM(endTime - startTime) > 0 
                THEN (SUM(wordsRead) * 60000.0) / SUM(endTime - startTime)
                ELSE 0 
            END as wpm
        FROM reading_sessions 
        WHERE bookId = :bookId AND endTime IS NOT NULL
    """)
    suspend fun getAverageReadingSpeedForBook(bookId: String): Double

    /**
     * Get overall average reading speed
     */
    @Query("""
        SELECT 
            CASE 
                WHEN SUM(endTime - startTime) > 0 
                THEN (SUM(wordsRead) * 60000.0) / SUM(endTime - startTime)
                ELSE 0 
            END as wpm
        FROM reading_sessions 
        WHERE endTime IS NOT NULL
    """)
    suspend fun getOverallAverageReadingSpeed(): Double

    /**
     * Get longest reading session for a book
     */
    @Query("""
        SELECT MAX(endTime - startTime) 
        FROM reading_sessions 
        WHERE bookId = :bookId AND endTime IS NOT NULL
    """)
    suspend fun getLongestSessionForBook(bookId: String): Long?

    /**
     * Get shortest reading session for a book
     */
    @Query("""
        SELECT MIN(endTime - startTime) 
        FROM reading_sessions 
        WHERE bookId = :bookId AND endTime IS NOT NULL AND (endTime - startTime) > 0
    """)
    suspend fun getShortestSessionForBook(bookId: String): Long?

    /**
     * Get reading sessions by type
     */
    @Query("SELECT * FROM reading_sessions WHERE sessionType = :sessionType ORDER BY startTime DESC")
    fun getSessionsByType(sessionType: ReadingSessionType): Flow<List<ReadingSession>>

    /**
     * Get first reading session for a book
     */
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime ASC LIMIT 1")
    suspend fun getFirstSessionForBook(bookId: String): ReadingSession?

    /**
     * Get last reading session for a book
     */
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastSessionForBook(bookId: String): ReadingSession?

    /**
     * Get reading sessions for today
     */
    @Query("""
        SELECT * FROM reading_sessions 
        WHERE startTime >= :startOfDay AND startTime <= :endOfDay 
        ORDER BY startTime DESC
    """)
    fun getSessionsForToday(startOfDay: Long, endOfDay: Long): Flow<List<ReadingSession>>

    /**
     * Get reading sessions for current week
     */
    @Query("""
        SELECT * FROM reading_sessions 
        WHERE startTime >= :startOfWeek 
        ORDER BY startTime DESC
    """)
    fun getSessionsForCurrentWeek(startOfWeek: Long): Flow<List<ReadingSession>>

    /**
     * Get reading sessions for current month
     */
    @Query("""
        SELECT * FROM reading_sessions 
        WHERE startTime >= :startOfMonth 
        ORDER BY startTime DESC
    """)
    fun getSessionsForCurrentMonth(startOfMonth: Long): Flow<List<ReadingSession>>

    /**
     * Get unique days with reading activity
     */
    @Query("""
        SELECT DISTINCT DATE(startTime / 1000, 'unixepoch') as readingDate
        FROM reading_sessions 
        ORDER BY readingDate DESC
    """)
    suspend fun getUniqueDaysWithReading(): List<String>

    /**
     * Get reading session count by day of week (0 = Sunday, 6 = Saturday)
     */
    @Query("""
        SELECT 
            CAST(strftime('%w', startTime / 1000, 'unixepoch') AS INTEGER) as dayOfWeek,
            COUNT(*) as sessionCount
        FROM reading_sessions 
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek
    """)
    suspend fun getSessionCountByDayOfWeek(): List<DayOfWeekStats>

    /**
     * Get reading session count by hour of day
     */
    @Query("""
        SELECT 
            CAST(strftime('%H', startTime / 1000, 'unixepoch') AS INTEGER) as hour,
            COUNT(*) as sessionCount
        FROM reading_sessions 
        GROUP BY hour
        ORDER BY hour
    """)
    suspend fun getSessionCountByHour(): List<HourStats>

    /**
     * Clean up old sessions (older than specified days)
     */
    @Query("DELETE FROM reading_sessions WHERE startTime < :cutoffTime")
    suspend fun cleanupOldSessions(cutoffTime: Long)

    /**
     * End all active sessions (useful for app lifecycle management)
     */
    @Query("UPDATE reading_sessions SET endTime = :endTime WHERE endTime IS NULL")
    suspend fun endAllActiveSessions(endTime: Long)
}

/**
 * Data class for day of week statistics
 */
data class DayOfWeekStats(
    val dayOfWeek: Int, // 0 = Sunday, 6 = Saturday
    val sessionCount: Int
)

/**
 * Data class for hour statistics
 */
data class HourStats(
    val hour: Int, // 0-23
    val sessionCount: Int
)