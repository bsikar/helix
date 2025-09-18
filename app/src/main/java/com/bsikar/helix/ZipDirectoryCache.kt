package com.bsikar.helix

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

/**
 * Caches ZIP file directory structures to eliminate repeated zipFile.getEntry() calls
 * This provides massive performance improvements for repeated image lookups
 */
class ZipDirectoryCache private constructor() {
    private val directoryCache = ConcurrentHashMap<String, ZipDirectoryInfo>()

    companion object {
        @Volatile
        private var INSTANCE: ZipDirectoryCache? = null

        fun getInstance(): ZipDirectoryCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ZipDirectoryCache().also { INSTANCE = it }
            }
        }
    }

    /**
     * Get or build the directory information for a ZIP file
     */
    fun getDirectoryInfo(file: File): ZipDirectoryInfo {
        val cacheKey = "${file.absolutePath}_${file.lastModified()}"

        return directoryCache.computeIfAbsent(cacheKey) {
            buildDirectoryInfo(file)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun buildDirectoryInfo(file: File): ZipDirectoryInfo {
        val entryMap = mutableMapOf<String, String>() // lowercase key -> actual entry name
        val imageEntries = mutableSetOf<String>()
        val imageExtensions = setOf(".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp", ".bmp")

        try {
            ZipFile(file).use { zipFile ->
                zipFile.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory) {
                        val entryName = entry.name
                        val lowerName = entryName.lowercase()

                        // Store both exact and lowercase mappings for fast lookup
                        entryMap[entryName] = entryName
                        entryMap[lowerName] = entryName

                        // Track image files separately
                        if (imageExtensions.any { ext -> lowerName.endsWith(ext) }) {
                            imageEntries.add(entryName)
                        }
                    }
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }

        return ZipDirectoryInfo(entryMap, imageEntries)
    }

    /**
     * Find the actual entry name for a given path, trying multiple variations
     */
    @Suppress("ReturnCount")
    fun findEntryName(file: File, searchPaths: List<String>): String? {
        val directoryInfo = getDirectoryInfo(file)

        // Try each search path in order
        for (searchPath in searchPaths) {
            // Try exact match first
            directoryInfo.entryMap[searchPath]?.let {
                return it
            }

            // Try lowercase match
            directoryInfo.entryMap[searchPath.lowercase()]?.let {
                return it
            }
        }

        return null
    }

    /**
     * Find an image by filename (case-insensitive)
     */
    fun findImageByFilename(file: File, filename: String): String? {
        val directoryInfo = getDirectoryInfo(file)
        val targetFilename = filename.lowercase()

        return directoryInfo.imageEntries.find { entry ->
            entry.substringAfterLast('/').lowercase() == targetFilename
        }
    }

    /**
     * Get all image entries in the ZIP file
     */
    fun getAllImages(file: File): Set<String> {
        return getDirectoryInfo(file).imageEntries
    }

    /**
     * Clear cache for memory management
     */
    fun clearCache() {
        directoryCache.clear()
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): String {
        val totalEntries = directoryCache.values.sumOf { it.entryMap.size }
        val totalImages = directoryCache.values.sumOf { it.imageEntries.size }
        return "ZIP cache: ${directoryCache.size} files, $totalEntries entries, $totalImages images"
    }
}

/**
 * Contains cached directory information for a ZIP file
 */
data class ZipDirectoryInfo(
    val entryMap: Map<String, String>, // path -> actual entry name
    val imageEntries: Set<String> // all image file entries
)
