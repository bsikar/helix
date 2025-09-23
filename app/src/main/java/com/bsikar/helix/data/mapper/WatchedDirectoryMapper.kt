package com.bsikar.helix.data.mapper

import com.bsikar.helix.data.WatchedDirectory
import com.bsikar.helix.data.source.entities.WatchedDirectoryEntity

fun WatchedDirectory.toEntity(): WatchedDirectoryEntity {
    return WatchedDirectoryEntity(
        path = path,
        uri = uri,
        lastScanned = lastScanned,
        recursive = recursive,
        totalBooks = totalBooks,
        isUri = isUri,
        requiresRescan = requiresRescan
    )
}

fun WatchedDirectoryEntity.toWatchedDirectory(): WatchedDirectory {
    return WatchedDirectory(
        path = path,
        uri = uri,
        lastScanned = lastScanned,
        recursive = recursive,
        totalBooks = totalBooks,
        isUri = isUri,
        requiresRescan = requiresRescan
    )
}

fun List<WatchedDirectory>.toEntities(): List<WatchedDirectoryEntity> {
    return map { it.toEntity() }
}

fun List<WatchedDirectoryEntity>.toWatchedDirectories(): List<WatchedDirectory> {
    return map { it.toWatchedDirectory() }
}