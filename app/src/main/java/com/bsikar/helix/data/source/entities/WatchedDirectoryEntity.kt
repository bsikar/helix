package com.bsikar.helix.data.source.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_directories")
data class WatchedDirectoryEntity(
    @PrimaryKey val path: String,
    val uri: String? = null, 
    val lastScanned: Long = System.currentTimeMillis(),
    val recursive: Boolean = true,
    val totalBooks: Int = 0,
    val isUri: Boolean = uri != null,
    val requiresRescan: Boolean = false
)