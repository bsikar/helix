package com.bsikar.helix.data

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class EpubMetadata(
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val language: String? = null,
    val isbn: String? = null,
    val publishedDate: String? = null,
    val rights: String? = null,
    val subjects: List<String> = emptyList()
)

@Serializable
data class EpubChapter(
    val id: String,
    val title: String,
    val href: String,
    val content: String = "", // HTML content
    val order: Int
)

@Serializable 
data class EpubTocEntry(
    val title: String,
    val href: String,
    val children: List<EpubTocEntry> = emptyList()
)

data class ParsedEpub(
    val metadata: EpubMetadata,
    val chapters: List<EpubChapter>,
    val tableOfContents: List<EpubTocEntry>,
    val coverImagePath: String? = null,
    val filePath: String?,
    val fileSize: Long,
    val lastModified: Long,
    val images: Map<String, String> = emptyMap(), // Map of image href to zip paths
    val opfPath: String = "OEBPS/content.opf", // OPF file path for lazy loading
    val totalChapters: Int? = null // Direct chapter count for fast import
) {
    val chapterCount: Int get() = totalChapters ?: chapters.size
    
    fun getChapterByOrder(order: Int): EpubChapter? {
        return chapters.find { it.order == order }
    }
    
    fun getChapterById(id: String): EpubChapter? {
        return chapters.find { it.id == id }
    }
}