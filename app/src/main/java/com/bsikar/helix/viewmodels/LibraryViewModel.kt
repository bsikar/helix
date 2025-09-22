package com.bsikar.helix.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.BookRepository
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.data.ReadingStatus
import com.bsikar.helix.data.Tag
import com.bsikar.helix.data.PresetTags
import com.bsikar.helix.data.TagMatcher
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

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = UserPreferencesManager(application)
    val libraryManager = LibraryManager(application, preferencesManager)
    
    // Convert LibraryManager's books state to StateFlow
    private val _allBooks = MutableStateFlow<List<Book>>(emptyList())
    val allBooks: StateFlow<List<Book>> = _allBooks.asStateFlow()
    
    init {
        // Observe LibraryManager's books state and update our StateFlow
        viewModelScope.launch {
            snapshotFlow { libraryManager.books.value }.collect { books ->
                _allBooks.value = books.toList() // Create a new list to ensure state change detection
            }
        }
    }

    // Reading Books (currently reading)
    val readingBooks: StateFlow<List<Book>> = allBooks.map { books ->
        books.filter { it.readingStatus == ReadingStatus.READING }
             .sortedByDescending { it.lastReadTimestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Plan to Read Books
    val planToReadBooks: StateFlow<List<Book>> = allBooks.map { books ->
        books.filter { it.readingStatus == ReadingStatus.PLAN_TO_READ }
             .sortedBy { it.title }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Completed Books
    val completedBooks: StateFlow<List<Book>> = allBooks.map { books ->
        books.filter { it.readingStatus == ReadingStatus.COMPLETED }
             .sortedBy { it.title }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Recent Books (books that have been read recently)
    val recentBooks: StateFlow<List<Book>> = allBooks.map { books ->
        books.filter { it.lastReadTimestamp > 0 }
             .sortedByDescending { it.lastReadTimestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Updates the progress of a specific book by its ID
     */
    fun updateBookProgress(bookId: String, newProgress: Float) {
        val book = libraryManager.getBookById(bookId)
        if (book != null) {
            val totalPages = if (book.totalChapters > 0) book.totalChapters * 20 else book.totalPages
            val currentPage = (newProgress * totalPages).toInt().coerceAtLeast(1)
            libraryManager.updateBookProgress(bookId, book.currentChapter, currentPage, book.scrollPosition)
        }
    }

    /**
     * Marks a book as started reading (sets progress to a small value if it was 0)
     */
    fun startReading(bookId: String) {
        updateBookProgress(bookId, 0.05f) // 5%
    }
    
    /**
     * Moves a book back to Plan to Read status
     */
    fun moveToplanToRead(bookId: String) {
        updateBookProgress(bookId, 0f)
    }
    
    /**
     * Sets a specific progress for a book (between 0 and 1)
     */
    fun setBookProgress(bookId: String, progress: Float) {
        updateBookProgress(bookId, progress)
    }

    /**
     * Marks a book as completed
     */
    fun markAsCompleted(bookId: String) {
        updateBookProgress(bookId, 1.0f)
    }

    /**
     * Adds a new book to the library
     */
    fun addBook(book: Book) {
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
    fun getBookById(bookId: String): Book? {
        return libraryManager.getBookById(bookId)
    }

    /**
     * Simulates reading progress update - useful for demo purposes
     */
    fun simulateReadingProgress(bookId: String) {
        val book = getBookById(bookId)
        if (book != null) {
            val newProgress = (book.progress + 0.1f).coerceAtMost(1.0f)
            updateBookProgress(bookId, newProgress)
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
     * Updates the tags for a specific book
     */
    fun updateBookTags(bookId: String, newTags: List<String>) {
        libraryManager.updateBookTags(bookId, newTags)
    }

    /**
     * Updates the settings for a specific book (cover display mode, user color, etc.)
     */
    fun updateBookSettings(updatedBook: Book) {
        libraryManager.updateBookSettings(updatedBook)
    }

    /**
     * Add a tag to a book
     */
    fun addTagToBook(bookId: String, tagId: String) {
        val book = getBookById(bookId)
        if (book != null && !book.hasTag(tagId)) {
            updateBookTags(bookId, book.tags + tagId)
        }
    }

    /**
     * Remove a tag from a book
     */
    fun removeTagFromBook(bookId: String, tagId: String) {
        val book = getBookById(bookId)
        if (book != null) {
            updateBookTags(bookId, book.tags.filter { it != tagId })
        }
    }

    /**
     * Get all books that have any of the specified tags
     */
    fun getBooksByTags(tagIds: List<String>): List<Book> {
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
        // LibraryManager handles persistence, so no need to reload
    }
}