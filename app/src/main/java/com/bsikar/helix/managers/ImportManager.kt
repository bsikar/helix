package com.bsikar.helix.managers

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.bsikar.helix.data.model.ImportTask
import com.bsikar.helix.data.model.ImportStatus
import com.bsikar.helix.data.model.ImportProgress
import com.bsikar.helix.data.source.dao.ImportTaskDao
import com.bsikar.helix.workers.EpubImportWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling EPUB imports in the background using WorkManager
 */
@Singleton
class ImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val importTaskDao: ImportTaskDao
) {
    
    // Create a supervisor scope for database operations
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Flow of all import tasks
     */
    val importTasks: Flow<List<ImportTask>> = importTaskDao.getAllImportTasks()

    /**
     * Flow of active import tasks
     */
    val activeImports: Flow<List<ImportTask>> = importTaskDao.getActiveImportTasks()

    /**
     * Flow of import progress for UI
     */
    val importProgress: Flow<List<ImportProgress>> = activeImports.map { tasks ->
        tasks.map { task ->
            ImportProgress(
                id = task.id,
                fileName = task.fileName,
                status = task.status,
                progress = task.progress,
                message = task.progressMessage,
                isVisible = task.status in listOf(ImportStatus.PENDING, ImportStatus.IN_PROGRESS)
            )
        }
    }

    /**
     * Start importing an EPUB file
     */
    suspend fun startImport(fileUri: Uri, fileName: String): String {
        val importId = UUID.randomUUID().toString()
        val fileUriString = fileUri.toString()

        // Check if file is already being imported
        val isAlreadyImporting = importTaskDao.isFileBeingImported(fileUriString) > 0
        if (isAlreadyImporting) {
            throw IllegalStateException("File is already being imported")
        }

        // Create import task record
        val importTask = ImportTask(
            id = importId,
            fileName = fileName,
            fileUri = fileUriString,
            status = ImportStatus.PENDING,
            progress = 0,
            progressMessage = "Queued for import"
        )
        importTaskDao.insertImportTask(importTask)

        // Create and enqueue work request
        val workRequest = EpubImportWorker.createWorkRequest(fileUriString, fileName)
        
        // Update task with worker ID
        val updatedTask = importTask.copy(workerId = workRequest.id.toString())
        importTaskDao.updateImportTask(updatedTask)

        // Enqueue the work
        workManager.enqueueUniqueWork(
            "epub_import_$importId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        // Observe work progress
        observeWorkProgress(workRequest.id, importId)

        return importId
    }

    /**
     * Cancel an import
     */
    suspend fun cancelImport(importId: String) {
        val task = importTaskDao.getImportTaskById(importId)
        if (task != null && task.workerId != null) {
            // Cancel the work
            workManager.cancelWorkById(UUID.fromString(task.workerId))
            
            // Update task status
            importTaskDao.updateStatus(importId, ImportStatus.CANCELLED)
        }
    }

    /**
     * Retry a failed import
     */
    suspend fun retryImport(importId: String): String? {
        val task = importTaskDao.getImportTaskById(importId) ?: return null
        
        if (task.status != ImportStatus.FAILED) {
            throw IllegalStateException("Can only retry failed imports")
        }

        // Create new import with same file
        return startImport(Uri.parse(task.fileUri), task.fileName)
    }

    /**
     * Clear completed imports
     */
    suspend fun clearCompletedImports() {
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days ago
        importTaskDao.clearOldTasks(cutoffTime)
    }

    /**
     * Get import task by ID
     */
    suspend fun getImportTask(importId: String): ImportTask? {
        return importTaskDao.getImportTaskById(importId)
    }

    /**
     * Get failed imports that can be retried
     */
    fun getFailedImports(): Flow<List<ImportTask>> {
        return importTaskDao.getFailedImportTasks()
    }

    /**
     * Check if any imports are currently active
     */
    fun hasActiveImports(): Flow<Boolean> {
        return activeImports.map { it.isNotEmpty() }
    }

    /**
     * Get import statistics
     */
    suspend fun getImportStats(): ImportStats {
        val pending = importTaskDao.getTaskCountByStatus(ImportStatus.PENDING)
        val inProgress = importTaskDao.getTaskCountByStatus(ImportStatus.IN_PROGRESS)
        val completed = importTaskDao.getTaskCountByStatus(ImportStatus.COMPLETED)
        val failed = importTaskDao.getTaskCountByStatus(ImportStatus.FAILED)
        val cancelled = importTaskDao.getTaskCountByStatus(ImportStatus.CANCELLED)

        return ImportStats(
            pending = pending,
            inProgress = inProgress,
            completed = completed,
            failed = failed,
            cancelled = cancelled,
            total = pending + inProgress + completed + failed + cancelled
        )
    }

    /**
     * Observe work progress and update database
     */
    private fun observeWorkProgress(workId: UUID, importId: String) {
        workManager.getWorkInfoByIdLiveData(workId).observeForever { workInfo ->
            when (workInfo?.state) {
                WorkInfo.State.ENQUEUED -> {
                    // Work is queued
                    managerScope.launch {
                        importTaskDao.updateStatus(importId, ImportStatus.PENDING)
                    }
                }
                WorkInfo.State.RUNNING -> {
                    // Work is running, update progress
                    managerScope.launch {
                        importTaskDao.updateStatus(importId, ImportStatus.IN_PROGRESS)
                        
                        // Extract progress data
                        val progress = workInfo.progress.getInt("progress", 0)
                        val message = workInfo.progress.getString("message") ?: "Processing..."
                        
                        importTaskDao.updateProgress(importId, progress, message)
                    }
                }
                WorkInfo.State.SUCCEEDED -> {
                    // Work completed successfully
                    managerScope.launch {
                        val bookId = workInfo.outputData.getString("book_id")
                        importTaskDao.updateCompletion(
                            importId,
                            ImportStatus.COMPLETED,
                            System.currentTimeMillis(),
                            bookId
                        )
                    }
                }
                WorkInfo.State.FAILED -> {
                    // Work failed
                    managerScope.launch {
                        val errorMessage = workInfo.outputData.getString("error_message") ?: "Import failed"
                        importTaskDao.updateError(importId, System.currentTimeMillis(), errorMessage)
                    }
                }
                WorkInfo.State.CANCELLED -> {
                    // Work was cancelled
                    managerScope.launch {
                        importTaskDao.updateStatus(importId, ImportStatus.CANCELLED)
                    }
                }
                else -> {
                    // Other states (BLOCKED, etc.)
                }
            }
        }
    }
}

/**
 * Statistics about import operations
 */
data class ImportStats(
    val pending: Int,
    val inProgress: Int,
    val completed: Int,
    val failed: Int,
    val cancelled: Int,
    val total: Int
) {
    val activeCount: Int get() = pending + inProgress
    val completedSuccessfully: Int get() = completed
    val completedWithErrors: Int get() = failed + cancelled
}