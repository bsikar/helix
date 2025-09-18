@file:Suppress("TooGenericExceptionCaught")

package com.bsikar.helix.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/**
 * Repository for managing reading progress persistence
 */
@Suppress("TooManyFunctions")
class ReadingProgressRepository private constructor(private val context: Context) {

    private val progressCache = mutableMapOf<String, ReadingProgress>()
    private val _recentBooks = MutableStateFlow<List<ReadingProgress>>(emptyList())
    val recentBooks: Flow<List<ReadingProgress>> = _recentBooks.asStateFlow()

    private val progressDir: File by lazy {
        File(context.filesDir, "reading_progress").apply {
            if (!exists()) mkdirs()
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    companion object {
        @Volatile
        private var INSTANCE: ReadingProgressRepository? = null

        fun getInstance(context: Context): ReadingProgressRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReadingProgressRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

private const val MAX_RECENT_BOOKS = 1
        private const val CLEANUP_DAYS = 90
        private const val HOURS_IN_DAY = 24
        private const val MINUTES_IN_HOUR = 60
        private const val SECONDS_IN_MINUTE = 60
        private const val MILLIS_IN_SECOND = 1000L
    }

    init {
        // Load recent books on initialization
        loadRecentBooks()
    }

    /**
     * Save reading progress for an EPUB
     */
    suspend fun saveProgress(progress: ReadingProgress) = withContext(Dispatchers.IO) {
        try {
            val progressFile = getProgressFile(progress.epubPath)
            val jsonString = json.encodeToString(progress)
            progressFile.writeText(jsonString)

            // Update cache
            progressCache[progress.epubPath] = progress

            // Update recent books
            updateRecentBooks(progress)
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    /**
     * Load reading progress for an EPUB
     */
    suspend fun loadProgress(epubFile: File): ReadingProgress? = withContext(Dispatchers.IO) {
        try {
            val epubPath = epubFile.absolutePath

            // Check cache first
            progressCache[epubPath]?.let { cachedProgress ->
                if (cachedProgress.isValidFor(epubFile)) {
                    return@withContext cachedProgress
                } else {
                    // File changed, remove invalid cache
                    progressCache.remove(epubPath)
                }
            }

            // Load from disk
            val progressFile = getProgressFile(epubPath)
            if (progressFile.exists()) {
                val jsonString = progressFile.readText()
                val progress = json.decodeFromString<ReadingProgress>(jsonString)

                if (progress.isValidFor(epubFile)) {
                    progressCache[epubPath] = progress
                    return@withContext progress
                } else {
                    // File changed, delete invalid progress
                    progressFile.delete()
                }
            }

            null
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            null
        }
    }

    /**
     * Create initial progress for a new EPUB
     */
    suspend fun createInitialProgress(epubFile: File, totalChapters: Int): ReadingProgress {
        return ReadingProgress(
            epubPath = epubFile.absolutePath,
            epubFileName = epubFile.name,
            lastModified = epubFile.lastModified(),
            fileSize = epubFile.length(),
            totalChapters = totalChapters,
            currentChapterIndex = 0,
            currentElementIndex = 0,
            scrollOffset = 0
        )
    }

    /**
     * Update progress with new position
     */
    suspend fun updatePosition(
        epubFile: File,
        chapterIndex: Int,
        elementIndex: Int,
        scrollOffset: Int,
        estimatedProgress: Float = 0f
    ) {
        val existingProgress = loadProgress(epubFile)
        if (existingProgress != null) {
            val updatedProgress = existingProgress.copy(
                currentChapterIndex = chapterIndex,
                currentElementIndex = elementIndex,
                scrollOffset = scrollOffset,
                lastReadTimestamp = System.currentTimeMillis(),
                estimatedProgress = estimatedProgress.coerceIn(0f, 1f)
            )
            saveProgress(updatedProgress)
        }
    }

    /**
     * Add a bookmark
     */
    @Suppress("LongParameterList")
    suspend fun addBookmark(
        epubFile: File,
        chapterIndex: Int,
        elementIndex: Int,
        title: String,
        previewText: String,
        note: String = ""
    ) {
        val existingProgress = loadProgress(epubFile)
        if (existingProgress != null) {
            val bookmark = BookmarkEntry(
                chapterIndex = chapterIndex,
                elementIndex = elementIndex,
                title = title,
                previewText = previewText,
                note = note
            )

            val updatedBookmarks = existingProgress.bookmarks + bookmark
            val updatedProgress = existingProgress.copy(bookmarks = updatedBookmarks)
            saveProgress(updatedProgress)
        }
    }

    /**
     * Remove a bookmark
     */
    suspend fun removeBookmark(epubFile: File, bookmarkIndex: Int) {
        val existingProgress = loadProgress(epubFile)
        if (existingProgress != null && bookmarkIndex < existingProgress.bookmarks.size) {
            val updatedBookmarks = existingProgress.bookmarks.toMutableList().apply {
                removeAt(bookmarkIndex)
            }
            val updatedProgress = existingProgress.copy(bookmarks = updatedBookmarks)
            saveProgress(updatedProgress)
        }
    }

    /**
     * Get all saved progress entries
     */
    suspend fun getAllProgress(): List<ReadingProgress> = withContext(Dispatchers.IO) {
        try {
            progressDir.listFiles()
                ?.mapNotNull { file ->
                    try {
                        val jsonString = file.readText()
                        json.decodeFromString<ReadingProgress>(jsonString)
                    } catch (e: Exception) {
                        @Suppress("TooGenericExceptionCaught")
                        null
                    }
                }
                ?.sortedByDescending { it.lastReadTimestamp }
                ?: emptyList()
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            emptyList()
        }
    }

    /**
     * Clean up old progress files
     */
    suspend fun cleanupOldProgress() = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() -
                (CLEANUP_DAYS * HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLIS_IN_SECOND)
            var deletedCount = 0

            progressDir.listFiles()?.forEach { file ->
                try {
                    val jsonString = file.readText()
                    val progress = json.decodeFromString<ReadingProgress>(jsonString)

                    if (progress.lastReadTimestamp < cutoffTime) {
                        file.delete()
                        progressCache.remove(progress.epubPath)
                        deletedCount++
                    }
                } catch (e: Exception) {
                    @Suppress("TooGenericExceptionCaught")
                    // Delete corrupted files
                    file.delete()
                    deletedCount++
                }
            }

            if (deletedCount > 0) {
                loadRecentBooks() // Refresh recent books
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    /**
     * Remove a book from the recent books list only (keeps progress file)
     */
    fun removeRecentBook(epubPath: String) {
        val currentList = _recentBooks.value.toMutableList()
        currentList.removeAll { it.epubPath == epubPath }
        _recentBooks.value = currentList
    }

    /**
     * Remove a book from recent books and optionally reset its progress
     */
    fun removeRecentBookWithOptions(epubPath: String, resetProgress: Boolean = false) {
        // Always remove from recent books list
        removeRecentBook(epubPath)

        if (resetProgress) {
            // Also delete the progress file from disk
            try {
                val progressFile = getProgressFile(epubPath)
                if (progressFile.exists()) {
                    progressFile.delete()
                    progressCache.remove(epubPath)
                }
            } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            }
        }
    }

    private fun getProgressFile(epubPath: String): File {
        val hash = hashString(epubPath)
        return File(progressDir, "$hash.json")
    }

    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun updateRecentBooks(progress: ReadingProgress) {
        val currentList = _recentBooks.value.toMutableList()

        // Remove existing entry for this book
        currentList.removeAll { it.epubPath == progress.epubPath }

        // Add to beginning
        currentList.add(0, progress)

        // Limit size
        while (currentList.size > MAX_RECENT_BOOKS) {
            currentList.removeAt(currentList.size - 1)
        }

        _recentBooks.value = currentList
    }

    private fun loadRecentBooks() {
        try {
            // Load in background and update flow
            CoroutineScope(Dispatchers.IO).launch {
                val allProgress = getAllProgress()
                val recent = allProgress.take(MAX_RECENT_BOOKS)
                _recentBooks.value = recent
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }
}
