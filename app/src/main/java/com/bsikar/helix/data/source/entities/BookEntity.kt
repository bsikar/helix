package com.bsikar.helix.data.source.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val coverColor: Long,
    val progress: Float = 0f,
    val lastReadTimestamp: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val currentChapter: Int = 1,
    val currentPage: Int = 1,
    val scrollPosition: Int = 0,
    val totalPages: Int = 0,
    val tags: String = "", // JSON string of tag IDs
    val originalMetadataTags: String = "", // JSON string of metadata tags
    
    // EPUB-specific fields
    val filePath: String? = null,
    val originalUri: String? = null,
    val backupFilePath: String? = null,
    val fileSize: Long = 0L,
    val totalChapters: Int = 1,
    val description: String? = null,
    val publisher: String? = null,
    val language: String? = null,
    val isbn: String? = null,
    val publishedDate: String? = null,
    val coverImagePath: String? = null,
    val isImported: Boolean = false,
    
    // Cover display settings
    val coverDisplayMode: String = "AUTO", // CoverDisplayMode enum as string
    val userSelectedColor: Long? = null,
    val fileChecksum: String? = null,
    val userEditedMetadata: Boolean = false
)