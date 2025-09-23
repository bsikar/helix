package com.bsikar.helix.data.source.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "imported_files")
data class ImportedFileEntity(
    @PrimaryKey val path: String,
    val originalPath: String? = null,
    val importedAt: Long = System.currentTimeMillis(),
    val bookId: String? = null,
    val sourceType: String = "individual",
    val sourceUri: String? = null
)