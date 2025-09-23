package com.bsikar.helix.data.source.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["lastUpdated"]),
        Index(value = ["bookId", "chapterIndex"], unique = false)
    ]
)
data class ReadingProgressEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val chapterTitle: String? = null,
    val scrollPosition: Int = 0,
    val pageNumber: Int = 1,
    val totalPagesInChapter: Int = 1,
    val readingTimeSeconds: Long = 0,
    val lastUpdated: Long,
    val isChapterCompleted: Boolean = false,
    val readingSpeedWpm: Float? = null,
    val notes: String? = null
)

/**
 * Convert ReadingProgressEntity to ReadingProgress domain model
 */
fun ReadingProgressEntity.toReadingProgress(): com.bsikar.helix.data.model.ReadingProgress {
    return com.bsikar.helix.data.model.ReadingProgress(
        id = id,
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        scrollPosition = scrollPosition,
        pageNumber = pageNumber,
        totalPagesInChapter = totalPagesInChapter,
        readingTimeSeconds = readingTimeSeconds,
        lastUpdated = lastUpdated,
        isChapterCompleted = isChapterCompleted,
        readingSpeedWpm = readingSpeedWpm,
        notes = notes
    )
}

/**
 * Convert ReadingProgress domain model to ReadingProgressEntity
 */
fun com.bsikar.helix.data.model.ReadingProgress.toEntity(): ReadingProgressEntity {
    return ReadingProgressEntity(
        id = id,
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        scrollPosition = scrollPosition,
        pageNumber = pageNumber,
        totalPagesInChapter = totalPagesInChapter,
        readingTimeSeconds = readingTimeSeconds,
        lastUpdated = lastUpdated,
        isChapterCompleted = isChapterCompleted,
        readingSpeedWpm = readingSpeedWpm,
        notes = notes
    )
}