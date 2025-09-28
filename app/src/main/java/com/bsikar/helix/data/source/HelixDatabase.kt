package com.bsikar.helix.data.source

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.bsikar.helix.data.source.dao.BookDao
import com.bsikar.helix.data.source.dao.TagDao
import com.bsikar.helix.data.source.dao.WatchedDirectoryDao
import com.bsikar.helix.data.source.dao.ImportedFileDao
import com.bsikar.helix.data.source.dao.ReadingProgressDao
import com.bsikar.helix.data.source.dao.CachedEpubMetadataDao
import com.bsikar.helix.data.source.dao.ChapterDao
import com.bsikar.helix.data.source.dao.AudioChapterDao
import com.bsikar.helix.data.source.dao.ImportTaskDao
import com.bsikar.helix.data.source.dao.ReadingSessionDao
import com.bsikar.helix.data.source.entities.BookEntity
import com.bsikar.helix.data.source.entities.TagEntity
import com.bsikar.helix.data.source.entities.WatchedDirectoryEntity
import com.bsikar.helix.data.source.entities.ImportedFileEntity
import com.bsikar.helix.data.source.entities.ReadingProgressEntity
import com.bsikar.helix.data.source.entities.CachedEpubMetadataEntity
import com.bsikar.helix.data.source.entities.ChapterEntity
import com.bsikar.helix.data.source.entities.AudioChapterEntity
import com.bsikar.helix.data.model.ImportTask
import com.bsikar.helix.data.model.ReadingSession

@Database(
    entities = [
        BookEntity::class,
        TagEntity::class,
        WatchedDirectoryEntity::class,
        ImportedFileEntity::class,
        ReadingProgressEntity::class,
        CachedEpubMetadataEntity::class,
        ChapterEntity::class,
        AudioChapterEntity::class,
        ImportTask::class,
        ReadingSession::class
    ],
    version = 11,
    exportSchema = false
)
abstract class HelixDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao
    abstract fun tagDao(): TagDao
    abstract fun watchedDirectoryDao(): WatchedDirectoryDao
    abstract fun importedFileDao(): ImportedFileDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun cachedEpubMetadataDao(): CachedEpubMetadataDao
    abstract fun chapterDao(): ChapterDao
    abstract fun audioChapterDao(): AudioChapterDao
    abstract fun importTaskDao(): ImportTaskDao
    abstract fun readingSessionDao(): ReadingSessionDao
    
    companion object {
        private const val DATABASE_NAME = "helix_database"
        
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add explicitReadingStatus column to books table
                database.execSQL("ALTER TABLE books ADD COLUMN explicitReadingStatus TEXT")
            }
        }
        
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add audiobook fields to books table
                database.execSQL("ALTER TABLE books ADD COLUMN bookType TEXT NOT NULL DEFAULT 'EPUB'")
                database.execSQL("ALTER TABLE books ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE books ADD COLUMN currentPositionMs INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE books ADD COLUMN playbackSpeed REAL NOT NULL DEFAULT 1.0")
                
                // Create audio_chapters table
                database.execSQL("""
                    CREATE TABLE audio_chapters (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        startTimeMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        chapter_order INTEGER NOT NULL,
                        bookId TEXT NOT NULL,
                        FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create index on bookId for audio_chapters
                database.execSQL("CREATE INDEX index_audio_chapters_bookId ON audio_chapters(bookId)")
            }
        }
        
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop bookmarks table as bookmark functionality has been removed
                database.execSQL("DROP TABLE IF EXISTS bookmarks")
            }
        }
        
        fun build(context: Context): HelixDatabase {
            return Room.databaseBuilder(
                context,
                HelixDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
             .build()
        }
    }
}