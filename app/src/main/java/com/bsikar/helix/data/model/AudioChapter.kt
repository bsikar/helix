package com.bsikar.helix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioChapter(
    val id: String,
    val title: String,
    val startTimeMs: Long,
    val durationMs: Long,
    val order: Int,
    val bookId: String
) {
    /**
     * Get formatted start time
     */
    fun getFormattedStartTime(): String {
        val hours = startTimeMs / (1000 * 60 * 60)
        val minutes = (startTimeMs % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (startTimeMs % (1000 * 60)) / 1000
        
        return when {
            hours > 0 -> "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
            minutes > 0 -> "${minutes}:${seconds.toString().padStart(2, '0')}"
            else -> "0:${seconds.toString().padStart(2, '0')}"
        }
    }
    
    /**
     * Get formatted duration
     */
    fun getFormattedDuration(): String {
        val hours = durationMs / (1000 * 60 * 60)
        val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
    
    /**
     * Get end time in milliseconds
     */
    val endTimeMs: Long
        get() = startTimeMs + durationMs
    
    /**
     * Check if a given position is within this chapter
     */
    fun containsPosition(positionMs: Long): Boolean {
        return positionMs >= startTimeMs && positionMs < endTimeMs
    }
}