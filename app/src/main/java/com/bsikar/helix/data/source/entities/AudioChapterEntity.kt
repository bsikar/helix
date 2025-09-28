package com.bsikar.helix.data.source.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "audio_chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class AudioChapterEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val startTimeMs: Long,
    val durationMs: Long,
    @ColumnInfo(name = "chapter_order") val order: Int,
    val bookId: String
)