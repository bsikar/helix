package com.bsikar.helix.viewmodels

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.data.UserPreferences
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.repository.ReadingProgressRepository
import com.bsikar.helix.data.repository.ChapterRepository
import com.bsikar.helix.data.repository.ReadingAnalyticsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ReaderViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var libraryManager: LibraryManager
    private lateinit var preferencesManager: UserPreferencesManager
    private lateinit var readingProgressRepository: ReadingProgressRepository
    private lateinit var chapterRepository: ChapterRepository
    private lateinit var analyticsRepository: ReadingAnalyticsRepository
    private lateinit var viewModel: ReaderViewModel
    private lateinit var mockContext: Context

    // Test data
    private val testBook = Book(
        id = "test-book-1",
        title = "Test Book",
        author = "Test Author",
        coverColor = 0xFF000000L,
        filePath = "/path/test.epub",
        progress = 0.3f,
        currentChapter = 2,
        currentPage = 10,
        totalPages = 100,
        totalChapters = 5,
        isImported = true
    )

    private val testReaderSettings = ReaderSettings(
        fontSize = 16,
        lineHeight = 1.5f,
        brightness = 1.0f
    )

    private val testUserPreferences = UserPreferences(
        selectedReaderSettings = testReaderSettings
    )

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        libraryManager = mockk<LibraryManager>(relaxed = true)
        preferencesManager = mockk<UserPreferencesManager>(relaxed = true)
        readingProgressRepository = mockk<ReadingProgressRepository>(relaxed = true)
        chapterRepository = mockk<ChapterRepository>(relaxed = true)
        analyticsRepository = mockk<ReadingAnalyticsRepository>(relaxed = true)
        mockContext = mockk<Context>(relaxed = true)
        
        // Mock preferences state
        every { preferencesManager.preferences } returns androidx.compose.runtime.mutableStateOf(testUserPreferences)
        
        viewModel = ReaderViewModel(
            libraryManager, 
            preferencesManager,
            readingProgressRepository,
            chapterRepository,
            analyticsRepository,
            mockContext
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        assertNull(viewModel.currentBook.value)
        assertNull(viewModel.parsedEpub.value)
        assertTrue(viewModel.currentContent.value.isEmpty())
        assertFalse(viewModel.isLoadingContent.value)
        assertFalse(viewModel.isLoadingChapter.value)
        assertNull(viewModel.loadingError.value)
        assertEquals(0, viewModel.currentChapterIndex.value)
    }

    @Test
    fun `loadBook should work with basic functionality`() = runTest {
        // Test that loadBook can be called without errors
        viewModel.loadBook(testBook)
        
        // Basic verification that the book was set
        assertEquals(testBook, viewModel.currentBook.value)
    }

    @Test
    fun `ViewModel should be created successfully`() = runTest {
        // Test that the ViewModel can be created without issues
        assertNotNull(viewModel)
        assertNotNull(viewModel.currentBook)
        assertNotNull(viewModel.currentContent)
        assertNotNull(viewModel.isLoadingContent)
        assertNotNull(viewModel.isLoadingChapter)
        assertNotNull(viewModel.currentChapterIndex)
    }

    @Test
    fun `getCacheStats should return cache information`() = runTest {
        val stats = viewModel.getCacheStats()
        
        assertTrue(stats.contains("EPUB cache"))
        assertTrue(stats.contains("Content cache"))
        assertTrue(stats.contains("entries"))
    }

    @Test
    fun `basic state properties should be accessible`() = runTest {
        // Test that all state properties are accessible (some may be null initially)
        assertNotNull(viewModel.currentBook) // StateFlow itself should not be null
        assertNotNull(viewModel.parsedEpub) // StateFlow itself should not be null
        assertNotNull(viewModel.currentContent.value) // List should not be null (but may be empty)
        assertNotNull(viewModel.isLoadingContent.value) // Boolean should not be null
        assertNotNull(viewModel.isLoadingChapter.value) // Boolean should not be null
        // loadingError can be null, so don't check it
        assertNotNull(viewModel.currentChapterIndex.value) // Int should not be null
    }

    @Test
    fun `loadBook with null should handle gracefully`() = runTest {
        // Test that loadBook handles non-imported books
        val nonImportedBook = testBook.copy(isImported = false)
        
        viewModel.loadBook(nonImportedBook)
        
        assertEquals(nonImportedBook, viewModel.currentBook.value)
    }

    @Test
    fun `cache operations should work`() = runTest {
        // Test cache operations don't throw exceptions
        viewModel.invalidateContentCache()
        viewModel.clearAllCaches()
        
        val stats = viewModel.getCacheStats()
        assertNotNull(stats)
    }
}