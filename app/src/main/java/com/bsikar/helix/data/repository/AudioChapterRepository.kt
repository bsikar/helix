package com.bsikar.helix.data.repository

import com.bsikar.helix.data.model.AudioChapter
import com.bsikar.helix.data.source.dao.AudioChapterDao
import com.bsikar.helix.data.source.entities.AudioChapterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioChapterRepository @Inject constructor(
    private val audioChapterDao: AudioChapterDao
) {
    
    fun getChaptersByBookId(bookId: String): Flow<List<AudioChapter>> {
        return audioChapterDao.getChaptersByBookId(bookId).map { entities ->
            entities.map { it.toAudioChapter() }
        }
    }
    
    suspend fun getChaptersByBookIdSync(bookId: String): List<AudioChapter> {
        return audioChapterDao.getChaptersByBookIdSync(bookId).map { it.toAudioChapter() }
    }
    
    suspend fun getChapterById(chapterId: String): AudioChapter? {
        return audioChapterDao.getChapterById(chapterId)?.toAudioChapter()
    }
    
    suspend fun getChapterAtPosition(bookId: String, positionMs: Long): AudioChapter? {
        return audioChapterDao.getChapterAtPosition(bookId, positionMs)?.toAudioChapter()
    }
    
    suspend fun insertChapter(chapter: AudioChapter) {
        audioChapterDao.insertChapter(chapter.toEntity())
    }
    
    suspend fun insertChapters(chapters: List<AudioChapter>) {
        audioChapterDao.insertChapters(chapters.map { it.toEntity() })
    }
    
    suspend fun updateChapter(chapter: AudioChapter) {
        audioChapterDao.updateChapter(chapter.toEntity())
    }
    
    suspend fun deleteChapter(chapter: AudioChapter) {
        audioChapterDao.deleteChapter(chapter.toEntity())
    }
    
    suspend fun deleteChaptersByBookId(bookId: String) {
        audioChapterDao.deleteChaptersByBookId(bookId)
    }
    
    suspend fun getChapterCountForBook(bookId: String): Int {
        return audioChapterDao.getChapterCountForBook(bookId)
    }
}

// Extension functions for converting between data classes and entities
fun AudioChapter.toEntity(): AudioChapterEntity {
    return AudioChapterEntity(
        id = this.id,
        title = this.title,
        startTimeMs = this.startTimeMs,
        durationMs = this.durationMs,
        order = this.order,
        bookId = this.bookId
    )
}

fun AudioChapterEntity.toAudioChapter(): AudioChapter {
    return AudioChapter(
        id = this.id,
        title = this.title,
        startTimeMs = this.startTimeMs,
        durationMs = this.durationMs,
        order = this.order,
        bookId = this.bookId
    )
}