package com.bsikar.helix.data.mapper

import com.bsikar.helix.data.ImportedFile
import com.bsikar.helix.data.source.entities.ImportedFileEntity

fun ImportedFile.toEntity(): ImportedFileEntity {
    return ImportedFileEntity(
        path = path,
        originalPath = originalPath,
        importedAt = importedAt,
        bookId = bookId,
        sourceType = sourceType,
        sourceUri = sourceUri
    )
}

fun ImportedFileEntity.toImportedFile(): ImportedFile {
    return ImportedFile(
        path = path,
        originalPath = originalPath,
        importedAt = importedAt,
        bookId = bookId,
        sourceType = sourceType,
        sourceUri = sourceUri
    )
}

fun List<ImportedFile>.toEntities(): List<ImportedFileEntity> {
    return map { it.toEntity() }
}

fun List<ImportedFileEntity>.toImportedFiles(): List<ImportedFile> {
    return map { it.toImportedFile() }
}