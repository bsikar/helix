package com.bsikar.helix.data

import androidx.compose.ui.graphics.Color

data class RecentBook(
    val book: Book,
    val lastAccessTime: Long, // timestamp in milliseconds
    val lastReadPage: Int = 1,
    val totalPages: Int = 100
) {
    fun getTimeAgoText(): String {
        val now = System.currentTimeMillis()
        val diff = now - lastAccessTime
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
    
    fun getProgressPercentage(): Int {
        return ((lastReadPage.toFloat() / totalPages) * 100).toInt()
    }
}