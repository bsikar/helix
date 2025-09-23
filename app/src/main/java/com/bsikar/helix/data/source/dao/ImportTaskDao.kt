package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.model.ImportTask
import com.bsikar.helix.data.model.ImportStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing import task data
 */
@Dao
interface ImportTaskDao {

    /**
     * Insert a new import task
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportTask(task: ImportTask)

    /**
     * Update an existing import task
     */
    @Update
    suspend fun updateImportTask(task: ImportTask)

    /**
     * Delete an import task
     */
    @Delete
    suspend fun deleteImportTask(task: ImportTask)

    /**
     * Delete import task by ID
     */
    @Query("DELETE FROM import_tasks WHERE id = :taskId")
    suspend fun deleteImportTaskById(taskId: String)

    /**
     * Get import task by ID
     */
    @Query("SELECT * FROM import_tasks WHERE id = :taskId")
    suspend fun getImportTaskById(taskId: String): ImportTask?

    /**
     * Get all import tasks
     */
    @Query("SELECT * FROM import_tasks ORDER BY startTime DESC")
    fun getAllImportTasks(): Flow<List<ImportTask>>

    /**
     * Get active import tasks (in progress or pending)
     */
    @Query("SELECT * FROM import_tasks WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY startTime DESC")
    fun getActiveImportTasks(): Flow<List<ImportTask>>

    /**
     * Get completed import tasks
     */
    @Query("SELECT * FROM import_tasks WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED') ORDER BY startTime DESC")
    fun getCompletedImportTasks(): Flow<List<ImportTask>>

    /**
     * Update import task progress
     */
    @Query("UPDATE import_tasks SET progress = :progress, progressMessage = :message WHERE id = :taskId")
    suspend fun updateProgress(taskId: String, progress: Int, message: String)

    /**
     * Update import task status
     */
    @Query("UPDATE import_tasks SET status = :status, endTime = :endTime WHERE id = :taskId")
    suspend fun updateStatus(taskId: String, status: ImportStatus, endTime: Long? = System.currentTimeMillis())

    /**
     * Update import task completion
     */
    @Query("UPDATE import_tasks SET status = :status, endTime = :endTime, bookId = :bookId WHERE id = :taskId")
    suspend fun updateCompletion(taskId: String, status: ImportStatus, endTime: Long, bookId: String?)

    /**
     * Update import task error
     */
    @Query("UPDATE import_tasks SET status = 'FAILED', endTime = :endTime, errorMessage = :errorMessage WHERE id = :taskId")
    suspend fun updateError(taskId: String, endTime: Long, errorMessage: String)

    /**
     * Get import tasks by status
     */
    @Query("SELECT * FROM import_tasks WHERE status = :status ORDER BY startTime DESC")
    fun getImportTasksByStatus(status: ImportStatus): Flow<List<ImportTask>>

    /**
     * Clear old completed tasks (older than specified time)
     */
    @Query("DELETE FROM import_tasks WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND endTime < :cutoffTime")
    suspend fun clearOldTasks(cutoffTime: Long)

    /**
     * Get import task count by status
     */
    @Query("SELECT COUNT(*) FROM import_tasks WHERE status = :status")
    suspend fun getTaskCountByStatus(status: ImportStatus): Int

    /**
     * Check if file is already being imported
     */
    @Query("SELECT COUNT(*) FROM import_tasks WHERE fileUri = :fileUri AND status IN ('PENDING', 'IN_PROGRESS')")
    suspend fun isFileBeingImported(fileUri: String): Int

    /**
     * Get recent import tasks (last 24 hours)
     */
    @Query("SELECT * FROM import_tasks WHERE startTime > :cutoffTime ORDER BY startTime DESC")
    fun getRecentImportTasks(cutoffTime: Long = System.currentTimeMillis() - 24 * 60 * 60 * 1000): Flow<List<ImportTask>>

    /**
     * Get failed import tasks that can be retried
     */
    @Query("SELECT * FROM import_tasks WHERE status = 'FAILED' ORDER BY startTime DESC")
    fun getFailedImportTasks(): Flow<List<ImportTask>>
}