package com.bsikar.helix.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.source.dao.BookDao
import com.bsikar.helix.data.source.entities.BookEntity
import com.bsikar.helix.data.mapper.toBook
import com.bsikar.helix.data.mapper.toBookEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookRepositoryImplTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var bookDao: BookDao
    private lateinit var repository: BookRepositoryImpl

    // Test data
    private val testBookEntity = BookEntity(
        id = "test-book-1",
        title = "Test Book",
        author = "Test Author",
        filePath = "/path/test.epub",
        coverColor = 0xFF000000L,
        progress = 0.5f,
        currentChapter = 2,
        currentPage = 10,
        totalPages = 100,
        totalChapters = 5,
        lastReadTimestamp = 123456789L,
        tags = "fiction,adventure",
        isImported = true,
        coverImagePath = null,
        scrollPosition = 0,
        originalUri = null
    )

    private val testBook = testBookEntity.toBook()

    private val testBookEntities = listOf(
        testBookEntity,
        testBookEntity.copy(
            id = "test-book-2",
            title = "Test Book 2",
            progress = 0.0f,
            explicitReadingStatus = "PLAN_TO_READ"
        ),
        testBookEntity.copy(
            id = "test-book-3", 
            title = "Test Book 3",
            progress = 1.0f
        )
    )

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        bookDao = mockk<BookDao>(relaxed = true)
        repository = BookRepositoryImpl(bookDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAllBooksFlow should return mapped books from dao`() = runTest {
        every { bookDao.getAllBooksFlow() } returns MutableStateFlow(testBookEntities)
        
        val result = repository.getAllBooksFlow().first()
        
        assertEquals(3, result.size)
        assertEquals("Test Book", result[0].title)
        assertEquals("Test Book 2", result[1].title)
        assertEquals("Test Book 3", result[2].title)
    }

    @Test
    fun `getAllBooks should return mapped books from dao`() = runTest {
        coEvery { bookDao.getAllBooks() } returns testBookEntities
        
        val result = repository.getAllBooks()
        
        assertEquals(3, result.size)
        assertEquals("Test Book", result[0].title)
        coVerify { bookDao.getAllBooks() }
    }

    @Test
    fun `getBookById should return mapped book when found`() = runTest {
        coEvery { bookDao.getBookById("test-book-1") } returns testBookEntity
        
        val result = repository.getBookById("test-book-1")
        
        assertNotNull(result)
        assertEquals("Test Book", result?.title)
        assertEquals("test-book-1", result?.id)
        coVerify { bookDao.getBookById("test-book-1") }
    }

    @Test
    fun `getBookById should return null when not found`() = runTest {
        coEvery { bookDao.getBookById("nonexistent") } returns null
        
        val result = repository.getBookById("nonexistent")
        
        assertNull(result)
        coVerify { bookDao.getBookById("nonexistent") }
    }

    @Test
    fun `insertBook should call dao with mapped entity`() = runTest {
        repository.insertBook(testBook)
        
        coVerify { bookDao.insertBook(testBook.toBookEntity()) }
    }

    @Test
    fun `updateBook should call dao with mapped entity`() = runTest {
        repository.updateBook(testBook)
        
        coVerify { bookDao.updateBook(testBook.toBookEntity()) }
    }

    @Test
    fun `deleteBook should call dao with book id`() = runTest {
        repository.deleteBook("test-book-1")
        
        coVerify { bookDao.deleteBookById("test-book-1") }
    }

    @Test
    fun `deleteAllBooks should call dao deleteAllBooks`() = runTest {
        repository.deleteAllBooks()
        
        coVerify { bookDao.deleteAllBooks() }
    }

    @Test
    fun `getReadingBooksFlow should return reading books only`() = runTest {
        val readingBooks = testBookEntities.filter { it.progress > 0 && it.progress < 1 }
        every { bookDao.getReadingBooksFlow() } returns MutableStateFlow(readingBooks)
        
        val result = repository.getReadingBooksFlow().first()
        
        assertEquals(1, result.size)
        assertEquals(ReadingStatus.READING, result[0].readingStatus)
        assertEquals("Test Book", result[0].title)
    }

    @Test
    fun `getPlanToReadBooksFlow should return plan to read books only`() = runTest {
        val planToReadBooks = testBookEntities.filter { it.progress == 0.0f }
        every { bookDao.getPlanToReadBooksFlow() } returns MutableStateFlow(planToReadBooks)
        
        val result = repository.getPlanToReadBooksFlow().first()
        
        assertEquals(1, result.size)
        assertEquals(ReadingStatus.PLAN_TO_READ, result[0].readingStatus)
        assertEquals("Test Book 2", result[0].title)
    }

    @Test
    fun `getCompletedBooksFlow should return completed books only`() = runTest {
        val completedBooks = testBookEntities.filter { it.progress >= 1.0f }
        every { bookDao.getCompletedBooksFlow() } returns MutableStateFlow(completedBooks)
        
        val result = repository.getCompletedBooksFlow().first()
        
        assertEquals(1, result.size)
        assertEquals(ReadingStatus.COMPLETED, result[0].readingStatus)
        assertEquals("Test Book 3", result[0].title)
    }

    @Test
    fun `getRecentBooksFlow should return recent books from dao`() = runTest {
        val recentBooks = testBookEntities.filter { it.lastReadTimestamp > 0 }
        every { bookDao.getRecentBooksFlow() } returns MutableStateFlow(recentBooks)
        
        val result = repository.getRecentBooksFlow().first()
        
        assertEquals(3, result.size) // All test books have lastReadTimestamp > 0
        assertTrue(result.all { it.lastReadTimestamp > 0 })
    }

    @Test
    fun `repository should handle empty book list`() = runTest {
        every { bookDao.getAllBooksFlow() } returns MutableStateFlow(emptyList())
        
        val result = repository.getAllBooksFlow().first()
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `repository should handle dao exceptions gracefully`() = runTest {
        coEvery { bookDao.getAllBooks() } throws RuntimeException("Database error")
        
        try {
            repository.getAllBooks()
            fail("Expected exception to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Database error", e.message)
        }
    }

    @Test
    fun `flow operations should preserve reactive behavior`() = runTest {
        val flow = MutableStateFlow(testBookEntities.take(1))
        every { bookDao.getAllBooksFlow() } returns flow
        
        val repositoryFlow = repository.getAllBooksFlow()
        
        // Initial value
        assertEquals(1, repositoryFlow.first().size)
        
        // Update the source flow
        flow.value = testBookEntities
        
        // Should reflect the change
        assertEquals(3, repositoryFlow.first().size)
    }

    @Test
    fun `mapping between entity and model should be consistent`() = runTest {
        // Test round trip conversion
        val originalBook = testBook
        val entity = originalBook.toBookEntity()
        val convertedBack = entity.toBook()
        
        assertEquals(originalBook.id, convertedBack.id)
        assertEquals(originalBook.title, convertedBack.title)
        assertEquals(originalBook.author, convertedBack.author)
        assertEquals(originalBook.readingStatus, convertedBack.readingStatus)
        assertEquals(originalBook.progress, convertedBack.progress, 0.001f)
        assertEquals(originalBook.currentChapter, convertedBack.currentChapter)
        assertEquals(originalBook.currentPage, convertedBack.currentPage)
    }
}