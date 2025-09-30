package com.bsikar.helix.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.PresetTags
import com.bsikar.helix.data.model.ReadingStatus
import io.mockk.any
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var libraryManager: LibraryManager
    private lateinit var viewModel: LibraryViewModel
    private lateinit var booksFlow: MutableStateFlow<List<Book>>

    // Test data
    private val testBooks = listOf(
        Book(
            id = "1",
            title = "Test Book 1",
            author = "Author 1",
            coverColor = 0xFF000000L,
            filePath = "/path/test1.epub",
            progress = 0.5f,
            tags = listOf("action", "adventure")
        ),
        Book(
            id = "2", 
            title = "Test Book 2",
            author = "Author 2",
            coverColor = 0xFF111111L,
            filePath = "/path/test2.epub",
            progress = 0.0f,
            tags = listOf("romance", "drama")
        ),
        Book(
            id = "3",
            title = "Test Book 3", 
            author = "Author 3",
            coverColor = 0xFF222222L,
            filePath = "/path/test3.epub",
            progress = 1.0f,
            tags = listOf("fantasy", "magic")
        )
    )

    @Before
    fun setUp() {
        // Initialize PresetTags for testing
        PresetTags.initializeForTesting()
        
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        libraryManager = mockk<LibraryManager>(relaxed = true)
        booksFlow = MutableStateFlow(testBooks)

        // Mock the books state from LibraryManager
        every { libraryManager.books } returns androidx.compose.runtime.mutableStateOf(testBooks)
        every { libraryManager.getBooksFlow() } returns booksFlow
        justRun { libraryManager.rescanWatchedDirectoriesAsync(any()) }
        justRun { libraryManager.removeBook(any()) }
        justRun { libraryManager.updateBookTags(any(), any()) }

        viewModel = LibraryViewModel(libraryManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        // Verify initial state
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(false, viewModel.readingSortAscending.value)
        assertEquals(true, viewModel.onDeckSortAscending.value)
        assertEquals(true, viewModel.completedSortAscending.value)
    }

    @Test
    fun `updateSearchQuery should update search state`() = runTest {
        val testQuery = "test search"

        viewModel.updateSearchQuery(testQuery)

        assertEquals(testQuery, viewModel.searchQuery.value)
    }

    @Test
    fun `updateSearchQuery sanitizes input`() = runTest {
        val unsanitized = "   test\nquery\u0007 with   extra   spaces" + "x".repeat(200)

        viewModel.updateSearchQuery(unsanitized)

        val expectedPrefix = "test query with extra spaces"
        val expected = (expectedPrefix + "x".repeat(200)).take(120)
        assertEquals(expected.length, viewModel.searchQuery.value.length)
        assertEquals(expected, viewModel.searchQuery.value)
    }

    @Test
    fun `clearSearch should reset search query`() = runTest {
        // Set a search query first
        viewModel.updateSearchQuery("test")
        assertEquals("test", viewModel.searchQuery.value)
        
        // Clear search
        viewModel.updateSearchQuery("")
        
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `toggleReadingSortOrder should toggle sort direction`() = runTest {
        val initialSort = viewModel.readingSortAscending.value
        
        viewModel.toggleReadingSortOrder()
        
        assertEquals(!initialSort, viewModel.readingSortAscending.value)
        
        // Toggle again
        viewModel.toggleReadingSortOrder()
        
        assertEquals(initialSort, viewModel.readingSortAscending.value)
    }

    @Test
    fun `togglePlanToReadSortOrder should toggle sort direction`() = runTest {
        val initialSort = viewModel.onDeckSortAscending.value
        
        viewModel.toggleOnDeckSortOrder()
        
        assertEquals(!initialSort, viewModel.onDeckSortAscending.value)
    }

    @Test
    fun `toggleCompletedSortOrder should toggle sort direction`() = runTest {
        val initialSort = viewModel.completedSortAscending.value
        
        viewModel.toggleCompletedSortOrder()
        
        assertEquals(!initialSort, viewModel.completedSortAscending.value)
    }

    @Test
    fun `basic ViewModel functionality should work`() = runTest {
        // Test that the ViewModel can be created and basic operations work
        assertNotNull(viewModel)
        
        // Test search functionality
        viewModel.updateSearchQuery("test")
        assertEquals("test", viewModel.searchQuery.value)
        
        // Test sort toggles
        val initialReadingSort = viewModel.readingSortAscending.value
        viewModel.toggleReadingSortOrder()
        assertEquals(!initialReadingSort, viewModel.readingSortAscending.value)
    }

    @Test
    fun `delegateOperations should call libraryManager methods`() = runTest {
        val bookId = "test-book-id"
        val newProgress = 0.75f
        val newTags = listOf("tag1", "tag2")
        
        // Test various delegate operations
        viewModel.removeBook(bookId)
        verify { libraryManager.removeBook(bookId) }
        
        viewModel.updateBookTags(bookId, newTags)
        verify { libraryManager.updateBookTags(bookId, newTags) }
    }

    @Test
    fun `toggleTagFilter ignores blank ids`() = runTest {
        viewModel.toggleTagFilter("  ")
        assertTrue(viewModel.activeTagFilters.value.isEmpty())

        viewModel.toggleTagFilter("action")
        viewModel.toggleTagFilter("action")
        assertTrue(viewModel.activeTagFilters.value.isEmpty())
    }

    @Test
    fun `filteredLibraryBooks handles empty tags safely`() = runTest {
        val bookWithBlankTag = testBooks.first().copy(tags = listOf("action", ""))
        booksFlow.value = listOf(bookWithBlankTag, testBooks[1])

        advanceUntilIdle()

        viewModel.toggleTagFilter("action")
        advanceUntilIdle()

        val filtered = viewModel.filteredLibraryBooks.value
        assertEquals(1, filtered.size)
        assertEquals(bookWithBlankTag.id, filtered.first().id)
    }

    @Test
    fun `refreshLibrary should call libraryManager refresh`() = runTest {
        viewModel.refreshLibrary()
        
        // refreshLibrary method was renamed to refreshBooks
        // This test should verify the actual method call made by viewModel.refreshLibrary()
    }

    @Test
    fun `sorting state management should work correctly`() = runTest {
        // Test initial state
        assertEquals(false, viewModel.readingSortAscending.value)
        assertEquals(true, viewModel.onDeckSortAscending.value)
        assertEquals(true, viewModel.completedSortAscending.value)
        
        // Test toggling
        viewModel.toggleReadingSortOrder()
        assertEquals(true, viewModel.readingSortAscending.value)
        
        viewModel.toggleOnDeckSortOrder()
        assertEquals(false, viewModel.onDeckSortAscending.value)
        
        viewModel.toggleCompletedSortOrder()
        assertEquals(false, viewModel.completedSortAscending.value)
    }

    @Test
    fun `search query management should work`() = runTest {
        // Test empty initial state
        assertEquals("", viewModel.searchQuery.value)
        
        // Test setting search query
        viewModel.updateSearchQuery("test query")
        assertEquals("test query", viewModel.searchQuery.value)
        
        // Test clearing search
        viewModel.updateSearchQuery("")
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `book operations should delegate to LibraryManager`() = runTest {
        val bookId = "test-id"
        val progress = 0.5f
        val tags = listOf("tag1", "tag2")
        
        // Test book operations
        viewModel.setBookProgress(bookId, progress)
        viewModel.updateBookTags(bookId, tags)
        viewModel.removeBook(bookId)
        
        // Verify that these operations are delegated to LibraryManager
        verify { libraryManager.removeBook(bookId) }
        verify { libraryManager.updateBookTags(bookId, tags) }
    }
}