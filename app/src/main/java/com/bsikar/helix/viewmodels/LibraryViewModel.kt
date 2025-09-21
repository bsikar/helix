package com.bsikar.helix.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.BookRepository
import com.bsikar.helix.data.ReadingStatus
import com.bsikar.helix.data.Tag
import com.bsikar.helix.data.PresetTags
import com.bsikar.helix.data.TagMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class LibraryViewModel : ViewModel() {

    private val _allBooks = MutableStateFlow<List<Book>>(emptyList())
    val allBooks: StateFlow<List<Book>> = _allBooks.asStateFlow()

    init {
        loadBooks()
    }

    private fun loadBooks() {
        _allBooks.value = BookRepository.getAllBooks()
    }

    // Reading Books (currently reading)
    val readingBooks: StateFlow<List<Book>> = _allBooks.map { books ->
        books.filter { it.readingStatus == ReadingStatus.READING }
             .sortedByDescending { it.lastReadTimestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Plan to Read Books
    val planToReadBooks: StateFlow<List<Book>> = _allBooks.map { books ->
        books.filter { it.readingStatus == ReadingStatus.PLAN_TO_READ }
             .sortedBy { it.title }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Completed Books
    val completedBooks: StateFlow<List<Book>> = _allBooks.map { books ->
        books.filter { it.readingStatus == ReadingStatus.COMPLETED }
             .sortedBy { it.title }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Recent Books (books that have been read recently)
    val recentBooks: StateFlow<List<Book>> = _allBooks.map { books ->
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
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    book.copy(
                        progress = newProgress.coerceIn(0f, 1f),
                        lastReadTimestamp = System.currentTimeMillis()
                    )
                } else {
                    book
                }
            }
        }
    }

    /**
     * Marks a book as started reading (sets progress to a small value if it was 0)
     */
    fun startReading(bookId: String) {
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId && book.progress == 0f) {
                    book.copy(
                        progress = 0.05f, // Small progress to indicate started (5%)
                        lastReadTimestamp = System.currentTimeMillis()
                    )
                } else {
                    book
                }
            }
        }
    }
    
    /**
     * Moves a book back to Plan to Read status
     */
    fun moveToplanToRead(bookId: String) {
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    book.copy(
                        progress = 0f,
                        lastReadTimestamp = 0L
                    )
                } else {
                    book
                }
            }
        }
    }
    
    /**
     * Sets a specific progress for a book (between 0 and 1)
     */
    fun setBookProgress(bookId: String, progress: Float) {
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    book.copy(
                        progress = progress.coerceIn(0f, 1f),
                        lastReadTimestamp = if (progress > 0f) System.currentTimeMillis() else 0L
                    )
                } else {
                    book
                }
            }
        }
    }

    /**
     * Marks a book as completed
     */
    fun markAsCompleted(bookId: String) {
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    book.copy(
                        progress = 1.0f,
                        lastReadTimestamp = System.currentTimeMillis()
                    )
                } else {
                    book
                }
            }
        }
    }

    /**
     * Adds a new book to the library
     */
    fun addBook(book: Book) {
        _allBooks.update { currentBooks ->
            currentBooks + book
        }
    }

    /**
     * Removes a book from the library
     */
    fun removeBook(bookId: String) {
        _allBooks.update { currentBooks ->
            currentBooks.filter { it.id != bookId }
        }
    }

    /**
     * Gets a specific book by its ID
     */
    fun getBookById(bookId: String): Book? {
        return _allBooks.value.find { it.id == bookId }
    }

    /**
     * Simulates reading progress update - useful for demo purposes
     */
    fun simulateReadingProgress(bookId: String) {
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    val newProgress = (book.progress + 0.1f).coerceAtMost(1.0f)
                    book.copy(
                        progress = newProgress,
                        lastReadTimestamp = System.currentTimeMillis()
                    )
                } else {
                    book
                }
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
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    val newProgress = currentPage.toFloat() / book.totalPages
                    book.copy(
                        currentPage = currentPage,
                        currentChapter = currentChapter,
                        scrollPosition = scrollPosition,
                        progress = newProgress.coerceIn(0f, 1f),
                        lastReadTimestamp = System.currentTimeMillis()
                    )
                } else {
                    book
                }
            }
        }
    }

    /**
     * Updates the tags for a specific book
     */
    fun updateBookTags(bookId: String, newTags: List<String>) {
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    book.copy(tags = newTags)
                } else {
                    book
                }
            }
        }
    }

    /**
     * Add a tag to a book
     */
    fun addTagToBook(bookId: String, tagId: String) {
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId && !book.hasTag(tagId)) {
                    book.copy(tags = book.tags + tagId)
                } else {
                    book
                }
            }
        }
    }

    /**
     * Remove a tag from a book
     */
    fun removeTagFromBook(bookId: String, tagId: String) {
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    book.copy(tags = book.tags.filter { it != tagId })
                } else {
                    book
                }
            }
        }
    }

    /**
     * Get all books that have any of the specified tags
     */
    fun getBooksByTags(tagIds: List<String>): List<Book> {
        return _allBooks.value.filter { book ->
            book.hasAnyTag(tagIds)
        }
    }

    /**
     * Get all unique tags used across all books
     */
    fun getAllUsedTags(): List<Tag> {
        val usedTagIds = _allBooks.value
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
        
        _allBooks.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    book.copy(
                        tags = tagIds,
                        originalMetadataTags = metadataTags
                    )
                } else {
                    book
                }
            }
        }
    }

    /**
     * Refreshes the book list from the repository
     */
    fun refreshBooks() {
        loadBooks()
    }
}