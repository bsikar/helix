package com.bsikar.helix.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track import tasks and their progress
 */
@Entity(tableName = "import_tasks")
data class ImportTask(
    @PrimaryKey val id: String,
    val fileName: String,
    val fileUri: String,
    val status: ImportStatus,
    val progress: Int = 0,
    val progressMessage: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val errorMessage: String? = null,
    val bookId: String? = null,
    val workerId: String? = null
)

/**
 * Status of an import operation
 */
enum class ImportStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * UI state for import operations
 */
data class ImportProgress(
    val id: String,
    val fileName: String,
    val status: ImportStatus,
    val progress: Int = 0,
    val message: String = "",
    val isVisible: Boolean = true
)

/**
 * Result of an import operation
 */
data class ImportResult(
    val success: Boolean,
    val book: Book? = null,
    val errorMessage: String? = null,
    val importId: String,
    val chaptersImported: Int = 0,
    val status: ImportStatus = ImportStatus.IN_PROGRESS
)