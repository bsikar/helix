package com.bsikar.helix.data.mapper

import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.model.CoverDisplayMode
import com.bsikar.helix.data.source.entities.BookEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Extension functions to convert between BookEntity (Room database) and Book (domain model)
 */

fun BookEntity.toBook(): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        coverColor = coverColor,
        progress = progress,
        lastReadTimestamp = lastReadTimestamp,
        dateAdded = dateAdded,
        currentChapter = currentChapter,
        currentPage = currentPage,
        scrollPosition = scrollPosition,
        totalPages = totalPages,
        tags = if (tags.isNotEmpty()) {
            try {
                Json.decodeFromString<List<String>>(tags)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList(),
        originalMetadataTags = if (originalMetadataTags.isNotEmpty()) {
            try {
                Json.decodeFromString<List<String>>(originalMetadataTags)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList(),
        filePath = filePath,
        originalUri = originalUri,
        backupFilePath = backupFilePath,
        fileSize = fileSize,
        totalChapters = totalChapters,
        description = description,
        publisher = publisher,
        language = language,
        isbn = isbn,
        publishedDate = publishedDate,
        coverImagePath = coverImagePath,
        isImported = isImported,
        coverDisplayMode = try {
            CoverDisplayMode.valueOf(coverDisplayMode)
        } catch (e: Exception) {
            CoverDisplayMode.AUTO
        },
        userSelectedColor = userSelectedColor,
        fileChecksum = fileChecksum,
        userEditedMetadata = userEditedMetadata
    )
}

fun Book.toBookEntity(): BookEntity {
    return BookEntity(
        id = id,
        title = title,
        author = author,
        coverColor = coverColor,
        progress = progress,
        lastReadTimestamp = lastReadTimestamp,
        dateAdded = dateAdded,
        currentChapter = currentChapter,
        currentPage = currentPage,
        scrollPosition = scrollPosition,
        totalPages = totalPages,
        tags = Json.encodeToString(tags),
        originalMetadataTags = Json.encodeToString(originalMetadataTags),
        filePath = filePath,
        originalUri = originalUri,
        backupFilePath = backupFilePath,
        fileSize = fileSize,
        totalChapters = totalChapters,
        description = description,
        publisher = publisher,
        language = language,
        isbn = isbn,
        publishedDate = publishedDate,
        coverImagePath = coverImagePath,
        isImported = isImported,
        coverDisplayMode = coverDisplayMode.name,
        userSelectedColor = userSelectedColor,
        fileChecksum = fileChecksum,
        userEditedMetadata = userEditedMetadata
    )
}