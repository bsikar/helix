package com.bsikar.helix.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.data.model.ParsedEpub
import com.bsikar.helix.data.model.EpubChapter
import com.bsikar.helix.data.model.UiState
import com.bsikar.helix.data.EpubParser
import com.bsikar.helix.data.repository.ReadingProgressRepository
import com.bsikar.helix.data.repository.ChapterRepository
import com.bsikar.helix.data.repository.ReadingAnalyticsRepository
import com.bsikar.helix.data.model.ReadingProgress
import com.bsikar.helix.data.model.EpubTocEntry
import com.bsikar.helix.data.source.dao.ReadingStats
import com.bsikar.helix.ui.screens.ContentElement
import com.bsikar.helix.ui.screens.parseHtmlToContentElements
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

/**
 * ViewModel for managing reader content, caching, and performance optimizations
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val libraryManager: LibraryManager,
    private val preferencesManager: UserPreferencesManager,
    private val readingProgressRepository: ReadingProgressRepository,
    private val chapterRepository: ChapterRepository,
    private val analyticsRepository: ReadingAnalyticsRepository
) : ViewModel() {

    // Cache for parsed content to prevent re-parsing on recomposition
    private val contentCache = mutableMapOf<String, List<ContentElement>>()
    
    // Cache for parsed EPUB data
    private val epubCache = mutableMapOf<String, ParsedEpub>()
    
    // Session tracking
    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0
    private var wordsReadInSession: Int = 0
    
    // Current book state
    private val _currentBook = MutableStateFlow<com.bsikar.helix.data.model.Book?>(null)
    val currentBook: StateFlow<com.bsikar.helix.data.model.Book?> = _currentBook.asStateFlow()
    
    // EPUB content state
    private val _parsedEpub = MutableStateFlow<ParsedEpub?>(null)
    val parsedEpub: StateFlow<ParsedEpub?> = _parsedEpub.asStateFlow()
    
    // Chapter content state
    private val _currentContent = MutableStateFlow<List<ContentElement>>(emptyList())
    val currentContent: StateFlow<List<ContentElement>> = _currentContent.asStateFlow()
    
    // Loading states
    private val _isLoadingContent = MutableStateFlow(false)
    val isLoadingContent: StateFlow<Boolean> = _isLoadingContent.asStateFlow()
    
    private val _isLoadingChapter = MutableStateFlow(false)
    val isLoadingChapter: StateFlow<Boolean> = _isLoadingChapter.asStateFlow()
    
    // Error state
    private val _loadingError = MutableStateFlow<String?>(null)
    val loadingError: StateFlow<String?> = _loadingError.asStateFlow()
    
    // Current reading position
    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()
    
    // Chapter navigation state
    private val _chapters = MutableStateFlow<List<EpubChapter>>(emptyList())
    val chapters: StateFlow<List<EpubChapter>> = _chapters.asStateFlow()
    
    private val _tableOfContents = MutableStateFlow<List<EpubTocEntry>>(emptyList())
    val tableOfContents: StateFlow<List<EpubTocEntry>> = _tableOfContents.asStateFlow()
    
    private val _currentChapter = MutableStateFlow<EpubChapter?>(null)
    val currentChapter: StateFlow<EpubChapter?> = _currentChapter.asStateFlow()
    
    private val _readingProgressPercentage = MutableStateFlow(0f)
    val readingProgressPercentage: StateFlow<Float> = _readingProgressPercentage.asStateFlow()
    
    // Reading progress state
    private val _currentReadingProgress = MutableStateFlow<ReadingProgress?>(null)
    val currentReadingProgress: StateFlow<ReadingProgress?> = _currentReadingProgress.asStateFlow()
    
    private val _readingSessionStartTime = MutableStateFlow<Long?>(null)
    private val _lastScrollPosition = MutableStateFlow(0)
    private val _currentPageNumber = MutableStateFlow(1)
    
    /**
     * Load book content and cache EPUB data (internal method)
     */
    private fun loadBookInternal(book: com.bsikar.helix.data.model.Book) {
        if (_currentBook.value?.id == book.id) {
            // Book already loaded, just update the current book reference
            _currentBook.value = book
            return
        }
        
        _currentBook.value = book
        _currentChapterIndex.value = (book.currentChapter - 1).coerceAtLeast(0)
        
        if (book.isImported) {
            loadEpubContent(book)
        } else {
            // For non-imported books, clear EPUB data
            _parsedEpub.value = null
            _currentContent.value = emptyList()
        }
    }
    
    /**
     * Load EPUB content with caching
     */
    private fun loadEpubContent(book: com.bsikar.helix.data.model.Book) {
        viewModelScope.launch {
            _isLoadingContent.value = true
            _loadingError.value = null
            
            try {
                // Check cache first
                val cachedEpub = epubCache[book.id]
                if (cachedEpub != null) {
                    _parsedEpub.value = cachedEpub
                    // Load chapters from database
                    launch { loadChaptersForBook(book.id) }
                    loadChapterContent(cachedEpub, _currentChapterIndex.value)
                } else {
                    // Load from LibraryManager
                    val result = libraryManager.getEpubContent(book)
                    if (result.isSuccess) {
                        val epub = result.getOrNull()
                        if (epub != null) {
                            // Cache the EPUB data
                            epubCache[book.id] = epub
                            _parsedEpub.value = epub
                            
                            // Store chapters in database if not already stored
                            launch { storeChaptersForBook(book.id, epub) }
                            
                            loadChapterContent(epub, _currentChapterIndex.value)
                        } else {
                            _loadingError.value = "EPUB content is null"
                        }
                    } else {
                        _loadingError.value = result.exceptionOrNull()?.message ?: "Failed to load EPUB content"
                    }
                }
            } catch (e: Exception) {
                _loadingError.value = "Error loading book: ${e.message}"
            } finally {
                _isLoadingContent.value = false
            }
        }
    }
    
    /**
     * Load and cache chapter content
     */
    fun loadChapterContent(epub: ParsedEpub, chapterIndex: Int, context: Context? = null) {
        viewModelScope.launch {
            _isLoadingChapter.value = true
            
            try {
                val book = _currentBook.value ?: return@launch
                val cacheKey = "${book.id}_$chapterIndex"
                
                // Check cache first
                val cachedContent = contentCache[cacheKey]
                if (cachedContent != null) {
                    _currentContent.value = cachedContent
                    _currentChapterIndex.value = chapterIndex
                    return@launch
                }
                
                // Load and parse content
                val chapter = epub.chapters.getOrNull(chapterIndex)
                if (chapter != null) {
                    val content = if (chapter.content.isNotEmpty()) {
                        // Content already available (legacy)
                        parseContentWithSettings(chapter.content, epub.images)
                    } else if (context != null) {
                        // Load content on demand
                        loadChapterContentOnDemand(context, book, epub, chapter)
                    } else {
                        // No context available, show error
                        listOf(ContentElement.TextElement(
                            androidx.compose.ui.text.AnnotatedString("Context required to load chapter content")
                        ))
                    }
                    
                    // Cache the parsed content
                    contentCache[cacheKey] = content
                    _currentContent.value = content
                    _currentChapterIndex.value = chapterIndex
                } else {
                    _currentContent.value = listOf(ContentElement.TextElement(
                        androidx.compose.ui.text.AnnotatedString("Chapter not found")
                    ))
                }
            } catch (e: Exception) {
                _currentContent.value = listOf(ContentElement.TextElement(
                    androidx.compose.ui.text.AnnotatedString("Error loading chapter: ${e.message}")
                ))
            } finally {
                _isLoadingChapter.value = false
            }
        }
    }
    
    /**
     * Load chapter content on demand using EpubParser
     */
    private suspend fun loadChapterContentOnDemand(
        context: Context,
        book: com.bsikar.helix.data.model.Book,
        epub: ParsedEpub,
        chapter: EpubChapter
    ): List<ContentElement> {
        val epubParser = EpubParser(context)
        val contentResult = if (book.originalUri != null) {
            // Use URI-based loading for SAF imports
            epubParser.loadChapterContentFromUri(
                context = context,
                uri = book.originalUri!!,
                chapterHref = chapter.href,
                opfPath = epub.opfPath
            )
        } else if (epub.filePath != null) {
            // Use file-based loading for direct file imports
            epubParser.loadChapterContent(
                epubFilePath = epub.filePath!!,
                chapterHref = chapter.href,
                opfPath = epub.opfPath
            )
        } else {
            // Fallback: try to use originalUri if no file path
            book.originalUri?.let { uri ->
                epubParser.loadChapterContentFromUri(
                    context = context,
                    uri = uri,
                    chapterHref = chapter.href,
                    opfPath = epub.opfPath
                )
            } ?: Result.failure(Exception("No file path or URI available"))
        }
        
        return if (contentResult.isSuccess) {
            val chapterContent = contentResult.getOrThrow()
            parseContentWithSettings(chapterContent, epub.images)
        } else {
            listOf(ContentElement.TextElement(
                androidx.compose.ui.text.AnnotatedString("Error loading chapter: ${contentResult.exceptionOrNull()?.message}")
            ))
        }
    }
    
    /**
     * Parse HTML content with current reader settings
     */
    private fun parseContentWithSettings(htmlContent: String, images: Map<String, String>): List<ContentElement> {
        val readerSettings = preferencesManager.preferences.value.selectedReaderSettings
        return parseHtmlToContentElements(htmlContent, readerSettings, images)
    }
    
    /**
     * Navigate to a specific chapter
     */
    fun navigateToChapter(chapterIndex: Int, context: Context? = null) {
        val epub = _parsedEpub.value ?: return
        if (chapterIndex != _currentChapterIndex.value) {
            loadChapterContent(epub, chapterIndex, context)
        }
    }
    
    /**
     * Navigate to previous chapter
     */
    fun previousChapter(context: Context? = null) {
        val currentIndex = _currentChapterIndex.value
        if (currentIndex > 0) {
            navigateToChapter(currentIndex - 1, context)
        }
    }
    
    /**
     * Navigate to next chapter
     */
    fun nextChapter(context: Context? = null) {
        val epub = _parsedEpub.value ?: return
        val currentIndex = _currentChapterIndex.value
        if (currentIndex < epub.chapters.size - 1) {
            navigateToChapter(currentIndex + 1, context)
        }
    }
    
    /**
     * Invalidate cache for a specific book (useful when settings change)
     */
    fun invalidateBookCache(bookId: String) {
        // Remove EPUB cache
        epubCache.remove(bookId)
        
        // Remove content cache for all chapters of this book
        val keysToRemove = contentCache.keys.filter { it.startsWith("${bookId}_") }
        keysToRemove.forEach { contentCache.remove(it) }
    }
    
    /**
     * Invalidate content cache when reader settings change
     */
    fun invalidateContentCache() {
        contentCache.clear()
        // Reload current chapter with new settings
        val epub = _parsedEpub.value
        val chapterIndex = _currentChapterIndex.value
        if (epub != null) {
            viewModelScope.launch {
                val book = _currentBook.value ?: return@launch
                val cacheKey = "${book.id}_$chapterIndex"
                
                // Re-parse content with new settings
                val chapter = epub.chapters.getOrNull(chapterIndex)
                if (chapter != null && chapter.content.isNotEmpty()) {
                    val content = parseContentWithSettings(chapter.content, epub.images)
                    contentCache[cacheKey] = content
                    _currentContent.value = content
                }
            }
        }
    }
    
    /**
     * Clear all caches (useful for memory management)
     */
    fun clearAllCaches() {
        contentCache.clear()
        epubCache.clear()
    }
    
    /**
     * Get cache statistics for debugging
     */
    fun getCacheStats(): String {
        return "EPUB cache: ${epubCache.size} entries, Content cache: ${contentCache.size} entries"
    }
    
    /**
     * Load reading progress for the current book
     */
    private fun loadReadingProgress(bookId: String) {
        viewModelScope.launch {
            try {
                val latestProgress = readingProgressRepository.getLatestProgressForBook(bookId)
                _currentReadingProgress.value = latestProgress
                
                // Restore position if progress exists
                latestProgress?.let { progress ->
                    _currentChapterIndex.value = progress.chapterIndex
                    _lastScrollPosition.value = progress.scrollPosition
                    _currentPageNumber.value = progress.pageNumber
                }
            } catch (e: Exception) {
                // Log error but don't block reading
                android.util.Log.e("ReaderViewModel", "Failed to load reading progress: ${e.message}")
            }
        }
    }
    
    /**
     * Start a new reading session
     */
    fun startReadingSession() {
        _readingSessionStartTime.value = System.currentTimeMillis()
        
        // Start analytics tracking
        viewModelScope.launch {
            val book = _currentBook.value
            val chapterIndex = _currentChapterIndex.value
            
            if (book != null) {
                try {
                    currentSessionId = analyticsRepository.startReadingSession(
                        bookId = book.id,
                        chapterIndex = chapterIndex,
                        pageIndex = 0, // Using 0 as default since we don't have page tracking
                        scrollPosition = 0
                    )
                    sessionStartTime = System.currentTimeMillis()
                    wordsReadInSession = 0
                } catch (e: Exception) {
                    // Log error but don't interrupt reading
                }
            }
        }
    }
    
    /**
     * End the current reading session and save progress
     */
    fun endReadingSession() {
        _readingSessionStartTime.value = null
        saveCurrentReadingProgress()
        
        // End analytics tracking
        viewModelScope.launch {
            val chapterIndex = _currentChapterIndex.value
            val sessionId = currentSessionId
            
            if (sessionId != null) {
                try {
                    analyticsRepository.endReadingSession(
                        sessionId = sessionId,
                        endChapter = chapterIndex,
                        endPage = 0, // Using 0 as default since we don't have page tracking
                        endScrollPosition = 0,
                        wordsRead = wordsReadInSession,
                        charactersRead = wordsReadInSession * 5 // Rough estimation
                    )
                    currentSessionId = null
                    wordsReadInSession = 0
                } catch (e: Exception) {
                    // Log error but don't interrupt reading
                }
            }
        }
    }
    
    /**
     * Update scroll position (called from UI)
     */
    fun updateScrollPosition(scrollPosition: Int) {
        _lastScrollPosition.value = scrollPosition
        // Auto-save progress periodically
        saveCurrentReadingProgress()
    }
    
    /**
     * Update page number (called from UI)
     */
    fun updatePageNumber(pageNumber: Int) {
        _currentPageNumber.value = pageNumber
        saveCurrentReadingProgress()
    }
    
    /**
     * Save current reading progress to database
     */
    private fun saveCurrentReadingProgress() {
        val book = _currentBook.value ?: return
        val epub = _parsedEpub.value ?: return
        val chapterIndex = _currentChapterIndex.value
        val chapter = epub.chapters.getOrNull(chapterIndex) ?: return
        
        viewModelScope.launch {
            try {
                val sessionStartTime = _readingSessionStartTime.value
                val readingTime = if (sessionStartTime != null) {
                    (System.currentTimeMillis() - sessionStartTime) / 1000
                } else 0L
                
                val progress = ReadingProgress(
                    bookId = book.id,
                    chapterIndex = chapterIndex,
                    chapterTitle = chapter.title,
                    scrollPosition = _lastScrollPosition.value,
                    pageNumber = _currentPageNumber.value,
                    totalPagesInChapter = 1, // This could be calculated based on content length
                    readingTimeSeconds = readingTime,
                    lastUpdated = System.currentTimeMillis(),
                    isChapterCompleted = false // Could be determined by scroll position
                )
                
                // Use upsert to maintain one progress entry per chapter
                readingProgressRepository.upsertProgressForChapter(progress)
                _currentReadingProgress.value = progress
                
            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "Failed to save reading progress: ${e.message}")
            }
        }
    }
    
    /**
     * Mark current chapter as completed
     */
    fun markChapterCompleted() {
        val book = _currentBook.value ?: return
        val epub = _parsedEpub.value ?: return
        val chapterIndex = _currentChapterIndex.value
        val chapter = epub.chapters.getOrNull(chapterIndex) ?: return
        
        viewModelScope.launch {
            try {
                val sessionStartTime = _readingSessionStartTime.value
                val readingTime = if (sessionStartTime != null) {
                    (System.currentTimeMillis() - sessionStartTime) / 1000
                } else 0L
                
                val progress = ReadingProgress(
                    bookId = book.id,
                    chapterIndex = chapterIndex,
                    chapterTitle = chapter.title,
                    scrollPosition = _lastScrollPosition.value,
                    pageNumber = _currentPageNumber.value,
                    totalPagesInChapter = _currentPageNumber.value,
                    readingTimeSeconds = readingTime,
                    lastUpdated = System.currentTimeMillis(),
                    isChapterCompleted = true
                )
                
                readingProgressRepository.saveProgress(progress)
                _currentReadingProgress.value = progress
                
            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "Failed to mark chapter as completed: ${e.message}")
            }
        }
    }
    
    /**
     * Get reading statistics for the current book
     */
    suspend fun getReadingStats(): ReadingStats? {
        val book = _currentBook.value ?: return null
        return readingProgressRepository.getReadingStatsForBook(book.id)
    }
    
    /**
     * Get total reading time for the current book
     */
    suspend fun getTotalReadingTime(): Long {
        val book = _currentBook.value ?: return 0L
        return readingProgressRepository.getTotalReadingTimeForBook(book.id)
    }
    
    /**
     * Load book with reading progress support
     */
    fun loadBook(book: com.bsikar.helix.data.model.Book) {
        if (_currentBook.value?.id == book.id) {
            // Book already loaded, just update the current book reference
            _currentBook.value = book
            return
        }
        
        _currentBook.value = book
        
        // Load reading progress first, then set chapter index
        loadReadingProgress(book.id)
        
        if (book.isImported) {
            loadEpubContent(book)
        } else {
            // For non-imported books, clear EPUB data
            _parsedEpub.value = null
            _currentContent.value = emptyList()
        }
        
        // Start reading session
        startReadingSession()
    }
    
    /**
     * Load chapters from database for navigation
     */
    private suspend fun loadChaptersForBook(bookId: String) {
        try {
            val chapters = chapterRepository.getChaptersForBook(bookId)
            val tableOfContents = chapterRepository.getTableOfContents(bookId)
            
            _chapters.value = chapters
            _tableOfContents.value = tableOfContents
            
            // Update current chapter based on index
            val currentIndex = _currentChapterIndex.value
            if (currentIndex < chapters.size) {
                _currentChapter.value = chapters[currentIndex]
            }
            
            // Calculate reading progress percentage
            val progressPercentage = if (chapters.isNotEmpty()) {
                chapterRepository.getProgressPercentage(bookId, currentIndex)
            } else 0f
            _readingProgressPercentage.value = progressPercentage
            
        } catch (e: Exception) {
            // Log error but don't fail the whole loading process
            android.util.Log.e("ReaderViewModel", "Failed to load chapters for book $bookId: ${e.message}", e)
        }
    }
    
    /**
     * Store chapters in database from parsed EPUB
     */
    private suspend fun storeChaptersForBook(bookId: String, parsedEpub: ParsedEpub) {
        try {
            val success = chapterRepository.storeChaptersFromEpub(bookId, parsedEpub)
            if (success) {
                // Load the stored chapters
                loadChaptersForBook(bookId)
            }
        } catch (e: Exception) {
            android.util.Log.e("ReaderViewModel", "Failed to store chapters for book $bookId: ${e.message}", e)
        }
    }
    
    /**
     * Navigate to specific chapter
     */
    fun navigateToChapter(chapter: EpubChapter) {
        val currentEpub = _parsedEpub.value ?: return
        val chapters = _chapters.value
        
        // Find the chapter index
        val chapterIndex = chapters.indexOfFirst { it.id == chapter.id || it.href == chapter.href }
        if (chapterIndex >= 0) {
            _currentChapterIndex.value = chapterIndex
            _currentChapter.value = chapter
            
            // Load content for this chapter
            loadChapterContent(currentEpub, chapterIndex)
            
            // Update reading progress
            saveCurrentReadingProgress()
        }
    }
    
    /**
     * Navigate to previous chapter
     */
    fun navigateToPreviousChapter() {
        val currentIndex = _currentChapterIndex.value
        val chapters = _chapters.value
        
        if (currentIndex > 0) {
            val previousChapter = chapters[currentIndex - 1]
            navigateToChapter(previousChapter)
        }
    }
    
    /**
     * Navigate to next chapter
     */
    fun navigateToNextChapter() {
        val currentIndex = _currentChapterIndex.value
        val chapters = _chapters.value
        
        if (currentIndex < chapters.size - 1) {
            val nextChapter = chapters[currentIndex + 1]
            navigateToChapter(nextChapter)
        }
    }
    
    /**
     * Check if there's a previous chapter available
     */
    fun hasPreviousChapter(): Boolean {
        return _currentChapterIndex.value > 0
    }
    
    /**
     * Check if there's a next chapter available
     */
    fun hasNextChapter(): Boolean {
        val currentIndex = _currentChapterIndex.value
        val chapters = _chapters.value
        return currentIndex < chapters.size - 1
    }

    /**
     * Update reading progress and track words read
     */
    fun updateReadingProgress(additionalWords: Int) {
        if (currentSessionId != null) {
            wordsReadInSession += additionalWords
        }
    }

    /**
     * Get current reading speed for this session
     */
    fun getCurrentReadingSpeed(): Double {
        if (currentSessionId == null || sessionStartTime == 0L) return 0.0
        
        return analyticsRepository.calculateReadingSpeed(
            wordsReadInSession,
            sessionStartTime
        )
    }

    /**
     * Check if a reading session is currently active
     */
    fun isSessionActive(): Boolean {
        return currentSessionId != null
    }
}