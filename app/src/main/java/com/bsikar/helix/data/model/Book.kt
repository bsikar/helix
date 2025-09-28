package com.bsikar.helix.data.model

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
enum class ReadingStatus {
    UNREAD,        // Book is imported but not organized yet
    PLAN_TO_READ,  // User explicitly added to reading plan  
    READING,       // Currently reading
    COMPLETED      // Finished reading
}

@Serializable
enum class CoverDisplayMode {
    AUTO, // Use cover art if available, fallback to color
    COLOR_ONLY, // Always use color
    COVER_ART_ONLY // Use cover art if available, otherwise show placeholder
}

@Serializable
data class Book(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val coverColor: Long, // Store as Long for serialization
    val progress: Float = 0f,
    val lastReadTimestamp: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val currentChapter: Int = 1,
    val currentPage: Int = 1,
    val scrollPosition: Int = 0,
    val totalPages: Int = 0, // Will be set based on actual chapters, no fake defaults
    val tags: List<String> = emptyList(), // List of tag IDs
    val originalMetadataTags: List<String> = emptyList(), // Original metadata tags for reference
    
    // EPUB-specific fields
    val filePath: String? = null, // Primary path to EPUB file (original location when possible)
    val originalUri: String? = null, // Original URI for SAF-based imports (content://)
    val backupFilePath: String? = null, // Backup copy in app storage (if needed)
    val fileSize: Long = 0L,
    val totalChapters: Int = 1,
    val description: String? = null,
    val publisher: String? = null,
    val language: String? = null,
    val isbn: String? = null,
    val publishedDate: String? = null,
    val coverImagePath: String? = null,
    val isImported: Boolean = false, // true for imported EPUBs
    val fileChecksum: String? = null, // SHA-256 checksum to detect file changes
    val userEditedMetadata: Boolean = false, // Track if user has edited metadata
    
    // Cover display preferences
    val coverDisplayMode: CoverDisplayMode = CoverDisplayMode.AUTO,
    val userSelectedColor: Long? = null, // Override color selected by user
    
    // Explicit reading status (for new books, defaults to UNREAD)
    val explicitReadingStatus: ReadingStatus? = null // null for backward compatibility
) {
    // Convenience property for UI
    val coverColorComposeColor: Color
        get() = Color(coverColor.toULong())
    
    /**
     * Get the effective cover color based on user preferences and fallback logic
     */
    fun getEffectiveCoverColor(): Color {
        return when (coverDisplayMode) {
            CoverDisplayMode.COLOR_ONLY -> {
                // Always use color (user selected or default)
                val colorValue = userSelectedColor ?: coverColor
                Color(colorValue.toULong())
            }
            CoverDisplayMode.AUTO -> {
                // Use cover art if available, fallback to color
                if (hasCoverArt()) {
                    // This will be handled by UI layer for actual image display
                    // Return transparent or a placeholder for color-based backgrounds
                    Color.Transparent
                } else {
                    val colorValue = userSelectedColor ?: coverColor
                    Color(colorValue.toULong())
                }
            }
            CoverDisplayMode.COVER_ART_ONLY -> {
                // Use cover art if available, otherwise show placeholder
                if (hasCoverArt()) {
                    Color.Transparent
                } else {
                    // Show a neutral placeholder color
                    Color(0xFF424242UL)
                }
            }
        }
    }
    
    /**
     * Check if book has cover art available
     */
    fun hasCoverArt(): Boolean {
        return !coverImagePath.isNullOrBlank() && 
               coverImagePath?.let { File(it).exists() } == true
    }
    
    /**
     * Determine if we should show cover art image
     */
    fun shouldShowCoverArt(): Boolean {
        return when (coverDisplayMode) {
            CoverDisplayMode.COLOR_ONLY -> false
            CoverDisplayMode.AUTO -> hasCoverArt()
            CoverDisplayMode.COVER_ART_ONLY -> hasCoverArt()
        }
    }
        
    companion object {
        fun fromColor(color: Color): Long = color.value.toLong()
    }
    val readingStatus: ReadingStatus
        get() = when {
            // First check explicit status if set by user
            explicitReadingStatus != null -> explicitReadingStatus!!
            // Fall back to progress-based logic only if no explicit status
            progress >= 1f -> ReadingStatus.COMPLETED
            progress > 0f -> ReadingStatus.READING
            else -> ReadingStatus.UNREAD
        }

    fun getTimeAgoText(): String {
        if (lastReadTimestamp == 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - lastReadTimestamp
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }
    
    /**
     * Get the actual Tag objects from tag IDs
     */
    fun getTagObjects(): List<Tag> {
        return tags.mapNotNull { tagId -> PresetTags.findTagById(tagId) }
    }
    
    /**
     * Check if book has a specific tag
     */
    fun hasTag(tagId: String): Boolean {
        return tags.contains(tagId)
    }
    
    /**
     * Check if book has any tags from a list
     */
    fun hasAnyTag(tagIds: List<String>): Boolean {
        return tags.any { tagIds.contains(it) }
    }
    
    /**
     * Get tags by category
     */
    fun getTagsByCategory(category: TagCategory): List<Tag> {
        return getTagObjects().filter { it.category == category }
    }
}