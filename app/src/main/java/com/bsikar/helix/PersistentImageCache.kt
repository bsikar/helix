package com.bsikar.helix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

/**
 * Persistent disk cache that survives app restarts
 */
@Suppress("TooManyFunctions", "NestedBlockDepth", "TooGenericExceptionCaught")
class PersistentImageCache private constructor(private val context: Context) {
    private val cacheDir: File by lazy {
        File(context.cacheDir, "image_cache").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val estimatedCacheSize = AtomicLong(0L)
    private var lastSizeCalculation = 0L

    companion object {
        @Volatile
        private var INSTANCE: PersistentImageCache? = null

        private const val MAX_CACHE_SIZE_MB = 100L
        private const val BYTES_PER_MB = 1024 * 1024
        private const val MAX_CACHE_SIZE_BYTES = MAX_CACHE_SIZE_MB * BYTES_PER_MB
        private const val COMPRESSION_QUALITY = 90
        private const val CLEANUP_THRESHOLD = 0.8
        private const val KB_CONVERSION = 1024
        private const val CACHE_SIZE_RECALC_INTERVAL_MS = 30000L

        fun initialize(context: Context) {
            INSTANCE = PersistentImageCache(context.applicationContext)
        }

        fun getInstance(): PersistentImageCache? = INSTANCE
    }

    /**
     * Load image from disk cache
     */
    suspend fun loadFromDisk(cacheKey: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(cacheKey)
            if (cacheFile.exists() && cacheFile.isFile && cacheFile.length() > 0) {
                val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                bitmap?.let {
                    it.asImageBitmap()
                }
            } else {
                null
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            null
        }
    }

    /**
     * Save image to disk cache
     */
    suspend fun saveToDisk(cacheKey: String, bitmap: ImageBitmap) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(cacheKey)

            // Convert ImageBitmap to Android Bitmap for saving
            val androidBitmap = bitmap.asAndroidBitmap()

            FileOutputStream(cacheFile).use { outputStream ->
                // Save as PNG for lossless compression
                androidBitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream)
            }

            // Update estimated cache size and trigger async cleanup if needed
            val fileSize = cacheFile.length()
            estimatedCacheSize.addAndGet(fileSize)

            // Trigger async cleanup if we might be over the limit
            if (estimatedCacheSize.get() > MAX_CACHE_SIZE_BYTES) {
                backgroundScope.launch {
                    cleanupIfNeeded()
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    /**
     * Load raw image data from disk cache
     */
    suspend fun loadRawDataFromDisk(cacheKey: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getRawDataCacheFile(cacheKey)
            if (cacheFile.exists() && cacheFile.isFile && cacheFile.length() > 0) {
                cacheFile.readBytes()
            } else {
                null
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            null
        }
    }

    /**
     * Save raw image data to disk cache
     */
    suspend fun saveRawDataToDisk(cacheKey: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getRawDataCacheFile(cacheKey)
            cacheFile.writeBytes(data)

            // Update estimated cache size and trigger async cleanup if needed
            estimatedCacheSize.addAndGet(data.size.toLong())

            // Trigger async cleanup if we might be over the limit
            if (estimatedCacheSize.get() > MAX_CACHE_SIZE_BYTES) {
                backgroundScope.launch {
                    cleanupIfNeeded()
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    private fun getCacheFile(cacheKey: String): File {
        val hashedKey = hashCacheKey(cacheKey)
        return File(cacheDir, "$hashedKey.png")
    }

    private fun getRawDataCacheFile(cacheKey: String): File {
        val hashedKey = hashCacheKey(cacheKey)
        return File(cacheDir, "$hashedKey.raw")
    }

    private fun hashCacheKey(cacheKey: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(cacheKey.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private suspend fun cleanupIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            // Only recalculate cache size periodically to avoid excessive file system operations
            if (now - lastSizeCalculation > CACHE_SIZE_RECALC_INTERVAL_MS) {
                val actualSize = calculateCacheSize()
                estimatedCacheSize.set(actualSize)
                lastSizeCalculation = now
            }

            val currentSize = estimatedCacheSize.get()
            if (currentSize > MAX_CACHE_SIZE_BYTES) {
                // Get all cache files sorted by last modified time (oldest first)
                val cacheFiles = cacheDir.listFiles()
                    ?.filter { it.isFile }
                    ?.sortedBy { it.lastModified() }
                    ?: return@withContext

                var deletedSize = 0L
                var deletedCount = 0

                for (file in cacheFiles) {
                    val remainingSize = currentSize - deletedSize
                    if (remainingSize <= MAX_CACHE_SIZE_BYTES * CLEANUP_THRESHOLD) break

                    val fileSize = file.length()
                    if (file.delete()) {
                        deletedSize += fileSize
                        deletedCount++
                    }
                }

                // Update estimated cache size with deleted amount
                estimatedCacheSize.addAndGet(-deletedSize)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    private fun calculateCacheSize(): Long {
        return try {
            cacheDir.listFiles()
                ?.filter { it.isFile }
                ?.sumOf { it.length() }
                ?: 0L
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            0L
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= BYTES_PER_MB -> "${bytes / BYTES_PER_MB}MB"
            bytes >= KB_CONVERSION -> "${bytes / KB_CONVERSION}KB"
            else -> "${bytes}B"
        }
    }

    /**
     * Clear all cached files
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            val deletedCount = cacheDir.listFiles()?.count { it.delete() } ?: 0
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): String {
        return try {
            val files = cacheDir.listFiles() ?: arrayOf()
            val totalSize = files.filter { it.isFile }.sumOf { it.length() }
            val pngFiles = files.count { it.name.endsWith(".png") }
            val rawFiles = files.count { it.name.endsWith(".raw") }

            "Disk cache: $pngFiles images, $rawFiles raw files, " +
                    "${formatBytes(totalSize)}/${formatBytes(MAX_CACHE_SIZE_BYTES)}"
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            "Disk cache: Error reading stats"
        }
    }
}
