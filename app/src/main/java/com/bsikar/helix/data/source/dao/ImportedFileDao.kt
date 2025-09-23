package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.source.entities.ImportedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportedFileDao {
    
    @Query("SELECT * FROM imported_files ORDER BY importedAt DESC")
    suspend fun getAllImportedFiles(): List<ImportedFileEntity>
    
    @Query("SELECT * FROM imported_files ORDER BY importedAt DESC")
    fun getAllImportedFilesFlow(): Flow<List<ImportedFileEntity>>
    
    @Query("SELECT * FROM imported_files WHERE path = :path")
    suspend fun getImportedFileByPath(path: String): ImportedFileEntity?
    
    @Query("SELECT * FROM imported_files WHERE bookId = :bookId")
    suspend fun getImportedFileByBookId(bookId: String): ImportedFileEntity?
    
    @Query("SELECT * FROM imported_files WHERE sourceType = :sourceType")
    suspend fun getImportedFilesBySourceType(sourceType: String): List<ImportedFileEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedFile(file: ImportedFileEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedFiles(files: List<ImportedFileEntity>)
    
    @Update
    suspend fun updateImportedFile(file: ImportedFileEntity)
    
    @Delete
    suspend fun deleteImportedFile(file: ImportedFileEntity)
    
    @Query("DELETE FROM imported_files WHERE path = :path")
    suspend fun deleteImportedFileByPath(path: String)
    
    @Query("DELETE FROM imported_files WHERE bookId = :bookId")
    suspend fun deleteImportedFileByBookId(bookId: String)
    
    @Query("DELETE FROM imported_files")
    suspend fun deleteAllImportedFiles()
}