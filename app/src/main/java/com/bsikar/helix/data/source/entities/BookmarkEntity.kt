package com.bsikar.helix.data.source.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val chapterNumber: Int,
    val pageNumber: Int,
    val scrollPosition: Int = 0,
    val note: String = "",
    val timestamp: Long
)

/**
 * Convert BookmarkEntity to Bookmark domain model
 */
fun BookmarkEntity.toBookmark(): com.bsikar.helix.data.model.Bookmark {
    return com.bsikar.helix.data.model.Bookmark(
        id = id,
        bookId = bookId,
        bookTitle = bookTitle,
        chapterNumber = chapterNumber,
        pageNumber = pageNumber,
        scrollPosition = scrollPosition,
        note = note,
        timestamp = timestamp
    )
}

/**
 * Convert Bookmark domain model to BookmarkEntity
 */
fun com.bsikar.helix.data.model.Bookmark.toEntity(): BookmarkEntity {
    return BookmarkEntity(
        id = id,
        bookId = bookId,
        bookTitle = bookTitle,
        chapterNumber = chapterNumber,
        pageNumber = pageNumber,
        scrollPosition = scrollPosition,
        note = note,
        timestamp = timestamp
    )
}