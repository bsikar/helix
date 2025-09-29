package com.bsikar.helix.data.parser

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import com.bsikar.helix.data.model.AudioChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.min

/**
 * Enhanced chapter extractor that creates meaningful chapter divisions for audiobooks
 */
class ChapterExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "ChapterExtractor"
        private const val DEFAULT_CHAPTER_LENGTH_MS = 30 * 60 * 1000L // 30 minutes per chapter
        private const val MIN_CHAPTER_LENGTH_MS = 10 * 60 * 1000L // Minimum 10 minutes
        private const val MAX_CHAPTERS = 50 // Maximum number of chapters to create
    }
    
    /**
     * Extract or generate chapters for an audiobook
     */
    suspend fun extractChapters(file: File, bookId: String): List<AudioChapter> = withContext(Dispatchers.IO) {
        try {
            // First try to get actual chapter markers if they exist
            val embeddedChapters = extractEmbeddedChapters(file, bookId)
            if (embeddedChapters.isNotEmpty()) {
                Log.d(TAG, "Found ${embeddedChapters.size} embedded chapters")
                return@withContext embeddedChapters
            }
            
            // If no embedded chapters, create logical divisions
            val generatedChapters = generateChapters(file, bookId)
            Log.d(TAG, "Generated ${generatedChapters.size} chapters")
            return@withContext generatedChapters
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract chapters: ${e.message}", e)
            // Return a single chapter as fallback
            return@withContext listOf(
                AudioChapter(
                    id = "${bookId}_chapter_1",
                    title = "Full Audiobook",
                    startTimeMs = 0L,
                    durationMs = getAudioDuration(file),
                    order = 1,
                    bookId = bookId
                )
            )
        }
    }
    
    /**
     * Try to extract embedded chapter information from the file
     */
    private suspend fun extractEmbeddedChapters(file: File, bookId: String): List<AudioChapter> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<AudioChapter>()
        
        try {
            // Use MediaMetadataRetriever to check for chapter metadata
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            
            // Some M4B files have chapter info in metadata
            // Check for common chapter metadata keys
            val chapterCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()
            
            retriever.release()
            
            // If we found chapter markers, return them
            // (In a real implementation, we'd need to parse actual chapter data from the M4B container)
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract embedded chapters: ${e.message}")
        }
        
        return@withContext chapters
    }
    
    /**
     * Generate logical chapter divisions based on duration
     */
    private suspend fun generateChapters(file: File, bookId: String): List<AudioChapter> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<AudioChapter>()
        val totalDuration = getAudioDuration(file)
        
        if (totalDuration <= 0) {
            // If we can't get duration, create a single chapter
            return@withContext listOf(
                AudioChapter(
                    id = "${bookId}_chapter_1",
                    title = "Chapter 1",
                    startTimeMs = 0L,
                    durationMs = DEFAULT_CHAPTER_LENGTH_MS,
                    order = 1,
                    bookId = bookId
                )
            )
        }
        
        // Calculate optimal chapter length
        val optimalChapterLength = calculateOptimalChapterLength(totalDuration)
        val numChapters = min(
            MAX_CHAPTERS,
            ceil(totalDuration.toDouble() / optimalChapterLength.toDouble()).toInt()
        )
        
        // Create chapters with appropriate names
        var currentPosition = 0L
        for (i in 1..numChapters) {
            val chapterDuration = if (i == numChapters) {
                // Last chapter gets remaining time
                totalDuration - currentPosition
            } else {
                optimalChapterLength
            }
            
            chapters.add(
                AudioChapter(
                    id = "${bookId}_chapter_$i",
                    title = if (numChapters == 1) "Full Audiobook" else "Chapter $i",
                    startTimeMs = currentPosition,
                    durationMs = chapterDuration,
                    order = i,
                    bookId = bookId
                )
            )
            
            currentPosition += chapterDuration
        }
        
        return@withContext chapters
    }
    
    /**
     * Calculate optimal chapter length based on total duration
     */
    private fun calculateOptimalChapterLength(totalDuration: Long): Long {
        return when {
            // Short audiobook (< 2 hours): single chapter or 30-minute chapters
            totalDuration <= 2 * 60 * 60 * 1000L -> {
                if (totalDuration <= 45 * 60 * 1000L) {
                    totalDuration // Single chapter
                } else {
                    DEFAULT_CHAPTER_LENGTH_MS
                }
            }
            // Medium audiobook (2-6 hours): 30-45 minute chapters
            totalDuration <= 6 * 60 * 60 * 1000L -> {
                35 * 60 * 1000L // 35 minutes
            }
            // Long audiobook (6-12 hours): 45-60 minute chapters
            totalDuration <= 12 * 60 * 60 * 1000L -> {
                50 * 60 * 1000L // 50 minutes
            }
            // Very long audiobook (> 12 hours): 60-90 minute chapters
            else -> {
                75 * 60 * 1000L // 75 minutes
            }
        }
    }
    
    /**
     * Get audio duration from file
     */
    private fun getAudioDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio duration: ${e.message}")
            DEFAULT_CHAPTER_LENGTH_MS
        }
    }
}