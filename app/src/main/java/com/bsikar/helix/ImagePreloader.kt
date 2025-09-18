package com.bsikar.helix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Preloads images from upcoming chapters to improve perceived performance
 */
@Suppress("TooGenericExceptionCaught")
class ImagePreloader private constructor() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val imageLoader = OptimizedImageLoader.getInstance()
    private val cache = ImageCache.getInstance()
    private val zipCache = ZipDirectoryCache.getInstance()

    private var currentPreloadJob: Job? = null

    companion object {
        @Volatile
        private var INSTANCE: ImagePreloader? = null

        fun getInstance(): ImagePreloader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImagePreloader().also { INSTANCE = it }
            }
        }
    }

    /**
     * Preload images from the next few chapters
     */
    fun preloadUpcomingImages(
        epubFile: File,
        currentChapterIndex: Int,
        chapters: List<RichEpubChapter>,
        chaptersToPreload: Int = 2
    ) {
        // Cancel any existing preload job
        currentPreloadJob?.cancel()

        currentPreloadJob = scope.launch {
            try {
                val endIndex = minOf(currentChapterIndex + chaptersToPreload, chapters.size - 1)

                for (chapterIndex in (currentChapterIndex + 1)..endIndex) {
                    val chapter = chapters[chapterIndex]
                    val imageElements = chapter.elements.filterIsInstance<ContentElement.Image>()

                    if (imageElements.isNotEmpty()) {
                        for (imageElement in imageElements) {
                            // Check if already cached
                            val cacheKey = cache.createCacheKey(epubFile.absolutePath, imageElement.src)
                            if (cache.getCachedBitmap(cacheKey) == null) {
                                try {
                                    imageLoader.loadImage(epubFile, imageElement.src)
                                } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
                                }
                            }
                        }
                    }
                }
            } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            }
        }
    }

    /**
     * Preload all images from the current EPUB file in the background
     */
    fun preloadAllImages(epubFile: File) {
        currentPreloadJob?.cancel()

        currentPreloadJob = scope.launch {
            try {
                val allImages = zipCache.getAllImages(epubFile)

                for (imagePath in allImages) {
                    val cacheKey = cache.createCacheKey(epubFile.absolutePath, imagePath)
                    if (cache.getCachedRawData(cacheKey) == null) {
                        try {
                            // Only load raw data for now to save memory
                            val imageData = extractRawImageData(epubFile, imagePath)
                            if (imageData != null) {
                                cache.cacheRawData(cacheKey, imageData)
                            }
                        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
                        }
                    }
                }
            } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private fun extractRawImageData(epubFile: File, imagePath: String): ByteArray? {
        return try {
            val actualEntryName = zipCache.findEntryName(epubFile, listOf(imagePath))
            if (actualEntryName != null) {
                java.util.zip.ZipFile(epubFile).use { zipFile ->
                    val entry = zipFile.getEntry(actualEntryName)
                    entry?.let {
                        zipFile.getInputStream(it).use { inputStream ->
                            inputStream.readBytes()
                        }
                    }
                }
            } else {
                null
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            null
        }
    }

    /**
     * Cancel any ongoing preload operations
     */
    fun cancelPreloading() {
        currentPreloadJob?.cancel()
        currentPreloadJob = null
    }

    /**
     * Get preloading status
     */
    fun isPreloading(): Boolean {
        return currentPreloadJob?.isActive == true
    }
}
