package com.bsikar.helix

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipFile

class OptimizedImageLoader private constructor() {
    private val cache = ImageCache.getInstance()
    private val zipCache = ZipDirectoryCache.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: OptimizedImageLoader? = null

        fun getInstance(): OptimizedImageLoader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OptimizedImageLoader().also { INSTANCE = it }
            }
        }

        private const val MAX_IMAGE_DIMENSION = 2048
    }

    // Pre-built common image path patterns for faster lookup
    private fun buildImagePathVariations(imagePath: String, chapterPath: String, baseDir: String): List<String> {
        val decodedPath = try {
            URLDecoder.decode(imagePath, "UTF-8")
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            imagePath
        }

        val imageFileName = imagePath.substringAfterLast('/')
        val decodedImageFileName = decodedPath.substringAfterLast('/')
        val chapterDir = chapterPath.substringBeforeLast('/')

        return listOf(
            imagePath, // Try exact path first
            decodedPath,
            // Chapter-relative paths
            if (chapterDir.isNotEmpty()) "$chapterDir/$imageFileName" else null,
            if (chapterDir.isNotEmpty()) "$chapterDir/$decodedImageFileName" else null,
            // Base directory paths
            if (baseDir.isNotEmpty()) "$baseDir/$imageFileName" else null,
            if (baseDir.isNotEmpty()) "$baseDir/$decodedImageFileName" else null,
            // Common EPUB image directories
            "images/$imageFileName",
            "Images/$imageFileName",
            "OEBPS/images/$imageFileName",
            "OEBPS/Images/$imageFileName",
            "content/images/$imageFileName",
            "text/images/$imageFileName",
            imageFileName,
            "images/$decodedImageFileName",
            "Images/$decodedImageFileName",
            "OEBPS/images/$decodedImageFileName",
            "OEBPS/Images/$decodedImageFileName",
            "content/images/$decodedImageFileName",
            "text/images/$decodedImageFileName",
            decodedImageFileName
        ).filterNotNull().distinct()
    }

    suspend fun loadImage(
        epubFile: File,
        imagePath: String,
        chapterPath: String = "",
        baseDir: String = ""
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = cache.createCacheKey(epubFile.absolutePath, imagePath)

        // 1. Check memory bitmap cache first (fastest)
        cache.getCachedBitmap(cacheKey)?.let {
            return@withContext it
        }

        // 2. Check persistent disk cache
        val persistentCache = PersistentImageCache.getInstance()
        if (persistentCache != null) {
            val diskBitmap = persistentCache.loadFromDisk(cacheKey)
            if (diskBitmap != null) {
                cache.cacheBitmap(cacheKey, diskBitmap) // Cache in memory too
                return@withContext diskBitmap
            }
        }

        // 3. Check raw data cache
        val cachedData = cache.getCachedRawData(cacheKey)
        if (cachedData != null) {
            val bitmap = decodeImageData(cachedData, cacheKey)
            if (bitmap != null && persistentCache != null) {
                // Save to disk cache for next time
                persistentCache.saveToDisk(cacheKey, bitmap)
            }
            return@withContext bitmap
        }

        // 4. Check persistent raw data cache
        if (persistentCache != null) {
            val diskRawData = persistentCache.loadRawDataFromDisk(cacheKey)
            if (diskRawData != null) {
                cache.cacheRawData(cacheKey, diskRawData) // Cache in memory too
                return@withContext decodeImageData(diskRawData, cacheKey)
            }
        }

        // 5. Load from ZIP file (slowest)
        val imageData = extractImageDataOptimized(epubFile, imagePath, chapterPath, baseDir)
        if (imageData != null) {
            cache.cacheRawData(cacheKey, imageData)

            // Save raw data to disk cache
            if (persistentCache != null) {
                persistentCache.saveRawDataToDisk(cacheKey, imageData)
            }

            val bitmap = decodeImageData(imageData, cacheKey)
            if (bitmap != null && persistentCache != null) {
                // Save decoded bitmap to disk cache
                persistentCache.saveToDisk(cacheKey, bitmap)
            }
            return@withContext bitmap
        }

        null
    }

    private suspend fun decodeImageData(
        data: ByteArray,
        cacheKey: String
    ): ImageBitmap? = withContext(Dispatchers.Default) {
        try {
            val options = BitmapFactory.Options().apply {
                // Sample down large images to save memory
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            // Calculate sample size for large images
            val sampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)

            options.apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory for most images
            }

            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            bitmap?.let {
                val imageBitmap = it.asImageBitmap()
                cache.cacheBitmap(cacheKey, imageBitmap)
                imageBitmap
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @Suppress("NestedBlockDepth")
    private fun extractImageDataOptimized(
        file: File,
        imagePath: String,
        chapterPath: String,
        baseDir: String
    ): ByteArray? {
        return try {
            ZipFile(file).use { zipFile ->
                extractFromZip(zipFile, imagePath, chapterPath, baseDir)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            null
        }
    }

    private fun extractFromZip(
        zipFile: ZipFile,
        imagePath: String,
        chapterPath: String,
        baseDir: String
    ): ByteArray? {
        val file = File(zipFile.name)
        val pathVariations = buildImagePathVariations(imagePath, chapterPath, baseDir)

        // Use ZIP directory cache for much faster lookups
        val actualEntryName = zipCache.findEntryName(file, pathVariations)
        if (actualEntryName != null) {
            val entry = zipFile.getEntry(actualEntryName)
            if (entry != null) {
                return zipFile.getInputStream(entry).use { inputStream ->
                    inputStream.readBytes()
                }
            }
        }

        // Last resort: partial matching by filename using cache
        return findByFilenameWithCache(zipFile, imagePath, file)
    }

    private fun findByFilenameWithCache(zipFile: ZipFile, imagePath: String, file: File): ByteArray? {
        val targetFileName = imagePath.substringAfterLast('/')
        val matchingEntryName = zipCache.findImageByFilename(file, targetFileName)

        if (matchingEntryName != null) {
            val entry = zipFile.getEntry(matchingEntryName)
            if (entry != null) {
                return zipFile.getInputStream(entry).use { inputStream ->
                    inputStream.readBytes()
                }
            }
        }

        return null
    }
}
