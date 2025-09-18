package com.bsikar.helix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Cache manager for EPUB cover images
 */
@Suppress("TooGenericExceptionCaught", "MagicNumber")
class CoverImageCache private constructor(private val context: Context) {

    private val coverExtractor = EpubCoverExtractor()
    private val memoryCache = mutableMapOf<String, Bitmap?>()

    private val cacheDir: File by lazy {
        File(context.cacheDir, "epub_covers").apply {
            if (!exists()) mkdirs()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CoverImageCache? = null

        fun getInstance(context: Context): CoverImageCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CoverImageCache(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private const val CACHE_VERSION = 1
        private const val MAX_MEMORY_CACHE_SIZE = 50
    }

    /**
     * Get cover image for EPUB file, using cache if available
     */
    suspend fun getCover(epubFile: File): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(epubFile)

        // Check memory cache first
        if (memoryCache.containsKey(cacheKey)) {
            return@withContext memoryCache[cacheKey]
        }

        // Check disk cache
        val cachedFile = File(cacheDir, "$cacheKey.jpg")
        if (cachedFile.exists() && cachedFile.lastModified() >= epubFile.lastModified()) {
            val bitmap = BitmapFactory.decodeFile(cachedFile.absolutePath)
            if (bitmap != null) {
                addToMemoryCache(cacheKey, bitmap)
                return@withContext bitmap
            }
        }

        // Extract cover from EPUB
        val cover = try {
            coverExtractor.extractCover(epubFile)
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            null
        }

        // Cache the result (even if null to avoid repeated extraction attempts)
        addToMemoryCache(cacheKey, cover)

        // Save to disk cache if we have a cover
        if (cover != null) {
            saveToDiskCache(cachedFile, cover)
        }

        cover
    }

    private fun generateCacheKey(epubFile: File): String {
        val input = "${epubFile.absolutePath}_${epubFile.lastModified()}_$CACHE_VERSION"
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun addToMemoryCache(key: String, bitmap: Bitmap?) {
        // Implement simple LRU by removing oldest entries when cache is full
        if (memoryCache.size >= MAX_MEMORY_CACHE_SIZE) {
            val oldestKey = memoryCache.keys.first()
            memoryCache.remove(oldestKey)
        }
        memoryCache[key] = bitmap
    }

    private fun saveToDiskCache(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    /**
     * Clear all cached covers
     */
    fun clearCache() {
        memoryCache.clear()
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): String {
        val memorySize = memoryCache.size
        val diskSize = try {
            cacheDir.listFiles()?.size ?: 0
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            0
        }
        return "Memory: $memorySize/$MAX_MEMORY_CACHE_SIZE, Disk: $diskSize files"
    }
}
