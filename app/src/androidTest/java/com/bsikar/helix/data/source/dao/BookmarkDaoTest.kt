package com.bsikar.helix.data.source.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bsikar.helix.data.source.AppDatabase
import com.bsikar.helix.data.source.entities.BookmarkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var bookmarkDao: BookmarkDao

    // Test data
    private val testBookmark1 = BookmarkEntity(
        id = "bookmark-1",
        bookId = "book-1",
        chapterNumber = 1,
        pageNumber = 10,
        note = "Interesting quote",
        createdAt = 1000L
    )

    private val testBookmark2 = BookmarkEntity(
        id = "bookmark-2",
        bookId = "book-1",
        chapterNumber = 2,
        pageNumber = 5,
        note = "Important concept",
        createdAt = 2000L
    )

    private val testBookmark3 = BookmarkEntity(
        id = "bookmark-3",
        bookId = "book-2",
        chapterNumber = 1,
        pageNumber = 15,
        note = "Reference point",
        createdAt = 1500L
    )

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        bookmarkDao = database.bookmarkDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetBookmark() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        
        val retrieved = bookmarkDao.getBookmarkById("bookmark-1")
        
        assertNotNull(retrieved)
        assertEquals(testBookmark1.id, retrieved?.id)
        assertEquals(testBookmark1.bookId, retrieved?.bookId)
        assertEquals(testBookmark1.note, retrieved?.note)
        assertEquals(testBookmark1.chapterNumber, retrieved?.chapterNumber)
        assertEquals(testBookmark1.pageNumber, retrieved?.pageNumber)
    }

    @Test
    fun getAllBookmarksFlow() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        bookmarkDao.insertBookmark(testBookmark2)
        bookmarkDao.insertBookmark(testBookmark3)
        
        val bookmarks = bookmarkDao.getAllBookmarksFlow().first()
        
        assertEquals(3, bookmarks.size)
        // Should be ordered by timestamp DESC
        assertEquals(listOf(2000L, 1500L, 1000L), bookmarks.map { it.createdAt })
    }

    @Test
    fun getBookmarksForBookFlow() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        bookmarkDao.insertBookmark(testBookmark2)
        bookmarkDao.insertBookmark(testBookmark3)
        
        val book1Bookmarks = bookmarkDao.getBookmarksForBookFlow("book-1").first()
        
        assertEquals(2, book1Bookmarks.size)
        assertTrue(book1Bookmarks.all { it.bookId == "book-1" })
        // Should be ordered by timestamp DESC
        assertEquals(listOf(2000L, 1000L), book1Bookmarks.map { it.createdAt })
    }

    @Test
    fun getBookmarksForBook() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        bookmarkDao.insertBookmark(testBookmark2)
        bookmarkDao.insertBookmark(testBookmark3)
        
        val book1Bookmarks = bookmarkDao.getBookmarksForBook("book-1")
        
        assertEquals(2, book1Bookmarks.size)
        assertTrue(book1Bookmarks.all { it.bookId == "book-1" })
        // Should be ordered by timestamp DESC
        assertEquals(listOf(2000L, 1000L), book1Bookmarks.map { it.createdAt })
    }

    @Test
    fun getBookmarksForNonExistentBook() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        
        val bookmarks = bookmarkDao.getBookmarksForBook("nonexistent-book")
        
        assertTrue(bookmarks.isEmpty())
    }

    @Test
    fun updateBookmark() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        
        val updatedBookmark = testBookmark1.copy(
            note = "Updated note",
            pageNumber = 20
        )
        bookmarkDao.updateBookmark(updatedBookmark)
        
        val retrieved = bookmarkDao.getBookmarkById("bookmark-1")
        
        assertNotNull(retrieved)
        assertEquals("Updated note", retrieved?.note)
        assertEquals(20, retrieved?.pageNumber)
        assertEquals(testBookmark1.chapterNumber, retrieved?.chapterNumber) // Unchanged
    }

    @Test
    fun deleteBookmark() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        
        // Verify bookmark exists
        assertNotNull(bookmarkDao.getBookmarkById("bookmark-1"))
        
        bookmarkDao.deleteBookmark(testBookmark1)
        
        // Verify bookmark is deleted
        assertNull(bookmarkDao.getBookmarkById("bookmark-1"))
    }

    @Test
    fun deleteBookmarkById() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        
        // Verify bookmark exists
        assertNotNull(bookmarkDao.getBookmarkById("bookmark-1"))
        
        bookmarkDao.deleteBookmarkById("bookmark-1")
        
        // Verify bookmark is deleted
        assertNull(bookmarkDao.getBookmarkById("bookmark-1"))
    }

    @Test
    fun deleteBookmarksForBook() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        bookmarkDao.insertBookmark(testBookmark2)
        bookmarkDao.insertBookmark(testBookmark3)
        
        // Verify bookmarks exist
        assertEquals(2, bookmarkDao.getBookmarksForBook("book-1").size)
        assertEquals(1, bookmarkDao.getBookmarksForBook("book-2").size)
        
        bookmarkDao.deleteBookmarksForBook("book-1")
        
        // Verify only book-1 bookmarks are deleted
        assertEquals(0, bookmarkDao.getBookmarksForBook("book-1").size)
        assertEquals(1, bookmarkDao.getBookmarksForBook("book-2").size)
    }

    @Test
    fun deleteAllBookmarks() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        bookmarkDao.insertBookmark(testBookmark2)
        bookmarkDao.insertBookmark(testBookmark3)
        
        // Verify bookmarks exist
        assertEquals(3, bookmarkDao.getAllBookmarksFlow().first().size)
        
        bookmarkDao.deleteAllBookmarks()
        
        // Verify all bookmarks are deleted
        assertEquals(0, bookmarkDao.getAllBookmarksFlow().first().size)
    }

    @Test
    fun insertBookmarkWithConflictReplace() = runTest {
        bookmarkDao.insertBookmark(testBookmark1)
        
        // Insert bookmark with same ID but different data
        val duplicateBookmark = testBookmark1.copy(note = "Replaced note")
        bookmarkDao.insertBookmark(duplicateBookmark)
        
        val retrieved = bookmarkDao.getBookmarkById("bookmark-1")
        
        // Should have replaced the original
        assertEquals("Replaced note", retrieved?.note)
        assertEquals(1, bookmarkDao.getAllBookmarksFlow().first().size) // Should still be only 1 bookmark
    }

    @Test
    fun queryByNonExistentId() = runTest {
        val retrieved = bookmarkDao.getBookmarkById("nonexistent")
        
        assertNull(retrieved)
    }

    @Test
    fun orderingByTimestamp() = runTest {
        // Insert bookmarks with different timestamps out of order
        val bookmark1 = testBookmark1.copy(id = "1", createdAt = 3000L)
        val bookmark2 = testBookmark2.copy(id = "2", createdAt = 1000L)
        val bookmark3 = testBookmark3.copy(id = "3", createdAt = 2000L)
        
        bookmarkDao.insertBookmark(bookmark2) // Insert middle timestamp first
        bookmarkDao.insertBookmark(bookmark1) // Insert latest timestamp
        bookmarkDao.insertBookmark(bookmark3) // Insert earliest timestamp
        
        val allBookmarks = bookmarkDao.getAllBookmarksFlow().first()
        
        // Should be ordered by timestamp DESC
        assertEquals(listOf(3000L, 2000L, 1000L), allBookmarks.map { it.createdAt })
        assertEquals(listOf("1", "3", "2"), allBookmarks.map { it.id })
    }

    @Test
    fun multipleBookmarksForSameLocation() = runTest {
        // Create bookmarks at same location but different times
        val bookmark1 = testBookmark1.copy(
            id = "first",
            chapterNumber = 1,
            pageNumber = 10,
            createdAt = 1000L
        )
        val bookmark2 = testBookmark1.copy(
            id = "second",
            chapterNumber = 1,
            pageNumber = 10,
            createdAt = 2000L
        )
        
        bookmarkDao.insertBookmark(bookmark1)
        bookmarkDao.insertBookmark(bookmark2)
        
        val bookmarks = bookmarkDao.getBookmarksForBook("book-1")
        
        assertEquals(2, bookmarks.size)
        // Should still have both bookmarks (DAO doesn't enforce location uniqueness)
        assertTrue(bookmarks.any { it.id == "first" })
        assertTrue(bookmarks.any { it.id == "second" })
    }

    @Test
    fun flowUpdatesOnDataChange() = runTest {
        val flow = bookmarkDao.getAllBookmarksFlow()
        
        // Initial empty state
        assertEquals(0, flow.first().size)
        
        // Insert bookmark
        bookmarkDao.insertBookmark(testBookmark1)
        assertEquals(1, flow.first().size)
        
        // Insert another bookmark
        bookmarkDao.insertBookmark(testBookmark2)
        assertEquals(2, flow.first().size)
        
        // Delete bookmark
        bookmarkDao.deleteBookmarkById("bookmark-1")
        assertEquals(1, flow.first().size)
        assertEquals("bookmark-2", flow.first()[0].id)
    }

    @Test
    fun bookSpecificFlowUpdates() = runTest {
        val book1Flow = bookmarkDao.getBookmarksForBookFlow("book-1")
        val book2Flow = bookmarkDao.getBookmarksForBookFlow("book-2")
        
        // Initial empty state
        assertEquals(0, book1Flow.first().size)
        assertEquals(0, book2Flow.first().size)
        
        // Insert bookmark for book-1
        bookmarkDao.insertBookmark(testBookmark1)
        assertEquals(1, book1Flow.first().size)
        assertEquals(0, book2Flow.first().size) // Should not affect book-2 flow
        
        // Insert bookmark for book-2
        bookmarkDao.insertBookmark(testBookmark3)
        assertEquals(1, book1Flow.first().size) // Should not affect book-1 flow
        assertEquals(1, book2Flow.first().size)
        
        // Delete bookmark for book-1
        bookmarkDao.deleteBookmarkById("bookmark-1")
        assertEquals(0, book1Flow.first().size)
        assertEquals(1, book2Flow.first().size) // Should not affect book-2 flow
    }
}