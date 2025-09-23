package com.bsikar.helix.data.source.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.bsikar.helix.data.model.ParsedEpub
import com.bsikar.helix.data.model.EpubMetadata
import com.bsikar.helix.data.model.EpubTocEntry
import com.bsikar.helix.data.model.EpubChapter

@Entity(
    tableName = "cached_epub_metadata",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"], unique = true),
        Index(value = ["fileChecksum"]),
        Index(value = ["lastModified"]),
        Index(value = ["cacheTimestamp"])
    ]
)
data class CachedEpubMetadataEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    
    // File identification for cache invalidation
    val filePath: String,
    val fileChecksum: String,
    val fileSize: Long,
    val lastModified: Long,
    val cacheTimestamp: Long = System.currentTimeMillis(),
    
    // Metadata fields
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val language: String? = null,
    val isbn: String? = null,
    val publishedDate: String? = null,
    val rights: String? = null,
    val subjects: String = "", // JSON string of subjects
    
    // EPUB structure info
    val totalChapters: Int,
    val coverImagePath: String? = null,
    val opfPath: String = "OEBPS/content.opf",
    
    // Table of contents as JSON
    val tableOfContentsJson: String = "[]",
    
    // Images map as JSON
    val imagesJson: String = "{}",
    
    // Parsing performance metrics
    val parsingTimeMs: Long = 0,
    val contentPreloadedCount: Int = 0,
    
    // Cache validity
    val isValid: Boolean = true,
    val validationErrors: String? = null
)

/**
 * Convert CachedEpubMetadataEntity to ParsedEpub
 */
fun CachedEpubMetadataEntity.toParsedEpub(chapters: List<com.bsikar.helix.data.model.EpubChapter> = emptyList()): com.bsikar.helix.data.model.ParsedEpub {
    val metadata = com.bsikar.helix.data.model.EpubMetadata(
        title = title,
        author = author,
        description = description,
        publisher = publisher,
        language = language,
        isbn = isbn,
        publishedDate = publishedDate,
        rights = rights,
        subjects = if (subjects.isNotEmpty()) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(subjects)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    )
    
    val toc = if (tableOfContentsJson.isNotEmpty()) {
        try {
            kotlinx.serialization.json.Json.decodeFromString<List<com.bsikar.helix.data.model.EpubTocEntry>>(tableOfContentsJson)
        } catch (e: Exception) {
            emptyList()
        }
    } else emptyList()
    
    val images = if (imagesJson.isNotEmpty()) {
        try {
            kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(imagesJson)
        } catch (e: Exception) {
            emptyMap()
        }
    } else emptyMap()
    
    return com.bsikar.helix.data.model.ParsedEpub(
        metadata = metadata,
        chapters = chapters,
        tableOfContents = toc,
        coverImagePath = coverImagePath,
        filePath = filePath,
        fileSize = fileSize,
        lastModified = lastModified,
        images = images,
        opfPath = opfPath,
        totalChapters = totalChapters
    )
}

/**
 * Convert ParsedEpub to CachedEpubMetadataEntity
 */
fun com.bsikar.helix.data.model.ParsedEpub.toCachedEntity(
    bookId: String,
    fileChecksum: String,
    parsingTimeMs: Long = 0
): CachedEpubMetadataEntity {
    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    
    return CachedEpubMetadataEntity(
        id = java.util.UUID.randomUUID().toString(),
        bookId = bookId,
        filePath = filePath ?: "",
        fileChecksum = fileChecksum,
        fileSize = fileSize,
        lastModified = lastModified,
        cacheTimestamp = System.currentTimeMillis(),
        title = metadata.title,
        author = metadata.author,
        description = metadata.description,
        publisher = metadata.publisher,
        language = metadata.language,
        isbn = metadata.isbn,
        publishedDate = metadata.publishedDate,
        rights = metadata.rights,
        subjects = try {
            json.encodeToString(serializer<List<String>>(), metadata.subjects)
        } catch (e: Exception) {
            "[]"
        },
        totalChapters = chapterCount,
        coverImagePath = coverImagePath,
        opfPath = opfPath,
        tableOfContentsJson = try {
            json.encodeToString(serializer<List<EpubTocEntry>>(), tableOfContents)
        } catch (e: Exception) {
            "[]"
        },
        imagesJson = try {
            json.encodeToString(serializer<Map<String, String>>(), images)
        } catch (e: Exception) {
            "{}"
        },
        parsingTimeMs = parsingTimeMs,
        contentPreloadedCount = chapters.count { it.content.isNotEmpty() },
        isValid = true,
        validationErrors = null
    )
}