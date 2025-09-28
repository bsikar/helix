package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.source.entities.AudioChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChapterDao {
    
    @Query("SELECT * FROM audio_chapters WHERE bookId = :bookId ORDER BY chapter_order ASC")
    fun getChaptersByBookId(bookId: String): Flow<List<AudioChapterEntity>>
    
    @Query("SELECT * FROM audio_chapters WHERE bookId = :bookId ORDER BY chapter_order ASC")
    suspend fun getChaptersByBookIdSync(bookId: String): List<AudioChapterEntity>
    
    @Query("SELECT * FROM audio_chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: String): AudioChapterEntity?
    
    @Query("SELECT * FROM audio_chapters WHERE bookId = :bookId AND startTimeMs <= :positionMs AND (startTimeMs + durationMs) > :positionMs LIMIT 1")
    suspend fun getChapterAtPosition(bookId: String, positionMs: Long): AudioChapterEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: AudioChapterEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<AudioChapterEntity>)
    
    @Update
    suspend fun updateChapter(chapter: AudioChapterEntity)
    
    @Delete
    suspend fun deleteChapter(chapter: AudioChapterEntity)
    
    @Query("DELETE FROM audio_chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: String)
    
    @Query("SELECT COUNT(*) FROM audio_chapters WHERE bookId = :bookId")
    suspend fun getChapterCountForBook(bookId: String): Int
}