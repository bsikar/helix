package com.bsikar.helix.data.repository

import com.bsikar.helix.data.model.*
import com.bsikar.helix.data.source.dao.ReadingSessionDao
import com.bsikar.helix.data.source.dao.BookDao
import com.bsikar.helix.data.mapper.toBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Repository for managing reading analytics and sessions
 */
@Singleton
class ReadingAnalyticsRepository @Inject constructor(
    private val readingSessionDao: ReadingSessionDao,
    private val bookDao: BookDao
) {

    /**
     * Start a new reading session
     */
    suspend fun startReadingSession(
        bookId: String,
        chapterIndex: Int,
        pageIndex: Int,
        scrollPosition: Int = 0,
        sessionType: ReadingSessionType = ReadingSessionType.NORMAL
    ): String {
        // End any existing active session for this book
        endActiveSessionForBook(bookId)

        val sessionId = UUID.randomUUID().toString()
        val session = ReadingSession(
            id = sessionId,
            bookId = bookId,
            startTime = System.currentTimeMillis(),
            startChapter = chapterIndex,
            startPage = pageIndex,
            startScrollPosition = scrollPosition,
            sessionType = sessionType
        )

        readingSessionDao.insertSession(session)
        return sessionId
    }

    /**
     * End an active reading session
     */
    suspend fun endReadingSession(
        sessionId: String,
        endChapter: Int,
        endPage: Int,
        endScrollPosition: Int = 0,
        wordsRead: Int = 0,
        charactersRead: Int = 0
    ) {
        val session = readingSessionDao.getAllSessions().map { sessions ->
            sessions.find { it.id == sessionId }
        }.toString() // This is a simplified approach, should be improved

        // For now, let's end any active session with the provided data
        val activeSession = readingSessionDao.getActiveSession()
        if (activeSession != null && activeSession.id == sessionId) {
            val updatedSession = activeSession.copy(
                endTime = System.currentTimeMillis(),
                endChapter = endChapter,
                endPage = endPage,
                endScrollPosition = endScrollPosition,
                wordsRead = wordsRead,
                charactersRead = charactersRead
            )
            readingSessionDao.updateSession(updatedSession)
        }
    }

    /**
     * End active session for a specific book
     */
    suspend fun endActiveSessionForBook(bookId: String) {
        val activeSession = readingSessionDao.getActiveSessionForBook(bookId)
        if (activeSession != null) {
            val updatedSession = activeSession.copy(
                endTime = System.currentTimeMillis()
            )
            readingSessionDao.updateSession(updatedSession)
        }
    }

    /**
     * End all active sessions
     */
    suspend fun endAllActiveSessions() {
        readingSessionDao.endAllActiveSessions(System.currentTimeMillis())
    }

    /**
     * Get reading analytics for a specific book
     */
    suspend fun getBookAnalytics(bookId: String): ReadingAnalytics? {
        val bookEntity = bookDao.getBookById(bookId) ?: return null
        val book = bookEntity.toBook()
        
        val totalReadingTime = readingSessionDao.getTotalReadingTimeForBook(bookId)
        val sessionCount = readingSessionDao.getSessionCountForBook(bookId)
        val averageSessionLength = readingSessionDao.getAverageSessionLengthForBook(bookId) ?: 0.0
        val totalWordsRead = readingSessionDao.getTotalWordsReadForBook(bookId)
        val averageReadingSpeed = readingSessionDao.getAverageReadingSpeedForBook(bookId)
        val longestSession = readingSessionDao.getLongestSessionForBook(bookId) ?: 0L
        val shortestSession = readingSessionDao.getShortestSessionForBook(bookId) ?: 0L
        val firstSession = readingSessionDao.getFirstSessionForBook(bookId)
        val lastSession = readingSessionDao.getLastSessionForBook(bookId)

        // Calculate reading streak and other metrics
        val readingStreak = calculateReadingStreak(bookId)
        val consistency = calculateReadingConsistency(bookId)
        val favoriteTime = calculateFavoriteReadingTime(bookId)
        val estimatedCompletion = calculateEstimatedTimeToCompletion(book, averageReadingSpeed)

        return ReadingAnalytics(
            bookId = bookId,
            bookTitle = book.title,
            totalReadingTime = totalReadingTime,
            totalSessions = sessionCount,
            averageSessionLength = averageSessionLength / (1000.0 * 60.0), // Convert to minutes
            totalWordsRead = totalWordsRead,
            totalCharactersRead = 0, // TODO: Implement character counting
            averageReadingSpeed = averageReadingSpeed,
            totalPagesRead = 0, // TODO: Calculate from sessions
            totalChaptersRead = 0, // TODO: Calculate from sessions
            firstReadingDate = firstSession?.startTime ?: 0L,
            lastReadingDate = lastSession?.startTime ?: 0L,
            completionPercentage = book.progress,
            readingStreak = readingStreak,
            longestSession = longestSession,
            shortestSession = shortestSession,
            favoriteReadingTime = favoriteTime,
            readingConsistency = consistency,
            estimatedTimeToCompletion = estimatedCompletion
        )
    }

    /**
     * Get global reading statistics
     */
    suspend fun getGlobalReadingStats(): GlobalReadingStats {
        val allBookEntities = bookDao.getAllBooks()
        val allBooks = allBookEntities.map { it.toBook() }
        val totalReadingTime = readingSessionDao.getTotalReadingTime()
        val totalSessions = readingSessionDao.getTotalSessionCount()
        val totalWordsRead = readingSessionDao.getTotalWordsRead()
        val averageReadingSpeed = readingSessionDao.getOverallAverageReadingSpeed()

        val totalBooksRead = allBooks.count { it.progress > 0 }
        val totalBooksCompleted = allBooks.count { it.progress >= 1.0f }
        val currentStreak = calculateGlobalReadingStreak()
        val longestStreak = calculateLongestReadingStreak()

        // Calculate monthly stats
        val currentMonth = Calendar.getInstance()
        val booksStartedThisMonth = allBooks.count { book ->
            val bookStarted = Calendar.getInstance().apply { timeInMillis = book.dateAdded }
            bookStarted.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
            bookStarted.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
        }

        val booksCompletedThisMonth = allBooks.count { book ->
            book.progress >= 1.0f && 
            // Approximation - would need completion date tracking for accuracy
            book.lastReadTimestamp > getStartOfMonth()
        }

        val favoriteGenres = calculateFavoriteGenres(allBooks)
        val monthlyStats = generateMonthlyStats()

        return GlobalReadingStats(
            totalBooksRead = totalBooksRead,
            totalBooksCompleted = totalBooksCompleted,
            totalReadingTime = totalReadingTime,
            totalSessions = totalSessions,
            averageReadingSpeed = averageReadingSpeed,
            totalWordsRead = totalWordsRead,
            totalPagesRead = 0, // TODO: Calculate from all sessions
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            booksStartedThisMonth = booksStartedThisMonth,
            booksCompletedThisMonth = booksCompletedThisMonth,
            averageBooksPerMonth = calculateAverageBooksPerMonth(allBooks),
            favoriteGenres = favoriteGenres,
            readingGoalProgress = null, // TODO: Implement reading goals
            monthlyStats = monthlyStats
        )
    }

    /**
     * Get reading sessions for a book
     */
    fun getReadingSessionsForBook(bookId: String): Flow<List<ReadingSession>> {
        return readingSessionDao.getSessionsForBook(bookId)
    }

    /**
     * Get recent reading sessions
     */
    fun getRecentReadingSessions(days: Int = 7): Flow<List<ReadingSession>> {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return readingSessionDao.getRecentSessions(cutoffTime)
    }

    /**
     * Calculate reading speed in real-time
     */
    fun calculateReadingSpeed(
        wordsRead: Int,
        startTime: Long,
        endTime: Long = System.currentTimeMillis()
    ): Double {
        val durationMinutes = (endTime - startTime) / (1000.0 * 60.0)
        return if (durationMinutes > 0) wordsRead / durationMinutes else 0.0
    }

    /**
     * Estimate words in text content
     */
    fun estimateWordCount(text: String): Int {
        return text.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .size
    }

    /**
     * Estimate reading time for content
     */
    fun estimateReadingTime(wordCount: Int, wordsPerMinute: Double = 250.0): Long {
        val minutes = wordCount / wordsPerMinute
        return (minutes * 60 * 1000).toLong() // Convert to milliseconds
    }

    // Private helper methods

    private suspend fun calculateReadingStreak(bookId: String): Int {
        // Simplified implementation - count consecutive days with reading sessions
        val sessions = readingSessionDao.getSessionsForBook(bookId)
        // TODO: Implement proper streak calculation
        return 0
    }

    private suspend fun calculateReadingConsistency(bookId: String): Double {
        // Calculate how consistently the user reads this book
        // TODO: Implement consistency calculation based on session frequency
        return 0.5
    }

    private suspend fun calculateFavoriteReadingTime(bookId: String): String {
        // TODO: Analyze session start times to determine favorite reading time
        return "evening"
    }

    private fun calculateEstimatedTimeToCompletion(book: Book, averageSpeed: Double): Long {
        if (book.progress >= 1.0f || averageSpeed <= 0) return 0L
        
        // Rough estimation based on average book length and reading speed
        val estimatedTotalWords = 80000 // Average book length
        val wordsRemaining = estimatedTotalWords * (1.0f - book.progress)
        val minutesToComplete = wordsRemaining / averageSpeed
        return (minutesToComplete * 60 * 1000).toLong()
    }

    private suspend fun calculateGlobalReadingStreak(): Int {
        // TODO: Calculate current consecutive reading days across all books
        return 0
    }

    private suspend fun calculateLongestReadingStreak(): Int {
        // TODO: Calculate longest historical reading streak
        return 0
    }

    private fun calculateFavoriteGenres(books: List<Book>): List<String> {
        // Calculate most common tags/genres
        return books.flatMap { it.tags }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    private fun calculateAverageBooksPerMonth(books: List<Book>): Double {
        if (books.isEmpty()) return 0.0
        
        val oldestBook = books.minByOrNull { it.dateAdded }?.dateAdded ?: System.currentTimeMillis()
        val monthsSinceStart = max(1, getMonthsDifference(oldestBook, System.currentTimeMillis()))
        
        return books.size.toDouble() / monthsSinceStart
    }

    private suspend fun generateMonthlyStats(): List<MonthlyReadingStats> {
        // TODO: Generate monthly statistics from reading sessions
        return emptyList()
    }

    private fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthsDifference(startDate: Long, endDate: Long): Int {
        val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
        val endCal = Calendar.getInstance().apply { timeInMillis = endDate }
        
        return (endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
               (endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH))
    }
}

/**
 * Helper class for tracking reading sessions in the reader
 */
class ReadingSessionTracker @Inject constructor(
    private val analyticsRepository: ReadingAnalyticsRepository
) {
    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0
    private var wordsReadInSession: Int = 0

    /**
     * Start tracking a reading session
     */
    suspend fun startSession(
        bookId: String,
        chapterIndex: Int,
        pageIndex: Int,
        scrollPosition: Int = 0
    ) {
        currentSessionId = analyticsRepository.startReadingSession(
            bookId, chapterIndex, pageIndex, scrollPosition
        )
        sessionStartTime = System.currentTimeMillis()
        wordsReadInSession = 0
    }

    /**
     * Update session progress
     */
    fun updateProgress(additionalWords: Int) {
        wordsReadInSession += additionalWords
    }

    /**
     * End the current session
     */
    suspend fun endSession(
        endChapter: Int,
        endPage: Int,
        endScrollPosition: Int = 0
    ) {
        currentSessionId?.let { sessionId ->
            analyticsRepository.endReadingSession(
                sessionId = sessionId,
                endChapter = endChapter,
                endPage = endPage,
                endScrollPosition = endScrollPosition,
                wordsRead = wordsReadInSession,
                charactersRead = wordsReadInSession * 5 // Rough estimation
            )
        }
        currentSessionId = null
        wordsReadInSession = 0
    }

    /**
     * Check if session is active
     */
    val isSessionActive: Boolean
        get() = currentSessionId != null

    /**
     * Get current session reading speed
     */
    fun getCurrentReadingSpeed(): Double {
        if (!isSessionActive || sessionStartTime == 0L) return 0.0
        
        return analyticsRepository.calculateReadingSpeed(
            wordsReadInSession,
            sessionStartTime
        )
    }
}