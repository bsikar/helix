package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.source.entities.WatchedDirectoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedDirectoryDao {
    
    @Query("SELECT * FROM watched_directories")
    suspend fun getAllWatchedDirectories(): List<WatchedDirectoryEntity>
    
    @Query("SELECT * FROM watched_directories")
    fun getAllWatchedDirectoriesFlow(): Flow<List<WatchedDirectoryEntity>>
    
    @Query("SELECT * FROM watched_directories WHERE path = :path")
    suspend fun getWatchedDirectoryByPath(path: String): WatchedDirectoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedDirectory(directory: WatchedDirectoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedDirectories(directories: List<WatchedDirectoryEntity>)
    
    @Update
    suspend fun updateWatchedDirectory(directory: WatchedDirectoryEntity)
    
    @Delete
    suspend fun deleteWatchedDirectory(directory: WatchedDirectoryEntity)
    
    @Query("DELETE FROM watched_directories WHERE path = :path")
    suspend fun deleteWatchedDirectoryByPath(path: String)
    
    @Query("DELETE FROM watched_directories")
    suspend fun deleteAllWatchedDirectories()
}