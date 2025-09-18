package com.bsikar.helix

import android.app.ActivityManager
import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Suppress("TooManyFunctions")
class ImageCache private constructor() {
    private val bitmapCache = LinkedHashMap<String, CacheEntry<ImageBitmap>>(INITIAL_CAPACITY, LOAD_FACTOR, true)
    private val rawDataCache = LinkedHashMap<String, CacheEntry<ByteArray>>(INITIAL_CAPACITY, LOAD_FACTOR, true)
    private val lock = ReentrantLock()

    private var maxBitmapCacheSize = calculateOptimalBitmapCacheSize()
    private var maxRawDataCacheSize = calculateOptimalRawDataCacheSize()
    private var currentBitmapMemoryUsage = 0L
    private var currentRawDataMemoryUsage = 0L

    companion object {
        const val MIN_CACHE_SIZE = 10
        const val MAX_CACHE_SIZE = 100
        const val DEFAULT_CACHE_SIZE = 50
        private const val BITMAP_BYTES_PER_PIXEL = 2 // RGB_565 format
        private const val MEMORY_CACHE_PERCENTAGE = 0.15f // Use 15% of available memory
        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
        private const val PERCENTAGE_DIVIDER = 2L
        private const val MB_CONVERSION = 1024 * 1024
        private const val KB_CONVERSION = 1024

        @Volatile
        private var INSTANCE: ImageCache? = null
        private var applicationContext: Context? = null

        fun initialize(context: Context) {
            applicationContext = context.applicationContext
        }

        fun getInstance(): ImageCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageCache().also { INSTANCE = it }
            }
        }
    }

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val memorySize: Long = 0L
    )

    fun getCachedBitmap(key: String): ImageBitmap? {
        return lock.withLock {
            bitmapCache[key]?.data
        }
    }

    fun getCachedRawData(key: String): ByteArray? {
        return lock.withLock {
            rawDataCache[key]?.data
        }
    }

    fun cacheBitmap(key: String, bitmap: ImageBitmap) {
        lock.withLock {
            val memorySize = estimateBitmapMemorySize(bitmap)

            // Evict entries if we exceed memory limits
            while (currentBitmapMemoryUsage + memorySize > maxBitmapCacheSize && bitmapCache.isNotEmpty()) {
                evictOldestBitmap()
            }

            // Remove existing entry if present
            bitmapCache.remove(key)?.let { oldEntry ->
                currentBitmapMemoryUsage -= oldEntry.memorySize
            }

            // Add new entry
            bitmapCache[key] = CacheEntry(bitmap, System.currentTimeMillis(), memorySize)
            currentBitmapMemoryUsage += memorySize
        }
    }

    fun cacheRawData(key: String, data: ByteArray) {
        lock.withLock {
            val memorySize = data.size.toLong()

            // Evict entries if we exceed memory limits
            while (currentRawDataMemoryUsage + memorySize > maxRawDataCacheSize && rawDataCache.isNotEmpty()) {
                evictOldestRawData()
            }

            // Remove existing entry if present
            rawDataCache.remove(key)?.let { oldEntry ->
                currentRawDataMemoryUsage -= oldEntry.memorySize
            }

            // Add new entry
            rawDataCache[key] = CacheEntry(data, System.currentTimeMillis(), memorySize)
            currentRawDataMemoryUsage += memorySize
        }
    }

    fun createCacheKey(epubPath: String, imagePath: String): String {
        return "${epubPath.hashCode()}_${imagePath.hashCode()}"
    }

    private fun evictOldestBitmap() {
        val oldestEntry = bitmapCache.entries.firstOrNull()
        if (oldestEntry != null) {
            bitmapCache.remove(oldestEntry.key)
            currentBitmapMemoryUsage -= oldestEntry.value.memorySize
        }
    }

    private fun evictOldestRawData() {
        val oldestEntry = rawDataCache.entries.firstOrNull()
        if (oldestEntry != null) {
            rawDataCache.remove(oldestEntry.key)
            currentRawDataMemoryUsage -= oldestEntry.value.memorySize
        }
    }

    private fun estimateBitmapMemorySize(bitmap: ImageBitmap): Long {
        return bitmap.width.toLong() * bitmap.height * BITMAP_BYTES_PER_PIXEL
    }

    private fun calculateOptimalBitmapCacheSize(): Long {
        val activityManager = applicationContext?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val availableMemory = memoryInfo.availMem
        val targetCacheSize = (availableMemory * MEMORY_CACHE_PERCENTAGE).toLong()

        return targetCacheSize
    }

    private fun calculateOptimalRawDataCacheSize(): Long {
        // Raw data cache can be larger since it's compressed
        return calculateOptimalBitmapCacheSize() / PERCENTAGE_DIVIDER
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= MB_CONVERSION -> "${bytes / MB_CONVERSION}MB"
            bytes >= KB_CONVERSION -> "${bytes / KB_CONVERSION}KB"
            else -> "${bytes}B"
        }
    }

    fun clearCache() {
        lock.withLock {
            bitmapCache.clear()
            rawDataCache.clear()
            currentBitmapMemoryUsage = 0L
            currentRawDataMemoryUsage = 0L
        }
    }

    fun getCacheStats(): String {
        return lock.withLock {
            "Bitmap cache: ${bitmapCache.size} items " +
            "(${formatBytes(currentBitmapMemoryUsage)}/${formatBytes(maxBitmapCacheSize)}), " +
            "Raw data cache: ${rawDataCache.size} items " +
            "(${formatBytes(currentRawDataMemoryUsage)}/${formatBytes(maxRawDataCacheSize)})"
        }
    }

    fun trimToSize() {
        lock.withLock {
            // Recalculate optimal sizes and trim if necessary
            val newBitmapCacheSize = calculateOptimalBitmapCacheSize()
            val newRawDataCacheSize = calculateOptimalRawDataCacheSize()

            maxBitmapCacheSize = newBitmapCacheSize
            maxRawDataCacheSize = newRawDataCacheSize

            // Trim bitmap cache
            while (currentBitmapMemoryUsage > maxBitmapCacheSize && bitmapCache.isNotEmpty()) {
                evictOldestBitmap()
            }

            // Trim raw data cache
            while (currentRawDataMemoryUsage > maxRawDataCacheSize && rawDataCache.isNotEmpty()) {
                evictOldestRawData()
            }
        }
    }
}
