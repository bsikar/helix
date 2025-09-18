package com.bsikar.helix

import android.graphics.Bitmap

/**
 * Detailed information about an EPUB book
 */
data class BookInfo(
    val title: String,
    val author: String?,
    val description: String?,
    val publisher: String?,
    val language: String?,
    val fileSize: String,
    val fileName: String,
    val filePath: String,
    val cover: Bitmap?,
    val chapterCount: Int = 0,
    val wordCount: Long = 0
) {
    fun getDisplayTitle(): String = title.ifBlank { fileName.removeSuffix(".epub") }

    fun getDisplayAuthor(): String = author ?: "Unknown Author"

    fun getDisplayDescription(): String = description ?: "No description available"
}
