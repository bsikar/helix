package com.bsikar.helix.di

import android.content.Context
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.data.repository.BookRepository
import com.bsikar.helix.data.repository.BookRepositoryImpl
import com.bsikar.helix.data.source.HelixDatabase
import com.bsikar.helix.data.source.dao.BookDao
import com.bsikar.helix.data.source.dao.TagDao
import com.bsikar.helix.data.source.dao.WatchedDirectoryDao
import com.bsikar.helix.data.source.dao.ImportedFileDao
import com.bsikar.helix.data.source.dao.ReadingProgressDao
import com.bsikar.helix.data.source.dao.CachedEpubMetadataDao
import com.bsikar.helix.data.source.dao.ChapterDao
import com.bsikar.helix.data.source.dao.AudioChapterDao
import com.bsikar.helix.data.repository.ReadingProgressRepository
import com.bsikar.helix.data.repository.EpubMetadataCacheRepository
import com.bsikar.helix.data.repository.ChapterRepository
import com.bsikar.helix.data.repository.AudioChapterRepository
import com.bsikar.helix.data.source.dao.ImportTaskDao
import com.bsikar.helix.data.source.dao.ReadingSessionDao
import com.bsikar.helix.data.repository.ReadingAnalyticsRepository
import com.bsikar.helix.data.parser.EpubParser
import com.bsikar.helix.data.parser.M4bParser
import com.bsikar.helix.managers.ImportManager
import com.bsikar.helix.player.AudioBookPlayer
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferencesManager(
        @ApplicationContext context: Context
    ): UserPreferencesManager {
        return UserPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideLibraryManager(
        @ApplicationContext context: Context,
        userPreferencesManager: UserPreferencesManager,
        bookRepository: BookRepository,
        watchedDirectoryDao: WatchedDirectoryDao,
        importedFileDao: ImportedFileDao,
        epubMetadataCacheRepository: EpubMetadataCacheRepository,
        chapterRepository: ChapterRepository,
        audioChapterRepository: AudioChapterRepository,
        m4bParser: M4bParser
    ): LibraryManager {
        return LibraryManager(context, userPreferencesManager, bookRepository, watchedDirectoryDao, importedFileDao, epubMetadataCacheRepository, chapterRepository, audioChapterRepository, m4bParser)
    }

    @Provides
    @Singleton
    fun provideHelixDatabase(
        @ApplicationContext context: Context
    ): HelixDatabase {
        return HelixDatabase.build(context)
    }

    @Provides
    fun provideBookDao(database: HelixDatabase): BookDao {
        return database.bookDao()
    }

    @Provides
    fun provideTagDao(database: HelixDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    fun provideWatchedDirectoryDao(database: HelixDatabase): WatchedDirectoryDao {
        return database.watchedDirectoryDao()
    }

    @Provides
    fun provideImportedFileDao(database: HelixDatabase): ImportedFileDao {
        return database.importedFileDao()
    }

    @Provides
    fun provideReadingProgressDao(database: HelixDatabase): ReadingProgressDao {
        return database.readingProgressDao()
    }

    @Provides
    fun provideCachedEpubMetadataDao(database: HelixDatabase): CachedEpubMetadataDao {
        return database.cachedEpubMetadataDao()
    }

    @Provides
    fun provideChapterDao(database: HelixDatabase): ChapterDao {
        return database.chapterDao()
    }

    @Provides
    fun provideAudioChapterDao(database: HelixDatabase): AudioChapterDao {
        return database.audioChapterDao()
    }

    @Provides
    fun provideImportTaskDao(database: HelixDatabase): ImportTaskDao {
        return database.importTaskDao()
    }

    @Provides
    fun provideReadingSessionDao(database: HelixDatabase): ReadingSessionDao {
        return database.readingSessionDao()
    }

    @Provides
    @Singleton
    fun provideReadingProgressRepository(
        readingProgressDao: ReadingProgressDao
    ): ReadingProgressRepository {
        return ReadingProgressRepository(readingProgressDao)
    }

    @Provides
    @Singleton
    fun provideEpubMetadataCacheRepository(
        cachedEpubMetadataDao: CachedEpubMetadataDao
    ): EpubMetadataCacheRepository {
        return EpubMetadataCacheRepository(cachedEpubMetadataDao)
    }

    @Provides
    @Singleton
    fun provideChapterRepository(
        chapterDao: ChapterDao
    ): ChapterRepository {
        return ChapterRepository(chapterDao)
    }

    @Provides
    @Singleton
    fun provideAudioChapterRepository(
        audioChapterDao: AudioChapterDao
    ): AudioChapterRepository {
        return AudioChapterRepository(audioChapterDao)
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideImportManager(
        @ApplicationContext context: Context,
        workManager: WorkManager,
        importTaskDao: ImportTaskDao,
        importedFileDao: ImportedFileDao
    ): ImportManager {
        return ImportManager(context, workManager, importTaskDao, importedFileDao)
    }

    @Provides
    @Singleton
    fun provideEpubParser(
        @ApplicationContext context: Context
    ): EpubParser {
        return EpubParser(context)
    }

    @Provides
    @Singleton
    fun provideM4bParser(
        @ApplicationContext context: Context
    ): M4bParser {
        return M4bParser(context)
    }

    @Provides
    @Singleton
    fun provideAudioBookPlayer(
        @ApplicationContext context: Context
    ): AudioBookPlayer {
        return AudioBookPlayer(context)
    }

    @Provides
    @Singleton
    fun provideReadingAnalyticsRepository(
        readingSessionDao: ReadingSessionDao,
        bookDao: BookDao
    ): ReadingAnalyticsRepository {
        return ReadingAnalyticsRepository(readingSessionDao, bookDao)
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(
        bookRepositoryImpl: BookRepositoryImpl
    ): BookRepository
}