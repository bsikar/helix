package com.bsikar.helix.data.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val bookTitle: String,
    val chapterNumber: Int,
    val pageNumber: Int,
    val scrollPosition: Int = 0,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getDisplayText(): String {
        return if (note.isNotBlank()) {
            note
        } else {
            "Chapter $chapterNumber, Page $pageNumber"
        }
    }
    
    fun getTimeAgoText(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
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
}