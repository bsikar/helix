package com.bsikar.helix.data.repository

import android.util.Log
import com.bsikar.helix.data.model.ParsedEpub
import com.bsikar.helix.data.source.dao.CachedEpubMetadataDao
import com.bsikar.helix.data.source.dao.CacheStatistics
import com.bsikar.helix.data.source.entities.CachedEpubMetadataEntity
import com.bsikar.helix.data.source.entities.toCachedEntity
import com.bsikar.helix.data.source.entities.toParsedEpub
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubMetadataCacheRepository @Inject constructor(
    private val cachedEpubMetadataDao: CachedEpubMetadataDao
) {
    private val TAG = "EpubMetadataCacheRepository"
    
    /**
     * Get cached metadata for a book
     */
    suspend fun getCachedMetadata(bookId: String): ParsedEpub? {
        return try {
            val cachedEntity = cachedEpubMetadataDao.getCachedMetadataForBook(bookId)
            cachedEntity?.let { entity ->
                if (entity.isValid) {
                    Log.d(TAG, "Found valid cached metadata for book $bookId")
                    entity.toParsedEpub()
                } else {
                    Log.d(TAG, "Found invalid cached metadata for book $bookId: ${entity.validationErrors}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cached metadata for book $bookId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get cached metadata as Flow
     */
    fun getCachedMetadataFlow(bookId: String): Flow<ParsedEpub?> {
        return cachedEpubMetadataDao.getCachedMetadataForBookFlow(bookId).map { entity ->
            entity?.takeIf { it.isValid }?.toParsedEpub()
        }
    }
    
    /**
     * Check if cache is valid for a file
     */
    suspend fun isCacheValid(bookId: String, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.d(TAG, "File does not exist: $filePath")
                return false
            }
            
            val checksum = calculateFileChecksum(file)
            val lastModified = file.lastModified()
            
            val isValid = cachedEpubMetadataDao.isCacheValid(bookId, checksum, lastModified)
            Log.d(TAG, "Cache validity for book $bookId: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check cache validity for book $bookId: ${e.message}", e)
            false
        }
    }
    
    /**
     * Cache parsed metadata
     */
    suspend fun cacheMetadata(
        bookId: String, 
        parsedEpub: ParsedEpub, 
        parsingTimeMs: Long = 0
    ): Boolean {
        return try {
            val filePath = parsedEpub.filePath
            if (filePath.isNullOrEmpty()) {
                Log.w(TAG, "Cannot cache metadata without file path for book $bookId")
                return false
            }
            
            val file = File(filePath)
            val checksum = if (file.exists()) {
                calculateFileChecksum(file)
            } else {
                // For stream-based imports, use content-based checksum
                calculateContentChecksum(parsedEpub)
            }
            
            val cachedEntity = parsedEpub.toCachedEntity(
                bookId = bookId,
                fileChecksum = checksum,
                parsingTimeMs = parsingTimeMs
            )
            
            cachedEpubMetadataDao.upsertCachedMetadata(cachedEntity)
            Log.d(TAG, "Successfully cached metadata for book $bookId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache metadata for book $bookId: ${e.message}", e)
            false
        }
    }
    
    /**
     * Invalidate cache for a book
     */
    suspend fun invalidateCache(bookId: String, reason: String? = null) {
        try {
            cachedEpubMetadataDao.invalidateCacheForBook(bookId, reason)
            Log.d(TAG, "Invalidated cache for book $bookId: $reason")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to invalidate cache for book $bookId: ${e.message}", e)
        }
    }
    
    /**
     * Remove cached metadata for a book
     */
    suspend fun removeCachedMetadata(bookId: String) {
        try {
            cachedEpubMetadataDao.deleteCachedMetadataForBook(bookId)
            Log.d(TAG, "Removed cached metadata for book $bookId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove cached metadata for book $bookId: ${e.message}", e)
        }
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStatistics(): CacheStatistics? {
        return try {
            cachedEpubMetadataDao.getCacheStatistics()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache statistics: ${e.message}", e)
            null
        }
    }
    
    /**
     * Clean up invalid cache entries
     */
    suspend fun cleanupInvalidCaches() {
        try {
            val invalidCaches = cachedEpubMetadataDao.getInvalidCachedMetadata()
            Log.d(TAG, "Found ${invalidCaches.size} invalid cache entries to clean up")
            
            cachedEpubMetadataDao.deleteInvalidCachedMetadata()
            Log.d(TAG, "Cleaned up ${invalidCaches.size} invalid cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup invalid caches: ${e.message}", e)
        }
    }
    
    /**
     * Clean up old cache entries
     */
    suspend fun cleanupOldCaches(maxAgeMs: Long = 30L * 24 * 60 * 60 * 1000) { // 30 days default
        try {
            val cutoffTime = System.currentTimeMillis() - maxAgeMs
            cachedEpubMetadataDao.deleteOldCachedMetadata(cutoffTime)
            Log.d(TAG, "Cleaned up cache entries older than $maxAgeMs ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old caches: ${e.message}", e)
        }
    }
    
    /**
     * Validate and refresh cache if needed
     */
    suspend fun validateAndRefreshCache(bookId: String, filePath: String): Boolean {
        return try {
            val isValid = isCacheValid(bookId, filePath)
            if (!isValid) {
                Log.d(TAG, "Cache is invalid for book $bookId, marking for refresh")
                invalidateCache(bookId, "File modified or checksum mismatch")
            }
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate cache for book $bookId: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get cache entries that need refresh
     */
    suspend fun getCachesNeedingRefresh(maxAgeMs: Long = 7L * 24 * 60 * 60 * 1000): List<String> { // 7 days
        return try {
            val cutoffTime = System.currentTimeMillis() - maxAgeMs
            val oldCaches = cachedEpubMetadataDao.getCachedMetadataOlderThan(cutoffTime)
            oldCaches.map { it.bookId }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get caches needing refresh: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Calculate file checksum for cache validation
     */
    private fun calculateFileChecksum(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate file checksum: ${e.message}", e)
            file.absolutePath.hashCode().toString()
        }
    }
    
    /**
     * Calculate content-based checksum for stream imports
     */
    private fun calculateContentChecksum(parsedEpub: ParsedEpub): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val content = buildString {
                append(parsedEpub.metadata.title)
                append(parsedEpub.metadata.author ?: "")
                append(parsedEpub.fileSize)
                append(parsedEpub.lastModified)
                append(parsedEpub.chapterCount)
            }
            md.update(content.toByteArray())
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate content checksum: ${e.message}", e)
            parsedEpub.metadata.title.hashCode().toString()
        }
    }
    
    /**
     * Refresh cache for all books
     */
    suspend fun refreshAllCaches() {
        try {
            Log.d(TAG, "Starting cache refresh for all books")
            
            // Get all cached metadata
            val allCaches = cachedEpubMetadataDao.getAllCachedMetadata()
            
            // Validate each cache
            for (cache in allCaches) {
                try {
                    if (cache.filePath.isNotEmpty()) {
                        val file = File(cache.filePath)
                        if (file.exists()) {
                            val currentChecksum = calculateFileChecksum(file)
                            val currentModified = file.lastModified()
                            
                            if (currentChecksum != cache.fileChecksum || currentModified != cache.lastModified) {
                                invalidateCache(cache.bookId, "File changed during bulk validation")
                            }
                        } else {
                            invalidateCache(cache.bookId, "File no longer exists")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to validate cache for book ${cache.bookId}: ${e.message}")
                    invalidateCache(cache.bookId, "Validation error: ${e.message}")
                }
            }
            
            Log.d(TAG, "Completed cache refresh for ${allCaches.size} books")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh all caches: ${e.message}", e)
        }
    }
}