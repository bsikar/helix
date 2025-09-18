@file:Suppress("TooGenericExceptionCaught")

package com.bsikar.helix

import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Global cache for parsed EPUB content to prevent reloading when navigating between screens
 */
object EpubContentCache {

    private data class CacheEntry(
        val content: RichEpubContent,
        val lastModified: Long,
        val fileSize: Long,
        var lastAccessed: Long = System.currentTimeMillis()
    )

    private val cache = mutableMapOf<String, CacheEntry>()
    private val mutex = Mutex()

    // Limit cache to 3 books maximum to manage memory usage
    private const val MAX_CACHED_BOOKS = 3

    /**
     * Get cached content for an EPUB file if it exists and is still valid
     */
    suspend fun getCachedContent(file: File): RichEpubContent? = mutex.withLock {
        try {
            if (!file.exists()) return null

            val entry = cache[file.absolutePath] ?: return null

            // Check if file has been modified since caching
            if (entry.lastModified != file.lastModified() || entry.fileSize != file.length()) {
                // File changed, remove invalid cache
                cache.remove(file.absolutePath)
                return null
            }

            // Update last accessed time for LRU tracking
            entry.lastAccessed = System.currentTimeMillis()

            return entry.content
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * Cache parsed content for an EPUB file
     */
    suspend fun cacheContent(file: File, content: RichEpubContent): Unit = mutex.withLock {
        try {
            if (file.exists()) {
                // If cache is at capacity, remove the least recently used entry
                if (cache.size >= MAX_CACHED_BOOKS && !cache.containsKey(file.absolutePath)) {
                    val lruKey = cache.entries.minByOrNull { it.value.lastAccessed }?.key
                    lruKey?.let { cache.remove(it) }
                }

                cache[file.absolutePath] = CacheEntry(
                    content = content,
                    lastModified = file.lastModified(),
                    fileSize = file.length(),
                    lastAccessed = System.currentTimeMillis()
                )
            }
        } catch (ignored: Exception) {
            // Ignore cache failures
        }
    }

    /**
     * Clear cached content for a specific file
     */
    suspend fun clearCache(file: File): Unit = mutex.withLock {
        cache.remove(file.absolutePath)
    }

    /**
     * Clear all cached content
     */
    suspend fun clearAllCache(): Unit = mutex.withLock {
        cache.clear()
    }

    /**
     * Get current cache size for debugging
     */
    suspend fun getCacheSize(): Int = mutex.withLock {
        cache.size
    }
}
