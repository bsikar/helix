package com.bsikar.helix.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bsikar.helix.data.model.Bookmark
import com.bsikar.helix.data.source.dao.BookmarkDao
import com.bsikar.helix.data.source.entities.BookmarkEntity
import com.bsikar.helix.data.source.entities.toBookmark
import com.bsikar.helix.data.source.entities.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val context: Context
) {
    private val BOOKMARKS_KEY = stringPreferencesKey("bookmarks")
    private var hasAttemptedMigration = false
    
    /**
     * Get all bookmarks as Flow
     */
    fun getAllBookmarksFlow(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarksFlow().map { entities ->
            entities.map { it.toBookmark() }
        }
    }
    
    /**
     * Get bookmarks for a specific book as Flow
     */
    fun getBookmarksForBookFlow(bookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksForBookFlow(bookId).map { entities ->
            entities.map { it.toBookmark() }
        }
    }
    
    /**
     * Get bookmarks for a specific book
     */
    suspend fun getBookmarksForBook(bookId: String): List<Bookmark> {
        return bookmarkDao.getBookmarksForBook(bookId).map { it.toBookmark() }
    }
    
    /**
     * Get all bookmarks
     */
    suspend fun getAllBookmarks(): List<Bookmark> {
        // Perform migration if not already done
        if (!hasAttemptedMigration) {
            migrateFromDataStore()
            hasAttemptedMigration = true
        }
        
        return bookmarkDao.getAllBookmarksFlow().first().map { it.toBookmark() }
    }
    
    /**
     * Get bookmark by ID
     */
    suspend fun getBookmarkById(id: String): Bookmark? {
        return bookmarkDao.getBookmarkById(id)?.toBookmark()
    }
    
    /**
     * Add or update a bookmark
     */
    suspend fun addBookmark(bookmark: Bookmark) {
        // Remove existing bookmark at same location if any
        val existingBookmarks = bookmarkDao.getBookmarksForBook(bookmark.bookId)
        existingBookmarks.filter { 
            it.chapterNumber == bookmark.chapterNumber && 
            it.pageNumber == bookmark.pageNumber 
        }.forEach { existing ->
            bookmarkDao.deleteBookmarkById(existing.id)
        }
        
        // Add new bookmark
        bookmarkDao.insertBookmark(bookmark.toEntity())
    }
    
    /**
     * Update an existing bookmark
     */
    suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark.toEntity())
    }
    
    /**
     * Delete a bookmark by ID
     */
    suspend fun deleteBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }
    
    /**
     * Delete all bookmarks for a book
     */
    suspend fun deleteBookmarksForBook(bookId: String) {
        bookmarkDao.deleteBookmarksForBook(bookId)
    }
    
    /**
     * Update bookmark note
     */
    suspend fun updateBookmarkNote(bookmarkId: String, note: String) {
        val bookmark = bookmarkDao.getBookmarkById(bookmarkId)
        if (bookmark != null) {
            val updatedBookmark = bookmark.copy(note = note)
            bookmarkDao.updateBookmark(updatedBookmark)
        }
    }
    
    /**
     * Check if a page is bookmarked
     */
    suspend fun isPageBookmarked(bookId: String, chapterNumber: Int, pageNumber: Int): Boolean {
        return bookmarkDao.getBookmarksForBook(bookId).any { 
            it.chapterNumber == chapterNumber && it.pageNumber == pageNumber 
        }
    }
    
    /**
     * Migrate bookmarks from DataStore to Room database
     */
    private suspend fun migrateFromDataStore() {
        try {
            Log.d("BookmarkRepository", "Starting bookmark migration from DataStore to Room")
            
            // Check if we already have bookmarks in the database
            val existingBookmarks = bookmarkDao.getAllBookmarksFlow().first()
            if (existingBookmarks.isNotEmpty()) {
                Log.d("BookmarkRepository", "Database already contains ${existingBookmarks.size} bookmarks, skipping migration")
                return
            }
            
            // Try to read bookmarks from DataStore
            val dataStoreBookmarks = try {
                val bookmarksJson = context.dataStore.data.first()[BOOKMARKS_KEY] ?: "[]"
                Json.decodeFromString<List<Bookmark>>(bookmarksJson)
            } catch (e: Exception) {
                Log.w("BookmarkRepository", "Failed to read bookmarks from DataStore: ${e.message}")
                emptyList<Bookmark>()
            }
            
            if (dataStoreBookmarks.isNotEmpty()) {
                Log.d("BookmarkRepository", "Migrating ${dataStoreBookmarks.size} bookmarks from DataStore to Room")
                
                // Insert all bookmarks into Room database
                dataStoreBookmarks.forEach { bookmark ->
                    try {
                        bookmarkDao.insertBookmark(bookmark.toEntity())
                    } catch (e: Exception) {
                        Log.w("BookmarkRepository", "Failed to migrate bookmark ${bookmark.id}: ${e.message}")
                    }
                }
                
                Log.d("BookmarkRepository", "Successfully migrated bookmarks to Room database")
                
                // Optionally clear the DataStore bookmarks after successful migration
                // Commenting this out to keep the old data as backup for now
                // context.dataStore.edit { preferences ->
                //     preferences.remove(BOOKMARKS_KEY)
                // }
            } else {
                Log.d("BookmarkRepository", "No bookmarks found in DataStore to migrate")
            }
        } catch (e: Exception) {
            Log.e("BookmarkRepository", "Error during bookmark migration: ${e.message}", e)
        }
    }
}