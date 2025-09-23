package com.bsikar.helix.data.repository

import android.util.Log
import com.bsikar.helix.data.model.EpubChapter
import com.bsikar.helix.data.model.EpubTocEntry
import com.bsikar.helix.data.model.ParsedEpub
import com.bsikar.helix.data.source.dao.ChapterDao
import com.bsikar.helix.data.source.entities.ChapterEntity
import com.bsikar.helix.data.source.entities.toChapterEntities
import com.bsikar.helix.data.source.entities.toEntity
import com.bsikar.helix.data.source.entities.toEpubChapter
import com.bsikar.helix.data.source.entities.toTocEntries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao
) {
    private val TAG = "ChapterRepository"
    
    /**
     * Get all chapters for a book
     */
    suspend fun getChaptersForBook(bookId: String): List<EpubChapter> {
        return try {
            val entities = chapterDao.getChaptersForBook(bookId)
            entities.map { it.toEpubChapter() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get chapters for book $bookId: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get chapters as Flow for reactive updates
     */
    fun getChaptersForBookFlow(bookId: String): Flow<List<EpubChapter>> {
        return chapterDao.getChaptersForBookFlow(bookId).map { entities ->
            entities.map { it.toEpubChapter() }
        }
    }
    
    /**
     * Get table of contents structure for a book
     */
    suspend fun getTableOfContents(bookId: String): List<EpubTocEntry> {
        return try {
            val entities = chapterDao.getChaptersForBook(bookId)
            entities.toTocEntries()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get table of contents for book $bookId: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get a specific chapter by ID
     */
    suspend fun getChapterById(chapterId: String): EpubChapter? {
        return try {
            chapterDao.getChapterById(chapterId)?.toEpubChapter()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get chapter $chapterId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get chapter by href (URL reference)
     */
    suspend fun getChapterByHref(bookId: String, href: String): EpubChapter? {
        return try {
            chapterDao.getChapterByHref(bookId, href)?.toEpubChapter()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get chapter by href $href for book $bookId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get chapter by order/position
     */
    suspend fun getChapterByOrder(bookId: String, order: Int): EpubChapter? {
        return try {
            chapterDao.getChapterByOrder(bookId, order)?.toEpubChapter()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get chapter at order $order for book $bookId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Navigation methods
     */
    suspend fun getPreviousChapter(bookId: String, currentOrder: Int): EpubChapter? {
        return try {
            chapterDao.getPreviousChapter(bookId, currentOrder)?.toEpubChapter()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get previous chapter for book $bookId: ${e.message}", e)
            null
        }
    }
    
    suspend fun getNextChapter(bookId: String, currentOrder: Int): EpubChapter? {
        return try {
            chapterDao.getNextChapter(bookId, currentOrder)?.toEpubChapter()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get next chapter for book $bookId: ${e.message}", e)
            null
        }
    }
    
    suspend fun getFirstChapter(bookId: String): EpubChapter? {
        return try {
            chapterDao.getFirstChapter(bookId)?.toEpubChapter()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get first chapter for book $bookId: ${e.message}", e)
            null
        }
    }
    
    suspend fun getLastChapter(bookId: String): EpubChapter? {
        return try {
            chapterDao.getLastChapter(bookId)?.toEpubChapter()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last chapter for book $bookId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Statistics and analysis
     */
    suspend fun getChapterCount(bookId: String): Int {
        return try {
            chapterDao.getChapterCount(bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get chapter count for book $bookId: ${e.message}", e)
            0
        }
    }
    
    suspend fun getTotalWordCount(bookId: String): Int {
        return try {
            chapterDao.getTotalWordCount(bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get total word count for book $bookId: ${e.message}", e)
            0
        }
    }
    
    suspend fun getTotalEstimatedReadingTime(bookId: String): Int {
        return try {
            chapterDao.getTotalEstimatedReadingTime(bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get estimated reading time for book $bookId: ${e.message}", e)
            0
        }
    }
    
    suspend fun getProgressPercentage(bookId: String, currentOrder: Int): Float {
        return try {
            chapterDao.getProgressPercentage(bookId, currentOrder)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get progress percentage for book $bookId: ${e.message}", e)
            0f
        }
    }
    
    /**
     * Search functionality
     */
    suspend fun searchChapters(bookId: String, query: String): List<EpubChapter> {
        return try {
            val entities = chapterDao.searchChapters(bookId, query)
            entities.map { it.toEpubChapter() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search chapters for book $bookId: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Store chapters from parsed EPUB
     */
    suspend fun storeChaptersFromEpub(bookId: String, parsedEpub: ParsedEpub): Boolean {
        return try {
            Log.d(TAG, "Storing chapters for book $bookId")
            
            // Convert TOC entries to chapter entities (primary source of truth)
            val chaptersFromToc = parsedEpub.tableOfContents.toChapterEntities(bookId)
            
            // Merge with chapter content if available
            val chaptersWithContent = chaptersFromToc.map { tocChapter ->
                val correspondingEpubChapter = parsedEpub.chapters.find { 
                    it.href == tocChapter.href || it.title == tocChapter.title 
                }
                
                if (correspondingEpubChapter != null) {
                    tocChapter.copy(
                        hasContent = correspondingEpubChapter.content.isNotEmpty(),
                        wordCount = correspondingEpubChapter.content.split("\\s+".toRegex()).size,
                        estimatedReadingMinutes = maxOf(1, correspondingEpubChapter.content.split("\\s+".toRegex()).size / 250)
                    )
                } else {
                    tocChapter
                }
            }
            
            // If no TOC entries, fall back to chapters directly
            val finalChapters = if (chaptersWithContent.isNotEmpty()) {
                chaptersWithContent
            } else {
                parsedEpub.chapters.map { it.toEntity(bookId) }
            }
            
            // Replace all chapters for this book
            chapterDao.replaceChaptersForBook(bookId, finalChapters)
            
            Log.d(TAG, "Successfully stored ${finalChapters.size} chapters for book $bookId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store chapters for book $bookId: ${e.message}", e)
            false
        }
    }
    
    /**
     * Update chapter statistics (word count, reading time, etc.)
     */
    suspend fun updateChapterStats(chapterId: String, wordCount: Int, readingTime: Int): Boolean {
        return try {
            chapterDao.updateChapterStats(chapterId, wordCount, readingTime)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update chapter stats for $chapterId: ${e.message}", e)
            false
        }
    }
    
    /**
     * Delete all chapters for a book
     */
    suspend fun deleteChaptersForBook(bookId: String): Boolean {
        return try {
            chapterDao.deleteChaptersForBook(bookId)
            Log.d(TAG, "Deleted all chapters for book $bookId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete chapters for book $bookId: ${e.message}", e)
            false
        }
    }
}