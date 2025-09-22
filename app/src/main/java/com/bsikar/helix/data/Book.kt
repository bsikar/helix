package com.bsikar.helix.data

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
enum class ReadingStatus {
    PLAN_TO_READ,
    READING,
    COMPLETED
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
    val totalPages: Int = 150,
    val tags: List<String> = emptyList(), // List of tag IDs
    val originalMetadataTags: List<String> = emptyList(), // Original metadata tags for reference
    
    // EPUB-specific fields
    val filePath: String? = null, // Path to EPUB file
    val fileSize: Long = 0L,
    val totalChapters: Int = 1,
    val description: String? = null,
    val publisher: String? = null,
    val language: String? = null,
    val isbn: String? = null,
    val publishedDate: String? = null,
    val coverImagePath: String? = null,
    val isImported: Boolean = false // true for real EPUBs, false for fake data
) {
    // Convenience property for UI
    val coverColorComposeColor: Color
        get() = Color(coverColor)
        
    companion object {
        fun fromColor(color: Color): Long = color.value.toLong()
    }
    val readingStatus: ReadingStatus
        get() = when {
            progress == 0f -> ReadingStatus.PLAN_TO_READ
            progress >= 1f -> ReadingStatus.COMPLETED
            else -> ReadingStatus.READING
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