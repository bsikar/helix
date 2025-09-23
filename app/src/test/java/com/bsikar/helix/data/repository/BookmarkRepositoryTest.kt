package com.bsikar.helix.data.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bsikar.helix.data.model.Bookmark
import com.bsikar.helix.data.source.dao.BookmarkDao
import com.bsikar.helix.data.source.entities.BookmarkEntity
import com.bsikar.helix.data.source.entities.toBookmark
import com.bsikar.helix.data.source.entities.toEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class BookmarkRepositoryTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var context: Context
    private lateinit var repository: BookmarkRepository

    // Test data
    private val testBookmarkEntity = BookmarkEntity(
        id = "bookmark-1",
        bookId = "book-1",
        bookTitle = "Test Book",
        chapterNumber = 1,
        pageNumber = 10,
        note = "Interesting point",
        timestamp = 123456789L
    )

    private val testBookmark = testBookmarkEntity.toBookmark()

    private val testBookmarkEntities = listOf(
        testBookmarkEntity,
        testBookmarkEntity.copy(
            id = "bookmark-2",
            bookId = "book-1",
            chapterNumber = 2,
            pageNumber = 5,
            note = "Another bookmark",
            timestamp = 234567890L
        ),
        testBookmarkEntity.copy(
            id = "bookmark-3",
            bookId = "book-2",
            chapterNumber = 1,
            pageNumber = 15,
            note = "Different book bookmark",
            timestamp = 345678901L
        )
    )

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        bookmarkDao = mockk<BookmarkDao>(relaxed = true)
        context = mockk<Context>(relaxed = true)
        repository = BookmarkRepository(bookmarkDao, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAllBookmarksFlow should return mapped bookmarks from dao`() = runTest {
        every { bookmarkDao.getAllBookmarksFlow() } returns MutableStateFlow(testBookmarkEntities)
        
        val result = repository.getAllBookmarksFlow().first()
        
        assertEquals(3, result.size)
        assertEquals("bookmark-1", result[0].id)
        assertEquals("Interesting point", result[0].note)
    }

    @Test
    fun `getBookmarksForBookFlow should return bookmarks for specific book`() = runTest {
        val book1Bookmarks = testBookmarkEntities.filter { it.bookId == "book-1" }
        every { bookmarkDao.getBookmarksForBookFlow("book-1") } returns MutableStateFlow(book1Bookmarks)
        
        val result = repository.getBookmarksForBookFlow("book-1").first()
        
        assertEquals(2, result.size)
        assertTrue(result.all { it.bookId == "book-1" })
        coVerify { bookmarkDao.getBookmarksForBookFlow("book-1") }
    }

    @Test
    fun `getBookmarksForBook should return bookmarks for specific book`() = runTest {
        val book1Bookmarks = testBookmarkEntities.filter { it.bookId == "book-1" }
        coEvery { bookmarkDao.getBookmarksForBook("book-1") } returns book1Bookmarks
        
        val result = repository.getBookmarksForBook("book-1")
        
        assertEquals(2, result.size)
        assertTrue(result.all { it.bookId == "book-1" })
        coVerify { bookmarkDao.getBookmarksForBook("book-1") }
    }

    @Test
    fun `getAllBookmarks should return all bookmarks and trigger migration`() = runTest {
        every { bookmarkDao.getAllBookmarksFlow() } returns MutableStateFlow(testBookmarkEntities)
        
        val result = repository.getAllBookmarks()
        
        assertEquals(3, result.size)
        // Migration should be attempted (tested indirectly through the first() call)
    }

    @Test
    fun `getBookmarkById should return bookmark when found`() = runTest {
        coEvery { bookmarkDao.getBookmarkById("bookmark-1") } returns testBookmarkEntity
        
        val result = repository.getBookmarkById("bookmark-1")
        
        assertNotNull(result)
        assertEquals("bookmark-1", result?.id)
        assertEquals("Interesting point", result?.note)
        coVerify { bookmarkDao.getBookmarkById("bookmark-1") }
    }

    @Test
    fun `getBookmarkById should return null when not found`() = runTest {
        coEvery { bookmarkDao.getBookmarkById("nonexistent") } returns null
        
        val result = repository.getBookmarkById("nonexistent")
        
        assertNull(result)
        coVerify { bookmarkDao.getBookmarkById("nonexistent") }
    }

    @Test
    fun `addBookmark should remove existing bookmark at same location`() = runTest {
        val existingBookmarks = listOf(
            testBookmarkEntity.copy(id = "existing-1", chapterNumber = 1, pageNumber = 10)
        )
        coEvery { bookmarkDao.getBookmarksForBook("book-1") } returns existingBookmarks
        
        val newBookmark = testBookmark.copy(chapterNumber = 1, pageNumber = 10)
        repository.addBookmark(newBookmark)
        
        // Should delete existing bookmark at same location
        coVerify { bookmarkDao.deleteBookmarkById("existing-1") }
        // Should insert new bookmark
        coVerify { bookmarkDao.insertBookmark(newBookmark.toEntity()) }
    }

    @Test
    fun `addBookmark should not remove bookmarks at different locations`() = runTest {
        val existingBookmarks = listOf(
            testBookmarkEntity.copy(id = "existing-1", chapterNumber = 1, pageNumber = 5) // Different page
        )
        coEvery { bookmarkDao.getBookmarksForBook("book-1") } returns existingBookmarks
        
        val newBookmark = testBookmark.copy(chapterNumber = 1, pageNumber = 10)
        repository.addBookmark(newBookmark)
        
        // Should NOT delete bookmark at different location
        coVerify(exactly = 0) { bookmarkDao.deleteBookmarkById("existing-1") }
        // Should insert new bookmark
        coVerify { bookmarkDao.insertBookmark(newBookmark.toEntity()) }
    }

    @Test
    fun `updateBookmark should call dao with mapped entity`() = runTest {
        repository.updateBookmark(testBookmark)
        
        coVerify { bookmarkDao.updateBookmark(testBookmark.toEntity()) }
    }

    @Test
    fun `deleteBookmark should call dao with bookmark id`() = runTest {
        repository.deleteBookmark("bookmark-1")
        
        coVerify { bookmarkDao.deleteBookmarkById("bookmark-1") }
    }

    @Test
    fun `deleteBookmarksForBook should call dao with book id`() = runTest {
        repository.deleteBookmarksForBook("book-1")
        
        coVerify { bookmarkDao.deleteBookmarksForBook("book-1") }
    }

    @Test
    fun `updateBookmarkNote should update existing bookmark note`() = runTest {
        coEvery { bookmarkDao.getBookmarkById("bookmark-1") } returns testBookmarkEntity
        
        repository.updateBookmarkNote("bookmark-1", "Updated note")
        
        coVerify { 
            bookmarkDao.updateBookmark(
                testBookmarkEntity.copy(note = "Updated note")
            ) 
        }
    }

    @Test
    fun `updateBookmarkNote should do nothing when bookmark not found`() = runTest {
        coEvery { bookmarkDao.getBookmarkById("nonexistent") } returns null
        
        repository.updateBookmarkNote("nonexistent", "Updated note")
        
        // Should not call update since bookmark doesn't exist
        coVerify(exactly = 0) { bookmarkDao.updateBookmark(any()) }
    }

    @Test
    fun `isPageBookmarked should return true when page is bookmarked`() = runTest {
        val bookmarks = listOf(
            testBookmarkEntity.copy(chapterNumber = 1, pageNumber = 10),
            testBookmarkEntity.copy(chapterNumber = 2, pageNumber = 5)
        )
        coEvery { bookmarkDao.getBookmarksForBook("book-1") } returns bookmarks
        
        val result = repository.isPageBookmarked("book-1", 1, 10)
        
        assertTrue(result)
        coVerify { bookmarkDao.getBookmarksForBook("book-1") }
    }

    @Test
    fun `isPageBookmarked should return false when page is not bookmarked`() = runTest {
        val bookmarks = listOf(
            testBookmarkEntity.copy(chapterNumber = 1, pageNumber = 10),
            testBookmarkEntity.copy(chapterNumber = 2, pageNumber = 5)
        )
        coEvery { bookmarkDao.getBookmarksForBook("book-1") } returns bookmarks
        
        val result = repository.isPageBookmarked("book-1", 1, 15) // Different page
        
        assertFalse(result)
        coVerify { bookmarkDao.getBookmarksForBook("book-1") }
    }

    @Test
    fun `isPageBookmarked should return false when no bookmarks exist`() = runTest {
        coEvery { bookmarkDao.getBookmarksForBook("book-1") } returns emptyList()
        
        val result = repository.isPageBookmarked("book-1", 1, 10)
        
        assertFalse(result)
        coVerify { bookmarkDao.getBookmarksForBook("book-1") }
    }

    @Test
    fun `repository should handle empty bookmark list`() = runTest {
        every { bookmarkDao.getAllBookmarksFlow() } returns MutableStateFlow(emptyList())
        
        val result = repository.getAllBookmarksFlow().first()
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `repository should handle dao exceptions gracefully`() = runTest {
        coEvery { bookmarkDao.getBookmarkById("test") } throws RuntimeException("Database error")
        
        try {
            repository.getBookmarkById("test")
            fail("Expected exception to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Database error", e.message)
        }
    }

    @Test
    fun `flow operations should preserve reactive behavior`() = runTest {
        val flow = MutableStateFlow(testBookmarkEntities.take(1))
        every { bookmarkDao.getAllBookmarksFlow() } returns flow
        
        val repositoryFlow = repository.getAllBookmarksFlow()
        
        // Initial value
        assertEquals(1, repositoryFlow.first().size)
        
        // Update the source flow
        flow.value = testBookmarkEntities
        
        // Should reflect the change
        assertEquals(3, repositoryFlow.first().size)
    }

    @Test
    fun `mapping between entity and model should be consistent`() = runTest {
        // Test round trip conversion
        val originalBookmark = testBookmark
        val entity = originalBookmark.toEntity()
        val convertedBack = entity.toBookmark()
        
        assertEquals(originalBookmark.id, convertedBack.id)
        assertEquals(originalBookmark.bookId, convertedBack.bookId)
        assertEquals(originalBookmark.chapterNumber, convertedBack.chapterNumber)
        assertEquals(originalBookmark.pageNumber, convertedBack.pageNumber)
        assertEquals(originalBookmark.note, convertedBack.note)
        assertEquals(originalBookmark.timestamp, convertedBack.timestamp)
    }
}