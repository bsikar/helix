package com.bsikar.helix.data.source.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.source.AppDatabase
import com.bsikar.helix.data.source.entities.BookEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var bookDao: BookDao

    // Test data
    private val testBook1 = BookEntity(
        id = "book-1",
        title = "Test Book 1",
        author = "Author 1",
        filePath = "/path/book1.epub",
        readingStatus = ReadingStatus.READING,
        progress = 0.5f,
        currentChapter = 2,
        currentPage = 50,
        totalPages = 100,
        totalChapters = 10,
        lastReadTimestamp = 1000L,
        tags = listOf("fiction", "adventure"),
        isImported = true,
        coverImagePath = null,
        userColor = null,
        scrollPosition = 0,
        originalUri = null
    )

    private val testBook2 = BookEntity(
        id = "book-2", 
        title = "Test Book 2",
        author = "Author 2",
        filePath = "/path/book2.epub",
        readingStatus = ReadingStatus.PLAN_TO_READ,
        progress = 0.0f,
        currentChapter = 1,
        currentPage = 1,
        totalPages = 200,
        totalChapters = 15,
        lastReadTimestamp = 0L,
        tags = listOf("science", "non-fiction"),
        isImported = false,
        coverImagePath = null,
        userColor = null,
        scrollPosition = 0,
        originalUri = null
    )

    private val testBook3 = BookEntity(
        id = "book-3",
        title = "Test Book 3", 
        author = "Author 3",
        filePath = "/path/book3.epub",
        readingStatus = ReadingStatus.COMPLETED,
        progress = 1.0f,
        currentChapter = 20,
        currentPage = 300,
        totalPages = 300,
        totalChapters = 20,
        lastReadTimestamp = 2000L,
        tags = listOf("fantasy", "magic"),
        isImported = true,
        coverImagePath = null,
        userColor = null,
        scrollPosition = 0,
        originalUri = null
    )

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        bookDao = database.bookDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetBook() = runTest {
        bookDao.insertBook(testBook1)
        
        val retrieved = bookDao.getBookById("book-1")
        
        assertNotNull(retrieved)
        assertEquals(testBook1.id, retrieved?.id)
        assertEquals(testBook1.title, retrieved?.title)
        assertEquals(testBook1.author, retrieved?.author)
        assertEquals(testBook1.progress, retrieved?.progress)
    }

    @Test
    fun getAllBooks() = runTest {
        bookDao.insertBooks(listOf(testBook1, testBook2, testBook3))
        
        val books = bookDao.getAllBooks()
        
        assertEquals(3, books.size)
        assertTrue(books.any { it.id == "book-1" })
        assertTrue(books.any { it.id == "book-2" })
        assertTrue(books.any { it.id == "book-3" })
    }

    @Test
    fun getAllBooksFlow() = runTest {
        bookDao.insertBooks(listOf(testBook1, testBook2))
        
        val books = bookDao.getAllBooksFlow().first()
        
        assertEquals(2, books.size)
    }

    @Test
    fun getReadingBooksFlow() = runTest {
        bookDao.insertBooks(listOf(testBook1, testBook2, testBook3))
        
        val readingBooks = bookDao.getReadingBooksFlow().first()
        
        assertEquals(1, readingBooks.size)
        assertEquals("book-1", readingBooks[0].id)
        assertEquals(ReadingStatus.READING, readingBooks[0].readingStatus)
        assertTrue(readingBooks[0].progress > 0 && readingBooks[0].progress < 1)
    }

    @Test
    fun getPlanToReadBooksFlow() = runTest {
        bookDao.insertBooks(listOf(testBook1, testBook2, testBook3))
        
        val planToReadBooks = bookDao.getPlanToReadBooksFlow().first()
        
        assertEquals(1, planToReadBooks.size)
        assertEquals("book-2", planToReadBooks[0].id)
        assertEquals(ReadingStatus.PLAN_TO_READ, planToReadBooks[0].readingStatus)
        assertEquals(0.0f, planToReadBooks[0].progress)
    }

    @Test
    fun getCompletedBooksFlow() = runTest {
        bookDao.insertBooks(listOf(testBook1, testBook2, testBook3))
        
        val completedBooks = bookDao.getCompletedBooksFlow().first()
        
        assertEquals(1, completedBooks.size)
        assertEquals("book-3", completedBooks[0].id)
        assertEquals(ReadingStatus.COMPLETED, completedBooks[0].readingStatus)
        assertEquals(1.0f, completedBooks[0].progress)
    }

    @Test
    fun getRecentBooksFlow() = runTest {
        bookDao.insertBooks(listOf(testBook1, testBook2, testBook3))
        
        val recentBooks = bookDao.getRecentBooksFlow().first()
        
        // Should return books with lastReadTimestamp > 0, ordered by timestamp DESC
        assertEquals(2, recentBooks.size)
        assertTrue(recentBooks.all { it.lastReadTimestamp > 0 })
        // Should be ordered by timestamp descending
        assertTrue(recentBooks[0].lastReadTimestamp >= recentBooks[1].lastReadTimestamp)
    }

    @Test
    fun updateBook() = runTest {
        bookDao.insertBook(testBook1)
        
        val updatedBook = testBook1.copy(
            title = "Updated Title",
            progress = 0.75f,
            currentPage = 75
        )
        bookDao.updateBook(updatedBook)
        
        val retrieved = bookDao.getBookById("book-1")
        
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved?.title)
        assertEquals(0.75f, retrieved?.progress)
        assertEquals(75, retrieved?.currentPage)
    }

    @Test
    fun deleteBookById() = runTest {
        bookDao.insertBook(testBook1)
        
        // Verify book exists
        assertNotNull(bookDao.getBookById("book-1"))
        
        bookDao.deleteBookById("book-1")
        
        // Verify book is deleted
        assertNull(bookDao.getBookById("book-1"))
    }

    @Test
    fun deleteBook() = runTest {
        bookDao.insertBook(testBook1)
        
        // Verify book exists
        assertNotNull(bookDao.getBookById("book-1"))
        
        bookDao.deleteBook(testBook1)
        
        // Verify book is deleted
        assertNull(bookDao.getBookById("book-1"))
    }

    @Test
    fun deleteAllBooks() = runTest {
        bookDao.insertBooks(listOf(testBook1, testBook2, testBook3))
        
        // Verify books exist
        assertEquals(3, bookDao.getAllBooks().size)
        
        bookDao.deleteAllBooks()
        
        // Verify all books are deleted
        assertEquals(0, bookDao.getAllBooks().size)
    }

    @Test
    fun insertBookWithConflictReplace() = runTest {
        bookDao.insertBook(testBook1)
        
        // Insert book with same ID but different data
        val duplicateBook = testBook1.copy(title = "Duplicate Title")
        bookDao.insertBook(duplicateBook)
        
        val retrieved = bookDao.getBookById("book-1")
        
        // Should have replaced the original
        assertEquals("Duplicate Title", retrieved?.title)
        assertEquals(1, bookDao.getAllBooks().size) // Should still be only 1 book
    }

    @Test
    fun queryByNonExistentId() = runTest {
        val retrieved = bookDao.getBookById("nonexistent")
        
        assertNull(retrieved)
    }

    @Test
    fun insertBooksInBatch() = runTest {
        val books = listOf(testBook1, testBook2, testBook3)
        bookDao.insertBooks(books)
        
        val allBooks = bookDao.getAllBooks()
        
        assertEquals(3, allBooks.size)
        assertEquals(books.map { it.id }.sorted(), allBooks.map { it.id }.sorted())
    }

    @Test
    fun readingStatusFiltering() = runTest {
        // Create books with different reading statuses
        val reading1 = testBook1.copy(id = "reading1", progress = 0.3f)
        val reading2 = testBook1.copy(id = "reading2", progress = 0.7f)
        val planToRead = testBook2.copy(id = "plan", progress = 0.0f)
        val completed = testBook3.copy(id = "completed", progress = 1.0f)
        
        bookDao.insertBooks(listOf(reading1, reading2, planToRead, completed))
        
        // Test reading books (0 < progress < 1)
        val readingBooks = bookDao.getReadingBooksFlow().first()
        assertEquals(2, readingBooks.size)
        assertTrue(readingBooks.all { it.progress > 0 && it.progress < 1 })
        
        // Test plan to read books (progress = 0)
        val planToReadBooks = bookDao.getPlanToReadBooksFlow().first()
        assertEquals(1, planToReadBooks.size)
        assertEquals(0.0f, planToReadBooks[0].progress)
        
        // Test completed books (progress >= 1)
        val completedBooks = bookDao.getCompletedBooksFlow().first()
        assertEquals(1, completedBooks.size)
        assertEquals(1.0f, completedBooks[0].progress)
    }

    @Test
    fun orderingVerification() = runTest {
        // Create books with different timestamps and titles
        val book1 = testBook1.copy(id = "1", title = "Z Book", lastReadTimestamp = 1000L)
        val book2 = testBook2.copy(id = "2", title = "A Book", lastReadTimestamp = 0L)
        val book3 = testBook3.copy(id = "3", title = "M Book", lastReadTimestamp = 2000L)
        
        bookDao.insertBooks(listOf(book1, book2, book3))
        
        // Test recent books ordering (by timestamp DESC)
        val recentBooks = bookDao.getRecentBooksFlow().first()
        val recentTimestamps = recentBooks.map { it.lastReadTimestamp }
        assertEquals(listOf(2000L, 1000L), recentTimestamps) // DESC order, excluding 0
        
        // Test plan to read ordering (by title ASC)
        val planToReadBooks = bookDao.getPlanToReadBooksFlow().first()
        assertEquals(listOf("A Book"), planToReadBooks.map { it.title })
        
        // Test completed ordering (by title ASC)
        val completedBooks = bookDao.getCompletedBooksFlow().first()
        assertEquals(listOf("M Book"), completedBooks.map { it.title })
    }
}