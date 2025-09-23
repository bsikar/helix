package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.source.entities.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `order` ASC")
    suspend fun getChaptersForBook(bookId: String): List<ChapterEntity>
    
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `order` ASC")
    fun getChaptersForBookFlow(bookId: String): Flow<List<ChapterEntity>>
    
    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: String): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND href = :href LIMIT 1")
    suspend fun getChapterByHref(bookId: String, href: String): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND `order` = :order LIMIT 1")
    suspend fun getChapterByOrder(bookId: String, order: Int): ChapterEntity?
    
    // Navigation queries
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND `order` < :currentOrder ORDER BY `order` DESC LIMIT 1")
    suspend fun getPreviousChapter(bookId: String, currentOrder: Int): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND `order` > :currentOrder ORDER BY `order` ASC LIMIT 1")
    suspend fun getNextChapter(bookId: String, currentOrder: Int): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `order` ASC LIMIT 1")
    suspend fun getFirstChapter(bookId: String): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `order` DESC LIMIT 1")
    suspend fun getLastChapter(bookId: String): ChapterEntity?
    
    // Hierarchical queries
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND level = 0 ORDER BY `order` ASC")
    suspend fun getRootChapters(bookId: String): List<ChapterEntity>
    
    @Query("SELECT * FROM chapters WHERE parentChapterId = :parentChapterId ORDER BY `order` ASC")
    suspend fun getChildChapters(parentChapterId: String): List<ChapterEntity>
    
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND level <= :maxLevel ORDER BY `order` ASC")
    suspend fun getChaptersUpToLevel(bookId: String, maxLevel: Int): List<ChapterEntity>
    
    // Statistics and analysis
    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId")
    suspend fun getChapterCount(bookId: String): Int
    
    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId AND hasContent = 1")
    suspend fun getContentChapterCount(bookId: String): Int
    
    @Query("SELECT SUM(wordCount) FROM chapters WHERE bookId = :bookId")
    suspend fun getTotalWordCount(bookId: String): Int
    
    @Query("SELECT SUM(estimatedReadingMinutes) FROM chapters WHERE bookId = :bookId")
    suspend fun getTotalEstimatedReadingTime(bookId: String): Int
    
    @Query("SELECT AVG(wordCount) FROM chapters WHERE bookId = :bookId AND hasContent = 1")
    suspend fun getAverageChapterWordCount(bookId: String): Float
    
    // Search functionality
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND title LIKE '%' || :query || '%' ORDER BY `order` ASC")
    suspend fun searchChaptersByTitle(bookId: String, query: String): List<ChapterEntity>
    
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND (title LIKE '%' || :query || '%' OR href LIKE '%' || :query || '%') ORDER BY `order` ASC")
    suspend fun searchChapters(bookId: String, query: String): List<ChapterEntity>
    
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)
    
    // Update operations
    @Update
    suspend fun updateChapter(chapter: ChapterEntity)
    
    @Query("UPDATE chapters SET wordCount = :wordCount, estimatedReadingMinutes = :readingTime WHERE id = :chapterId")
    suspend fun updateChapterStats(chapterId: String, wordCount: Int, readingTime: Int)
    
    @Query("UPDATE chapters SET hasContent = :hasContent WHERE id = :chapterId")
    suspend fun updateChapterContentStatus(chapterId: String, hasContent: Boolean)
    
    // Delete operations
    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)
    
    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun deleteChapterById(chapterId: String)
    
    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: String)
    
    @Query("DELETE FROM chapters")
    suspend fun deleteAllChapters()
    
    // Batch operations
    @Transaction
    suspend fun replaceChaptersForBook(bookId: String, chapters: List<ChapterEntity>) {
        deleteChaptersForBook(bookId)
        insertChapters(chapters)
    }
    
    @Transaction
    suspend fun updateChapterHierarchy(bookId: String, chapters: List<ChapterEntity>) {
        // Update existing chapters while preserving statistics
        chapters.forEach { newChapter ->
            val existing = getChapterById(newChapter.id)
            if (existing != null) {
                // Preserve word count and reading time if they exist
                val updatedChapter = newChapter.copy(
                    wordCount = if (existing.wordCount > 0) existing.wordCount else newChapter.wordCount,
                    estimatedReadingMinutes = if (existing.estimatedReadingMinutes > 0) existing.estimatedReadingMinutes else newChapter.estimatedReadingMinutes
                )
                updateChapter(updatedChapter)
            } else {
                insertChapter(newChapter)
            }
        }
        
        // Remove chapters that no longer exist
        val existingChapters = getChaptersForBook(bookId)
        val newChapterIds = chapters.map { it.id }.toSet()
        existingChapters.forEach { existing ->
            if (existing.id !in newChapterIds) {
                deleteChapter(existing)
            }
        }
    }
    
    // Progress tracking
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND `order` <= :currentOrder ORDER BY `order` ASC")
    suspend fun getCompletedChapters(bookId: String, currentOrder: Int): List<ChapterEntity>
    
    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId AND `order` <= :currentOrder")
    suspend fun getCompletedChapterCount(bookId: String, currentOrder: Int): Int
    
    // Reading progress calculation
    @Query("SELECT CAST(COUNT(*) AS FLOAT) / (SELECT COUNT(*) FROM chapters WHERE bookId = :bookId) FROM chapters WHERE bookId = :bookId AND `order` <= :currentOrder")
    suspend fun getProgressPercentage(bookId: String, currentOrder: Int): Float
}