package com.bsikar.helix.data.source.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: Long,
    val description: String? = null,
    val isCustom: Boolean = false
)