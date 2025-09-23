package com.bsikar.helix.data.source

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.bsikar.helix.data.source.dao.BookDao
import com.bsikar.helix.data.source.dao.BookmarkDao
import com.bsikar.helix.data.source.dao.TagDao
import com.bsikar.helix.data.source.dao.WatchedDirectoryDao
import com.bsikar.helix.data.source.dao.ImportedFileDao
import com.bsikar.helix.data.source.dao.ReadingProgressDao
import com.bsikar.helix.data.source.dao.CachedEpubMetadataDao
import com.bsikar.helix.data.source.dao.ChapterDao
import com.bsikar.helix.data.source.dao.ImportTaskDao
import com.bsikar.helix.data.source.dao.ReadingSessionDao
import com.bsikar.helix.data.source.entities.BookEntity
import com.bsikar.helix.data.source.entities.BookmarkEntity
import com.bsikar.helix.data.source.entities.TagEntity
import com.bsikar.helix.data.source.entities.WatchedDirectoryEntity
import com.bsikar.helix.data.source.entities.ImportedFileEntity
import com.bsikar.helix.data.source.entities.ReadingProgressEntity
import com.bsikar.helix.data.source.entities.CachedEpubMetadataEntity
import com.bsikar.helix.data.source.entities.ChapterEntity
import com.bsikar.helix.data.model.ImportTask
import com.bsikar.helix.data.model.ReadingSession

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        TagEntity::class,
        WatchedDirectoryEntity::class,
        ImportedFileEntity::class,
        ReadingProgressEntity::class,
        CachedEpubMetadataEntity::class,
        ChapterEntity::class,
        ImportTask::class,
        ReadingSession::class
    ],
    version = 8,
    exportSchema = false
)
abstract class HelixDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tagDao(): TagDao
    abstract fun watchedDirectoryDao(): WatchedDirectoryDao
    abstract fun importedFileDao(): ImportedFileDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun cachedEpubMetadataDao(): CachedEpubMetadataDao
    abstract fun chapterDao(): ChapterDao
    abstract fun importTaskDao(): ImportTaskDao
    abstract fun readingSessionDao(): ReadingSessionDao
    
    companion object {
        private const val DATABASE_NAME = "helix_database"
        
        fun build(context: Context): HelixDatabase {
            return Room.databaseBuilder(
                context,
                HelixDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}