package com.bsikar.helix.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsikar.helix.data.model.*
import com.bsikar.helix.data.repository.ReadingAnalyticsRepository
import com.bsikar.helix.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the analytics screen
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: ReadingAnalyticsRepository,
    private val bookRepository: BookRepository
) : ViewModel() {

    // UI state
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Analytics data
    private val _globalStats = MutableStateFlow<GlobalReadingStats?>(null)
    val globalStats: StateFlow<GlobalReadingStats?> = _globalStats.asStateFlow()

    private val _bookAnalytics = MutableStateFlow<List<ReadingAnalytics>>(emptyList())
    val bookAnalytics: StateFlow<List<ReadingAnalytics>> = _bookAnalytics.asStateFlow()

    private val _recentSessions = MutableStateFlow<List<ReadingSession>>(emptyList())
    val recentSessions: StateFlow<List<ReadingSession>> = _recentSessions.asStateFlow()

    private val _readingGoal = MutableStateFlow<ReadingGoalProgress?>(null)
    val readingGoal: StateFlow<ReadingGoalProgress?> = _readingGoal.asStateFlow()

    init {
        loadAnalyticsData()
        observeRecentSessions()
    }

    /**
     * Select a tab in the analytics screen
     */
    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        
        // Load specific data based on selected tab
        when (tabIndex) {
            1 -> loadBookAnalytics() // Books tab
            2 -> loadRecentSessions() // Sessions tab
            3 -> loadReadingGoals() // Goals tab
        }
    }

    /**
     * Refresh all analytics data
     */
    fun refreshAnalytics() {
        loadAnalyticsData()
        loadBookAnalytics()
        loadRecentSessions()
        loadReadingGoals()
    }

    /**
     * Create a new reading goal
     */
    fun createReadingGoal(goalType: ReadingGoalType, targetValue: Int, deadline: Long) {
        viewModelScope.launch {
            try {
                val goal = ReadingGoalProgress(
                    goalType = goalType,
                    targetValue = targetValue,
                    currentValue = getCurrentValueForGoalType(goalType),
                    deadline = deadline
                )
                _readingGoal.value = goal
                // TODO: Save goal to persistent storage
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create reading goal: ${e.message}"
            }
        }
    }

    /**
     * Update reading goal progress
     */
    fun updateReadingGoalProgress() {
        viewModelScope.launch {
            val currentGoal = _readingGoal.value
            if (currentGoal != null) {
                val updatedGoal = currentGoal.copy(
                    currentValue = getCurrentValueForGoalType(currentGoal.goalType),
                    isAchieved = currentGoal.currentValue >= currentGoal.targetValue
                )
                _readingGoal.value = updatedGoal
            }
        }
    }

    /**
     * Get analytics for a specific book
     */
    suspend fun getBookAnalytics(bookId: String): ReadingAnalytics? {
        return analyticsRepository.getBookAnalytics(bookId)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // Private methods

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val stats = analyticsRepository.getGlobalReadingStats()
                _globalStats.value = stats
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load analytics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadBookAnalytics() {
        viewModelScope.launch {
            try {
                val books = bookRepository.getAllBooks()
                val analyticsData = mutableListOf<ReadingAnalytics>()
                
                for (book in books) {
                    val analytics = analyticsRepository.getBookAnalytics(book.id)
                    if (analytics != null) {
                        analyticsData.add(analytics)
                    }
                }
                
                // Sort by total reading time descending
                _bookAnalytics.value = analyticsData.sortedByDescending { it.totalReadingTime }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load book analytics: ${e.message}"
            }
        }
    }

    private fun loadRecentSessions() {
        viewModelScope.launch {
            try {
                // Get sessions from the last 7 days
                analyticsRepository.getRecentReadingSessions(7).collect { sessions ->
                    _recentSessions.value = sessions.take(20) // Limit to 20 most recent
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load recent sessions: ${e.message}"
            }
        }
    }

    private fun observeRecentSessions() {
        viewModelScope.launch {
            analyticsRepository.getRecentReadingSessions(7).collect { sessions ->
                _recentSessions.value = sessions.take(10) // Show top 10 in overview
            }
        }
    }

    private fun loadReadingGoals() {
        viewModelScope.launch {
            try {
                // TODO: Load reading goals from persistent storage
                // For now, create a sample goal if none exists
                if (_readingGoal.value == null) {
                    val sampleGoal = ReadingGoalProgress(
                        goalType = ReadingGoalType.BOOKS_PER_MONTH,
                        targetValue = 4,
                        currentValue = getCurrentValueForGoalType(ReadingGoalType.BOOKS_PER_MONTH),
                        deadline = getEndOfCurrentMonth()
                    )
                    _readingGoal.value = sampleGoal
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load reading goals: ${e.message}"
            }
        }
    }

    private suspend fun getCurrentValueForGoalType(goalType: ReadingGoalType): Int {
        return when (goalType) {
            ReadingGoalType.BOOKS_PER_MONTH -> {
                val stats = _globalStats.value
                stats?.booksStartedThisMonth ?: 0
            }
            ReadingGoalType.BOOKS_PER_YEAR -> {
                val stats = _globalStats.value
                (stats?.averageBooksPerMonth?.times(12))?.toInt() ?: 0
            }
            ReadingGoalType.HOURS_PER_WEEK -> {
                // Get total reading time for current week in hours
                val totalTimeMs = analyticsRepository.getRecentReadingSessions(7)
                    .first()
                    .sumOf { it.durationMs }
                (totalTimeMs / (1000 * 60 * 60)).toInt()
            }
            ReadingGoalType.PAGES_PER_DAY -> {
                // TODO: Implement pages per day calculation
                0
            }
        }
    }

    private fun getEndOfCurrentMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}

/**
 * Extension functions for analytics calculations
 */
object AnalyticsCalculations {
    
    /**
     * Calculate reading streak from sessions
     */
    fun calculateReadingStreak(sessions: List<ReadingSession>): Int {
        if (sessions.isEmpty()) return 0
        
        val sortedSessions = sessions.sortedByDescending { it.startTime }
        val today = java.util.Calendar.getInstance()
        var streak = 0
        var currentDate = today.clone() as java.util.Calendar
        
        // Group sessions by date
        val sessionsByDate = sortedSessions.groupBy { session ->
            val sessionDate = java.util.Calendar.getInstance()
            sessionDate.timeInMillis = session.startTime
            "${sessionDate.get(java.util.Calendar.YEAR)}-${sessionDate.get(java.util.Calendar.DAY_OF_YEAR)}"
        }
        
        // Count consecutive days
        while (true) {
            val dateKey = "${currentDate.get(java.util.Calendar.YEAR)}-${currentDate.get(java.util.Calendar.DAY_OF_YEAR)}"
            if (sessionsByDate.containsKey(dateKey)) {
                streak++
                currentDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        
        return streak
    }
    
    /**
     * Calculate reading consistency score (0.0 to 1.0)
     */
    fun calculateConsistency(sessions: List<ReadingSession>, daysPeriod: Int = 30): Double {
        if (sessions.isEmpty()) return 0.0
        
        val cutoffTime = System.currentTimeMillis() - (daysPeriod * 24 * 60 * 60 * 1000L)
        val recentSessions = sessions.filter { it.startTime >= cutoffTime }
        
        if (recentSessions.isEmpty()) return 0.0
        
        // Group sessions by date
        val sessionsByDate = recentSessions.groupBy { session ->
            val sessionDate = java.util.Calendar.getInstance()
            sessionDate.timeInMillis = session.startTime
            "${sessionDate.get(java.util.Calendar.YEAR)}-${sessionDate.get(java.util.Calendar.DAY_OF_YEAR)}"
        }
        
        val daysWithReading = sessionsByDate.size
        return daysWithReading.toDouble() / daysPeriod.toDouble()
    }
    
    /**
     * Determine favorite reading time period
     */
    fun calculateFavoriteReadingTime(sessions: List<ReadingSession>): String {
        if (sessions.isEmpty()) return "No data"
        
        val timePeriods = mutableMapOf<String, Int>()
        
        sessions.forEach { session ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            val period = when (hour) {
                in 6..11 -> "Morning"
                in 12..17 -> "Afternoon"
                in 18..21 -> "Evening"
                else -> "Night"
            }
            
            timePeriods[period] = timePeriods.getOrDefault(period, 0) + 1
        }
        
        return timePeriods.maxByOrNull { it.value }?.key ?: "No data"
    }
    
    /**
     * Calculate estimated time to complete a book
     */
    fun calculateEstimatedTimeToCompletion(
        book: Book,
        averageReadingSpeed: Double,
        estimatedWordCount: Int = 80000
    ): Long {
        if (book.progress >= 1.0f || averageReadingSpeed <= 0) return 0L
        
        val wordsRemaining = estimatedWordCount * (1.0f - book.progress)
        val minutesToComplete = wordsRemaining / averageReadingSpeed
        return (minutesToComplete * 60 * 1000).toLong()
    }
}