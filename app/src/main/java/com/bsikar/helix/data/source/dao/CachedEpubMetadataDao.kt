package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.source.entities.CachedEpubMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedEpubMetadataDao {
    
    @Query("SELECT * FROM cached_epub_metadata WHERE bookId = :bookId")
    suspend fun getCachedMetadataForBook(bookId: String): CachedEpubMetadataEntity?
    
    @Query("SELECT * FROM cached_epub_metadata WHERE bookId = :bookId")
    fun getCachedMetadataForBookFlow(bookId: String): Flow<CachedEpubMetadataEntity?>
    
    @Query("SELECT * FROM cached_epub_metadata WHERE fileChecksum = :checksum")
    suspend fun getCachedMetadataByChecksum(checksum: String): CachedEpubMetadataEntity?
    
    @Query("SELECT * FROM cached_epub_metadata WHERE filePath = :filePath")
    suspend fun getCachedMetadataByPath(filePath: String): CachedEpubMetadataEntity?
    
    @Query("SELECT * FROM cached_epub_metadata ORDER BY cacheTimestamp DESC")
    suspend fun getAllCachedMetadata(): List<CachedEpubMetadataEntity>
    
    @Query("SELECT * FROM cached_epub_metadata ORDER BY cacheTimestamp DESC")
    fun getAllCachedMetadataFlow(): Flow<List<CachedEpubMetadataEntity>>
    
    @Query("SELECT * FROM cached_epub_metadata WHERE isValid = 1 ORDER BY cacheTimestamp DESC")
    suspend fun getValidCachedMetadata(): List<CachedEpubMetadataEntity>
    
    @Query("SELECT * FROM cached_epub_metadata WHERE isValid = 0")
    suspend fun getInvalidCachedMetadata(): List<CachedEpubMetadataEntity>
    
    @Query("SELECT COUNT(*) FROM cached_epub_metadata WHERE isValid = 1")
    suspend fun getValidCacheCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM cached_epub_metadata WHERE isValid = 1")
    suspend fun getTotalCachedFileSize(): Long?
    
    @Query("SELECT AVG(parsingTimeMs) FROM cached_epub_metadata WHERE parsingTimeMs > 0")
    suspend fun getAverageParsingTime(): Float?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMetadata(metadata: CachedEpubMetadataEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMetadataList(metadataList: List<CachedEpubMetadataEntity>)
    
    @Update
    suspend fun updateCachedMetadata(metadata: CachedEpubMetadataEntity)
    
    @Delete
    suspend fun deleteCachedMetadata(metadata: CachedEpubMetadataEntity)
    
    @Query("DELETE FROM cached_epub_metadata WHERE bookId = :bookId")
    suspend fun deleteCachedMetadataForBook(bookId: String)
    
    @Query("DELETE FROM cached_epub_metadata WHERE id = :id")
    suspend fun deleteCachedMetadataById(id: String)
    
    @Query("DELETE FROM cached_epub_metadata WHERE isValid = 0")
    suspend fun deleteInvalidCachedMetadata()
    
    @Query("DELETE FROM cached_epub_metadata")
    suspend fun deleteAllCachedMetadata()
    
    @Query("DELETE FROM cached_epub_metadata WHERE cacheTimestamp < :cutoffTime")
    suspend fun deleteOldCachedMetadata(cutoffTime: Long)
    
    /**
     * Check if cached metadata is still valid by comparing file checksum and modification time
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM cached_epub_metadata 
        WHERE bookId = :bookId 
        AND fileChecksum = :checksum 
        AND lastModified = :lastModified 
        AND isValid = 1
    """)
    suspend fun isCacheValid(bookId: String, checksum: String, lastModified: Long): Boolean
    
    /**
     * Mark cache as invalid for a specific book
     */
    @Query("UPDATE cached_epub_metadata SET isValid = 0, validationErrors = :reason WHERE bookId = :bookId")
    suspend fun invalidateCacheForBook(bookId: String, reason: String?)
    
    /**
     * Mark cache as invalid for files with specific checksum
     */
    @Query("UPDATE cached_epub_metadata SET isValid = 0, validationErrors = :reason WHERE fileChecksum = :checksum")
    suspend fun invalidateCacheByChecksum(checksum: String, reason: String?)
    
    /**
     * Update cache validity status
     */
    @Query("UPDATE cached_epub_metadata SET isValid = :isValid, validationErrors = :errors WHERE bookId = :bookId")
    suspend fun updateCacheValidityForBook(bookId: String, isValid: Boolean, errors: String?)
    
    /**
     * Get cache statistics for analytics
     */
    @Query("""
        SELECT 
            COUNT(*) as totalEntries,
            COUNT(CASE WHEN isValid = 1 THEN 1 END) as validEntries,
            COUNT(CASE WHEN isValid = 0 THEN 1 END) as invalidEntries,
            SUM(fileSize) as totalFileSize,
            AVG(parsingTimeMs) as avgParsingTime,
            MIN(cacheTimestamp) as oldestCache,
            MAX(cacheTimestamp) as newestCache
        FROM cached_epub_metadata
    """)
    suspend fun getCacheStatistics(): CacheStatistics?
    
    /**
     * Find cached metadata that may need refresh based on age
     */
    @Query("""
        SELECT * FROM cached_epub_metadata 
        WHERE cacheTimestamp < :maxAge 
        AND isValid = 1 
        ORDER BY cacheTimestamp ASC
        LIMIT :limit
    """)
    suspend fun getCachedMetadataOlderThan(maxAge: Long, limit: Int = 50): List<CachedEpubMetadataEntity>
    
    /**
     * Get cached metadata with parsing performance worse than threshold
     */
    @Query("""
        SELECT * FROM cached_epub_metadata 
        WHERE parsingTimeMs > :thresholdMs 
        AND isValid = 1 
        ORDER BY parsingTimeMs DESC
        LIMIT :limit
    """)
    suspend fun getSlowParsingCaches(thresholdMs: Long, limit: Int = 10): List<CachedEpubMetadataEntity>
    
    /**
     * Transaction method to upsert cached metadata
     */
    @Transaction
    suspend fun upsertCachedMetadata(metadata: CachedEpubMetadataEntity) {
        val existing = getCachedMetadataForBook(metadata.bookId)
        if (existing != null) {
            // Update existing cache
            updateCachedMetadata(metadata.copy(id = existing.id))
        } else {
            // Insert new cache
            insertCachedMetadata(metadata)
        }
    }
    
    /**
     * Transaction method to refresh cache for a book
     */
    @Transaction
    suspend fun refreshCacheForBook(bookId: String, newMetadata: CachedEpubMetadataEntity) {
        // Delete old cache
        deleteCachedMetadataForBook(bookId)
        // Insert new cache
        insertCachedMetadata(newMetadata.copy(bookId = bookId))
    }
}

/**
 * Data class for cache statistics
 */
data class CacheStatistics(
    val totalEntries: Int,
    val validEntries: Int,
    val invalidEntries: Int,
    val totalFileSize: Long,
    val avgParsingTime: Float,
    val oldestCache: Long,
    val newestCache: Long
) {
    val hitRate: Float get() = if (totalEntries > 0) validEntries.toFloat() / totalEntries else 0f
    val totalFileSizeMB: Float get() = totalFileSize / (1024f * 1024f)
}