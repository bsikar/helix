package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.source.entities.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTagsFlow(): Flow<List<TagEntity>>
    
    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTags(): List<TagEntity>
    
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: String): TagEntity?
    
    @Query("SELECT * FROM tags WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomTagsFlow(): Flow<List<TagEntity>>
    
    @Query("SELECT * FROM tags WHERE isCustom = 0 ORDER BY name ASC")
    fun getPresetTagsFlow(): Flow<List<TagEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)
    
    @Update
    suspend fun updateTag(tag: TagEntity)
    
    @Delete
    suspend fun deleteTag(tag: TagEntity)
    
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: String)
    
    @Query("DELETE FROM tags WHERE isCustom = 1")
    suspend fun deleteAllCustomTags()
}