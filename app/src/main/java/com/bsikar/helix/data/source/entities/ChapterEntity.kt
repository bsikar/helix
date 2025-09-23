package com.bsikar.helix.data.source.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.bsikar.helix.data.model.EpubChapter
import com.bsikar.helix.data.model.EpubTocEntry

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "order"]),
        Index(value = ["bookId", "href"])
    ]
)
data class ChapterEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val title: String,
    val href: String,
    val order: Int,
    val parentChapterId: String? = null, // For nested chapters
    val level: Int = 0, // Nesting level (0 = top level, 1 = first sub-level, etc.)
    val hasContent: Boolean = true, // Whether this chapter has actual content or is just a section header
    val wordCount: Int = 0, // Estimated word count for reading time calculations
    val estimatedReadingMinutes: Int = 0 // Estimated reading time in minutes
)

// Mapper functions to convert between domain models and entities
fun EpubChapter.toEntity(bookId: String, level: Int = 0, parentChapterId: String? = null): ChapterEntity {
    return ChapterEntity(
        id = "${bookId}_${this.id}",
        bookId = bookId,
        title = this.title,
        href = this.href,
        order = this.order,
        parentChapterId = parentChapterId,
        level = level,
        hasContent = this.content.isNotEmpty(),
        wordCount = this.content.split("\\s+".toRegex()).size,
        estimatedReadingMinutes = maxOf(1, this.content.split("\\s+".toRegex()).size / 250) // Average 250 words per minute
    )
}

fun EpubTocEntry.toEntity(bookId: String, order: Int, level: Int = 0, parentChapterId: String? = null): ChapterEntity {
    return ChapterEntity(
        id = "${bookId}_toc_${this.href.hashCode()}",
        bookId = bookId,
        title = this.title,
        href = this.href,
        order = order,
        parentChapterId = parentChapterId,
        level = level,
        hasContent = true, // Assume TOC entries have content unless proven otherwise
        wordCount = 0, // Will be updated when content is parsed
        estimatedReadingMinutes = 0 // Will be calculated based on word count
    )
}

fun ChapterEntity.toEpubChapter(): EpubChapter {
    return EpubChapter(
        id = this.id.substringAfterLast("_"),
        title = this.title,
        href = this.href,
        content = "", // Content will be loaded separately
        order = this.order
    )
}

// Helper function to convert TOC entries to a flat list of chapters with proper nesting
fun List<EpubTocEntry>.toChapterEntities(bookId: String): List<ChapterEntity> {
    val chapters = mutableListOf<ChapterEntity>()
    var order = 0
    
    fun processEntry(entry: EpubTocEntry, level: Int = 0, parentId: String? = null) {
        val chapterEntity = entry.toEntity(bookId, order++, level, parentId)
        chapters.add(chapterEntity)
        
        // Process children recursively
        entry.children.forEach { child ->
            processEntry(child, level + 1, chapterEntity.id)
        }
    }
    
    this.forEach { entry ->
        processEntry(entry)
    }
    
    return chapters
}

// Helper function to rebuild TOC structure from flat chapter list
fun List<ChapterEntity>.toTocEntries(): List<EpubTocEntry> {
    val chapterMap = this.associateBy { it.id }
    val rootChapters = this.filter { it.level == 0 }.sortedBy { it.order }
    
    fun buildTocEntry(chapter: ChapterEntity): EpubTocEntry {
        val children = this.filter { it.parentChapterId == chapter.id }
            .sortedBy { it.order }
            .map { buildTocEntry(it) }
        
        return EpubTocEntry(
            title = chapter.title,
            href = chapter.href,
            children = children
        )
    }
    
    return rootChapters.map { buildTocEntry(it) }
}