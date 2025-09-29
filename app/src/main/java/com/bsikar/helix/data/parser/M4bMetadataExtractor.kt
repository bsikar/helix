package com.bsikar.helix.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import com.bsikar.helix.data.model.AudioChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Advanced M4B metadata extractor using MediaMetadataRetriever for comprehensive metadata extraction
 */
class M4bMetadataExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "M4bMetadataExtractor"
        private const val COVER_QUALITY = 90
        private const val COVER_MAX_SIZE = 1024 // Max width/height for cover art
    }
    
    /**
     * Extract comprehensive metadata from M4B file using MediaMetadataRetriever
     */
    suspend fun extractMetadata(file: File): M4bMetadata = withContext(Dispatchers.IO) {
        var title = file.nameWithoutExtension
        var author: String? = null
        var album: String? = null
        var genre: String? = null
        var year: String? = null
        var description: String? = null
        var coverArt: ByteArray? = null
        var narrator: String? = null
        
        try {
            // Use MediaMetadataRetriever for metadata extraction
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                
                // Extract all available metadata
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { 
                    if (it.isNotBlank()) title = it
                }
                
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                    if (it.isNotBlank()) author = it
                }
                
                // Also check albumartist which is often used for audiobook authors
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.let {
                    if (it.isNotBlank() && author == null) author = it
                }
                
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                    if (it.isNotBlank()) album = it
                }
                
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let {
                    if (it.isNotBlank()) genre = it
                }
                
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.let {
                    if (it.isNotBlank()) year = it
                }
                
                // Try to get date as well
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.let {
                    if (it.isNotBlank() && year == null) {
                        // Extract year from date string (usually in format YYYYMMDD or YYYY-MM-DD)
                        year = it.take(4)
                    }
                }
                
                // Composer is sometimes used for narrator in audiobooks
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)?.let {
                    if (it.isNotBlank()) narrator = it
                }
                
                // Extract cover art
                val embeddedPicture = retriever.embeddedPicture
                if (embeddedPicture != null) {
                    coverArt = embeddedPicture
                    Log.d(TAG, "Found embedded cover art: ${embeddedPicture.size} bytes")
                } else {
                    Log.d(TAG, "No embedded cover art found in M4B file")
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Failed to extract metadata using MediaMetadataRetriever: ${e.message}")
            } finally {
                retriever.release()
            }
            
            // If no author found, try parsing from filename
            if (author == null) {
                val (parsedTitle, parsedAuthor) = parseFilename(file.nameWithoutExtension)
                if (title == file.nameWithoutExtension) {
                    title = parsedTitle
                }
                author = parsedAuthor
            }
            
            // Build description from available metadata
            if (description == null) {
                val descParts = mutableListOf<String>()
                narrator?.let { descParts.add("Narrated by $it") }
                album?.let { if (it != title) descParts.add("Album: $it") }
                if (descParts.isNotEmpty()) {
                    description = descParts.joinToString("\n")
                }
            }
            
            Log.d(TAG, "Extracted metadata - Title: $title, Author: $author, Album: $album, Genre: $genre, Year: $year, Cover: ${coverArt != null}")
            Log.d(TAG, "Cover art size: ${coverArt?.size ?: 0} bytes")
            
            return@withContext M4bMetadata(
                title = title,
                author = author,
                album = album,
                genre = genre,
                year = year,
                description = description,
                coverArt = coverArt
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract metadata from M4B file", e)
            // Return basic metadata from filename
            val (parsedTitle, parsedAuthor) = parseFilename(file.nameWithoutExtension)
            return@withContext M4bMetadata(
                title = parsedTitle,
                author = parsedAuthor,
                album = null,
                genre = "Audiobook",
                year = null,
                description = null,
                coverArt = null
            )
        }
    }
    
    /**
     * Parse filename to extract title and author
     */
    private fun parseFilename(fileName: String): Pair<String, String> {
        return when {
            " - " in fileName -> {
                val parts = fileName.split(" - ", limit = 2)
                if (parts.size == 2) {
                    // Check if it's "Author - Title" or "Title - Author" format
                    // Usually audiobooks are "Author - Title"
                    parts[1] to parts[0] // title to author
                } else {
                    fileName to "Unknown Author"
                }
            }
            " by " in fileName -> {
                val parts = fileName.split(" by ", limit = 2)
                if (parts.size == 2) {
                    parts[0] to parts[1] // title to author
                } else {
                    fileName to "Unknown Author"
                }
            }
            else -> fileName to "Unknown Author"
        }
    }
    
    /**
     * Extract chapter information from M4B file
     */
    suspend fun extractChapters(file: File): List<AudioChapter> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<AudioChapter>()
        
        try {
            // Use MediaExtractor to get track information
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            
            // Find the audio track
            var audioTrackIndex = -1
            var totalDurationUs = 0L
            
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    totalDurationUs = format.getLong(MediaFormat.KEY_DURATION)
                    break
                }
            }
            
            if (audioTrackIndex >= 0) {
                // Check for chapter markers in the format
                val format = extractor.getTrackFormat(audioTrackIndex)
                
                // For now, create a single chapter for the entire audiobook
                // In the future, we can enhance this to detect actual chapter markers
                chapters.add(
                    AudioChapter(
                        id = "${file.nameWithoutExtension}_chapter_1",
                        title = "Full Audiobook",
                        startTimeMs = 0L,
                        durationMs = totalDurationUs / 1000, // Convert from microseconds to milliseconds
                        order = 1,
                        bookId = ""
                    )
                )
                
                Log.d(TAG, "Created ${chapters.size} chapter(s) with total duration: ${totalDurationUs / 1000}ms")
            } else {
                Log.w(TAG, "No audio track found in M4B file")
                // Create default chapter
                chapters.add(createDefaultChapter(file.nameWithoutExtension))
            }
            
            extractor.release()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract chapters", e)
            // Create default chapter on error
            chapters.add(createDefaultChapter(file.nameWithoutExtension))
        }
        
        return@withContext chapters
    }
    
    /**
     * Create a default chapter when extraction fails
     */
    private fun createDefaultChapter(fileName: String): AudioChapter {
        return AudioChapter(
            id = "${fileName}_chapter_1",
            title = "Full Audiobook",
            startTimeMs = 0L,
            durationMs = 3600000L, // Default 1 hour
            order = 1,
            bookId = ""
        )
    }
    
    /**
     * Save cover art to file
     */
    suspend fun saveCoverArt(coverData: ByteArray, bookId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save cover art for book $bookId, data size: ${coverData.size} bytes")
            
            // Decode and resize the image to save space
            val bitmap = BitmapFactory.decodeByteArray(coverData, 0, coverData.size)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode cover art bitmap from ${coverData.size} bytes")
                return@withContext null
            }
            
            Log.d(TAG, "Decoded bitmap: ${bitmap.width}x${bitmap.height}")
            val resizedBitmap = resizeBitmap(bitmap, COVER_MAX_SIZE)
            
            // Create covers directory
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) {
                val created = coversDir.mkdirs()
                Log.d(TAG, "Created covers directory: $created")
            }
            
            // Save the cover image
            val coverFile = File(coversDir, "$bookId.jpg")
            FileOutputStream(coverFile).use { fos ->
                val compressed = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, fos)
                Log.d(TAG, "Compressed and saved: $compressed")
            }
            
            Log.d(TAG, "Successfully saved cover art to: ${coverFile.absolutePath}, file exists: ${coverFile.exists()}, size: ${coverFile.length()}")
            return@withContext coverFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cover art", e)
            return@withContext null
        }
    }
    
    /**
     * Resize bitmap to maximum dimensions while maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val scale = if (width > height) {
            maxSize.toFloat() / width.toFloat()
        } else {
            maxSize.toFloat() / height.toFloat()
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Extract accurate duration from M4B file
     */
    suspend fun extractDuration(file: File): Long = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            
            val duration = durationStr?.toLongOrNull() ?: 0L
            Log.d(TAG, "Extracted duration: ${duration}ms")
            return@withContext duration
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract duration", e)
            return@withContext 0L
        }
    }
}