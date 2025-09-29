package com.bsikar.helix.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.repository.BookRepository
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.model.Tag
import com.bsikar.helix.data.model.PresetTags
import com.bsikar.helix.data.model.TagMatcher
import com.bsikar.helix.data.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import com.bsikar.helix.data.model.UiState
import com.bsikar.helix.utils.safeCallWithUiState
import com.bsikar.helix.utils.toUiState
import com.bsikar.helix.ui.components.SearchUtils

@OptIn(FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    val libraryManager: LibraryManager
) : ViewModel() {
    
    // Convert LibraryManager's books state to StateFlow
    private val _allBooks = MutableStateFlow<List<com.bsikar.helix.data.model.Book>>(emptyList())
    val allBooks: StateFlow<List<com.bsikar.helix.data.model.Book>> = _allBooks.asStateFlow()
    
    // Search and filtering state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Sorting state
    private val _readingSortAscending = MutableStateFlow(false)
    val readingSortAscending: StateFlow<Boolean> = _readingSortAscending.asStateFlow()
    
    private val _onDeckSortAscending = MutableStateFlow(true)
    val onDeckSortAscending: StateFlow<Boolean> = _onDeckSortAscending.asStateFlow()
    
    private val _completedSortAscending = MutableStateFlow(true)
    val completedSortAscending: StateFlow<Boolean> = _completedSortAscending.asStateFlow()
    
    // Refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _scanMessage = MutableStateFlow("")
    val scanMessage: StateFlow<String> = _scanMessage.asStateFlow()
    
    // Error handling state
    private val _libraryState = MutableStateFlow<UiState<List<com.bsikar.helix.data.model.Book>>>(UiState.Loading)
    val libraryState: StateFlow<UiState<List<com.bsikar.helix.data.model.Book>>> = _libraryState.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Observe books directly from the database Flow for reliable updates
        viewModelScope.launch {
            libraryManager.getBooksFlow().collect { books ->
                println("LibraryViewModel: Received ${books.size} books from database flow")
                books.forEach { book ->
                    println("  - ${book.title}: status=${book.explicitReadingStatus}, progress=${book.progress}")
                }
                _allBooks.value = books
                // Update library state to Success once books are loaded
                _libraryState.value = if (books.isEmpty()) {
                    UiState.Success(emptyList())
                } else {
                    UiState.Success(books)
                }
            }
        }
        
        // Also observe the Compose state for UI updates during runtime
        viewModelScope.launch {
            snapshotFlow { libraryManager.books.value }.collect { books ->
                // Only update if different from current state to avoid loops
                if (_allBooks.value != books) {
                    _allBooks.value = books.toList()
                }
            }
        }
    }

    // Reading Books (currently reading)
    val readingBooks: StateFlow<List<com.bsikar.helix.data.model.Book>> = allBooks.map { books ->
        println("LibraryViewModel.readingBooks: Filtering ${books.size} books")
        books.forEach { book ->
            println("  Book ${book.title}: progress=${book.progress}, explicitStatus=${book.explicitReadingStatus}, readingStatus=${book.readingStatus}")
        }
        val reading = books.filter { it.readingStatus == ReadingStatus.READING || it.readingStatus == ReadingStatus.LISTENING }
             .sortedByDescending { it.lastReadTimestamp }
        println("  Result: ${reading.size} books in READING/LISTENING status")
        reading
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // On Deck Books
    val onDeckBooks: StateFlow<List<com.bsikar.helix.data.model.Book>> = allBooks.map { books ->
        books.filter { it.readingStatus == ReadingStatus.PLAN_TO_READ || it.readingStatus == ReadingStatus.PLAN_TO_LISTEN }
             .sortedBy { it.title }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Completed Books
    val completedBooks: StateFlow<List<com.bsikar.helix.data.model.Book>> = allBooks.map { books ->
        books.filter { it.readingStatus == ReadingStatus.COMPLETED }
             .sortedBy { it.title }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Recent Books (books that have been read recently)
    val recentBooks: StateFlow<List<com.bsikar.helix.data.model.Book>> = allBooks.map { books ->
        books.filter { it.lastReadTimestamp > 0 }
             .sortedByDescending { it.lastReadTimestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Debounced search query for better performance
    private val debouncedSearchQuery = searchQuery.debounce(300)

    // Filtered and sorted reading books
    val filteredReadingBooks: StateFlow<List<com.bsikar.helix.data.model.Book>> = combine(
        readingBooks,
        debouncedSearchQuery,
        readingSortAscending
    ) { books, query, sortAscending ->
        val filtered = if (query.isBlank()) {
            books
        } else {
            searchBooks(books, query)
        }
        // Sort properly by lastReadTimestamp instead of just reversing
        if (sortAscending) {
            filtered.sortedBy { it.lastReadTimestamp }
        } else {
            filtered.sortedByDescending { it.lastReadTimestamp }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered and sorted on deck books
    val filteredOnDeckBooks: StateFlow<List<com.bsikar.helix.data.model.Book>> = combine(
        onDeckBooks,
        debouncedSearchQuery,
        onDeckSortAscending
    ) { books, query, sortAscending ->
        val filtered = if (query.isBlank()) {
            books
        } else {
            searchBooks(books, query)
        }
        if (sortAscending) {
            filtered.sortedBy { it.title }
        } else {
            filtered.sortedByDescending { it.title }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered and sorted completed books
    val filteredCompletedBooks: StateFlow<List<com.bsikar.helix.data.model.Book>> = combine(
        completedBooks,
        debouncedSearchQuery,
        completedSortAscending
    ) { books, query, sortAscending ->
        val filtered = if (query.isBlank()) {
            books
        } else {
            searchBooks(books, query)
        }
        if (sortAscending) {
            filtered.sortedBy { it.title }
        } else {
            filtered.sortedByDescending { it.title }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Updates the progress of a specific book by its ID
     */
    fun updateBookProgress(bookId: String, newProgress: Float) {
        viewModelScope.launch {
            val book = libraryManager.getBookById(bookId)
            if (book != null) {
                val totalPages = if (book.totalChapters > 0) book.totalChapters * 20 else book.totalPages
                val currentPage = (newProgress * totalPages).toInt().coerceAtLeast(1)
                libraryManager.updateBookProgress(bookId, book.currentChapter, currentPage, book.scrollPosition)
            }
        }
    }

    /**
     * Marks a book as started reading (sets explicit reading status, preserves existing progress)
     */
    fun startReading(bookId: String) {
        // Find the book to determine if it's an audiobook or text book
        val book = _allBooks.value.find { it.id == bookId }
        val status = if (book?.isAudiobook() == true) {
            ReadingStatus.LISTENING
        } else {
            ReadingStatus.READING
        }
        
        // Only reset progress if the book hasn't been started before
        if (book != null) {
            val hasExistingProgress = if (book.isAudiobook()) {
                book.currentPositionMs > 0 || book.progress > 0f
            } else {
                book.progress > 0f
            }
            
            if (hasExistingProgress) {
                // Just update status, preserve existing progress
                libraryManager.updateBookStatus(bookId, status)
            } else {
                // New book, set status and reset progress to 0
                libraryManager.updateBookStatusAndProgress(bookId, status, 0.0f)
            }
        }
    }
    
    /**
     * Moves a book to On Deck status (explicitly set by user)
     */
    fun moveToOnDeck(bookId: String) {
        // Find the book to determine if it's an audiobook or text book
        val book = _allBooks.value.find { it.id == bookId }
        val status = if (book?.isAudiobook() == true) {
            ReadingStatus.PLAN_TO_LISTEN
        } else {
            ReadingStatus.PLAN_TO_READ
        }
        libraryManager.updateBookStatus(bookId, status)
    }
    
    /**
     * Removes a book from On Deck (moves back to unread status)
     */
    fun removeFromOnDeck(bookId: String) {
        libraryManager.updateBookStatus(bookId, ReadingStatus.UNREAD)
    }
    
    /**
     * Removes a book from library (resets to unread status, but keeps in browse and recents)
     */
    fun removeFromLibrary(bookId: String) {
        libraryManager.updateBookStatus(bookId, ReadingStatus.UNREAD)
        // Don't reset progress to preserve recents functionality and reading history
        // The UNREAD status is sufficient to remove it from active reading sections
    }
    
    /**
     * Sets a specific progress for a book (between 0 and 1)
     */
    fun setBookProgress(bookId: String, progress: Float) {
        libraryManager.updateBookProgressDirect(bookId, progress)
    }

    /**
     * Marks a book as completed
     */
    fun markAsCompleted(bookId: String) {
        libraryManager.updateBookProgressDirect(bookId, 1.0f)
        // No need to set explicit status - progress >= 1f will automatically make it COMPLETED
    }

    /**
     * Adds a new book to the library
     */
    fun addBook(book: com.bsikar.helix.data.model.Book) {
        // This would be handled by LibraryManager.importEpubFile
    }

    /**
     * Removes a book from the library
     */
    fun removeBook(bookId: String) {
        libraryManager.removeBook(bookId)
    }

    /**
     * Gets a specific book by its ID
     */
    suspend fun getBookById(bookId: String): com.bsikar.helix.data.model.Book? {
        return libraryManager.getBookById(bookId)
    }

    /**
     * Simulates reading progress update - useful for demo purposes
     */
    fun simulateReadingProgress(bookId: String) {
        viewModelScope.launch {
            val book = getBookById(bookId)
            if (book != null) {
                val newProgress = (book.progress + 0.1f).coerceAtMost(1.0f)
                updateBookProgress(bookId, newProgress)
            }
        }
    }

    /**
     * Updates the reading position for a book (page, chapter, scroll position)
     */
    fun updateReadingPosition(
        bookId: String, 
        currentPage: Int, 
        currentChapter: Int = 1, 
        scrollPosition: Int = 0
    ) {
        libraryManager.updateBookProgress(bookId, currentChapter, currentPage, scrollPosition)
    }

    /**
     * Updates the playback position for an audiobook
     */
    fun updateAudiobookProgress(
        bookId: String,
        positionMs: Long,
        playbackSpeed: Float
    ) {
        libraryManager.updateAudiobookProgress(bookId, positionMs, playbackSpeed)
    }

    /**
     * Updates the tags for a specific book
     */
    fun updateBookTags(bookId: String, newTags: List<String>) {
        libraryManager.updateBookTags(bookId, newTags)
    }

    /**
     * Updates the settings for a specific book (cover display mode, user color, etc.)
     */
    fun updateBookSettings(updatedBook: com.bsikar.helix.data.model.Book) {
        libraryManager.updateBookSettings(updatedBook)
    }

    /**
     * Add a tag to a book
     */
    fun addTagToBook(bookId: String, tagId: String) {
        viewModelScope.launch {
            val book = getBookById(bookId)
            if (book != null && !book.hasTag(tagId)) {
                updateBookTags(bookId, book.tags + tagId)
            }
        }
    }

    /**
     * Remove a tag from a book
     */
    fun removeTagFromBook(bookId: String, tagId: String) {
        viewModelScope.launch {
            val book = getBookById(bookId)
            if (book != null) {
                updateBookTags(bookId, book.tags.filter { it != tagId })
            }
        }
    }

    /**
     * Get all books that have any of the specified tags
     */
    fun getBooksByTags(tagIds: List<String>): List<com.bsikar.helix.data.model.Book> {
        return allBooks.value.filter { book ->
            book.hasAnyTag(tagIds)
        }
    }

    /**
     * Get all unique tags used across all books
     */
    fun getAllUsedTags(): List<Tag> {
        val usedTagIds = allBooks.value
            .flatMap { it.tags }
            .distinct()
        return usedTagIds.mapNotNull { tagId -> PresetTags.findTagById(tagId) }
    }

    /**
     * Parse metadata tags and update a book's tags
     */
    fun parseAndSetMetadataTags(bookId: String, metadataTags: List<String>) {
        val parsedTags = TagMatcher.parseMetadataTags(metadataTags)
        val tagIds = parsedTags.map { it.id }
        updateBookTags(bookId, tagIds)
    }

    /**
     * Refreshes the book list from the repository
     */
    fun refreshBooks() {
        viewModelScope.launch {
            // Force refresh from database
            val booksFromDb = libraryManager.getBookById("dummy") // This triggers a database read
            // The flow collector will automatically update _allBooks
            println("LibraryViewModel.refreshBooks: Triggered database refresh")
        }
    }

    // Search and filtering methods
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleReadingSortOrder() {
        _readingSortAscending.value = !_readingSortAscending.value
    }

    fun toggleOnDeckSortOrder() {
        _onDeckSortAscending.value = !_onDeckSortAscending.value
    }

    fun toggleCompletedSortOrder() {
        _completedSortAscending.value = !_completedSortAscending.value
    }

    // Refresh functionality
    fun refreshLibrary() {
        if (_isRefreshing.value) return
        
        _isRefreshing.value = true
        _scanMessage.value = "Scanning for new books..."
        
        libraryManager.rescanWatchedDirectoriesAsync { success, message, newCount ->
            _isRefreshing.value = false
            _scanMessage.value = if (success) {
                if (newCount > 0) "Found $newCount new books!" else "No new books found"
            } else {
                "Scan failed: $message"
            }
            
            // Clear message after 3 seconds
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                _scanMessage.value = ""
            }
        }
    }

    // Enhanced search logic with fuzzy matching
    private fun searchBooks(books: List<com.bsikar.helix.data.model.Book>, query: String): List<com.bsikar.helix.data.model.Book> {
        if (query.isBlank()) return books
        
        // Use fuzzy search for better matching
        val searchResults = SearchUtils.fuzzySearch(
            items = books,
            query = query,
            getText = { it.title },
            getSecondaryText = { it.author },
            threshold = 0.3 // Lower threshold for more inclusive search
        )
        
        return searchResults.map { it.item }
    }

    // Error handling methods
    fun clearError() {
        _errorMessage.value = null
    }

    private fun handleError(throwable: Throwable) {
        _errorMessage.value = throwable.message ?: "An unknown error occurred"
    }

    // Enhanced methods with error handling
    fun importEpubSafely(filePath: String) {
        viewModelScope.launch {
            _libraryState.value = UiState.Loading
            val result = libraryManager.importEpubFile(java.io.File(filePath)).toUiState()
            
            when (result) {
                is UiState.Error -> {
                    _libraryState.value = result
                    handleError(result.exception)
                }
                is UiState.Success -> {
                    _libraryState.value = UiState.Success(allBooks.value)
                    // Book was successfully imported, state will be updated via Flow
                }
                is UiState.Loading -> {
                    // Loading state already set above
                }
            }
        }
    }

    /**
     * Safely rescan watched directories with proper error handling
     */
    fun rescanWatchedDirectoriesSafely() {
        viewModelScope.launch {
            _libraryState.value = UiState.Loading
            _isRefreshing.value = true
            _scanMessage.value = "Scanning for new books..."
            
            val result = libraryManager.rescanWatchedDirectories().toUiState()
            
            when (result) {
                is UiState.Error -> {
                    _libraryState.value = result
                    _isRefreshing.value = false
                    _scanMessage.value = "Scan failed: ${result.exception.message}"
                    handleError(result.exception)
                }
                is UiState.Success -> {
                    _libraryState.value = UiState.Success(allBooks.value)
                    _isRefreshing.value = false
                    val newCount = result.data.size
                    _scanMessage.value = if (newCount > 0) "Found $newCount new books!" else "No new books found"
                    
                    // Clear scan message after 3 seconds
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _scanMessage.value = ""
                    }
                }
                is UiState.Loading -> {
                    // Loading state already set above
                }
            }
        }
    }

    /**
     * Safely import EPUBs from directory with proper error handling
     */
    fun importEpubsFromDirectorySafely(directoryPath: String) {
        viewModelScope.launch {
            _libraryState.value = UiState.Loading
            
            val result = libraryManager.importEpubsFromDirectory(java.io.File(directoryPath)).toUiState()
            
            when (result) {
                is UiState.Error -> {
                    _libraryState.value = result
                    handleError(result.exception)
                }
                is UiState.Success -> {
                    _libraryState.value = UiState.Success(allBooks.value)
                    val importedCount = result.data.size
                    _scanMessage.value = "Successfully imported $importedCount books!"
                    
                    // Clear message after 3 seconds
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _scanMessage.value = ""
                    }
                }
                is UiState.Loading -> {
                    // Loading state already set above
                }
            }
        }
    }
}