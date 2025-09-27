package com.bsikar.helix.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*
import android.util.Log
import com.bsikar.helix.utils.FileUtils
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.Bookmark
import com.bsikar.helix.data.model.Tag
import com.bsikar.helix.data.model.TagCategory
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.model.CoverDisplayMode
import com.bsikar.helix.data.repository.BookRepository
import com.bsikar.helix.data.parser.EpubParser
import com.bsikar.helix.data.model.ParsedEpub
import com.bsikar.helix.data.source.dao.WatchedDirectoryDao
import com.bsikar.helix.data.source.dao.ImportedFileDao
import com.bsikar.helix.data.mapper.toEntity
import com.bsikar.helix.data.mapper.toWatchedDirectory
import com.bsikar.helix.data.mapper.toImportedFile
import com.bsikar.helix.data.mapper.toWatchedDirectories
import com.bsikar.helix.data.mapper.toImportedFiles
import com.bsikar.helix.data.repository.EpubMetadataCacheRepository
import com.bsikar.helix.data.repository.ChapterRepository

@Serializable
data class WatchedDirectory(
    val path: String,
    val uri: String? = null, // Store both URI and file path for compatibility
    val lastScanned: Long = System.currentTimeMillis(),
    val recursive: Boolean = true,
    val totalBooks: Int = 0,
    val isUri: Boolean = uri != null, // Flag to know which type to use
    val requiresRescan: Boolean = false // Flag to indicate if directory needs to be scanned
)

@Serializable 
data class ImportedFile(
    val path: String,
    val originalPath: String? = null, // Store original URI/path for directory imports
    val importedAt: Long = System.currentTimeMillis(),
    val bookId: String? = null,
    val sourceType: String = "individual", // "individual", "directory", "rescan"
    val sourceUri: String? = null // Store the directory URI if from directory import
)

class LibraryManager(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager,
    private val bookRepository: BookRepository,
    private val watchedDirectoryDao: WatchedDirectoryDao,
    private val importedFileDao: ImportedFileDao,
    private val epubMetadataCacheRepository: EpubMetadataCacheRepository,
    private val chapterRepository: ChapterRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private val epubParser = EpubParser(context)
    
    private val _books = mutableStateOf<List<com.bsikar.helix.data.model.Book>>(emptyList())
    val books: State<List<com.bsikar.helix.data.model.Book>> = _books
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _importProgress = mutableStateOf<ImportProgress?>(null)
    val importProgress: State<ImportProgress?> = _importProgress
    
    private val _lastImportResult = mutableStateOf<ImportResult?>(null)
    val lastImportResult: State<ImportResult?> = _lastImportResult
    
    private val _watchedDirectories = mutableStateOf<List<WatchedDirectory>>(emptyList())
    val watchedDirectories: State<List<WatchedDirectory>> = _watchedDirectories
    
    private val _importedFiles = mutableStateOf<List<ImportedFile>>(emptyList())
    val importedFiles: State<List<ImportedFile>> = _importedFiles
    
    private var currentImportJob: Job? = null
    
    // Flag to temporarily suppress database flow updates during manual state changes
    private var suppressDatabaseFlow = false
    
    fun cancelCurrentImport() {
        currentImportJob?.cancel()
        currentImportJob = null
        _isLoading.value = false
        _importProgress.value = null
    }
    
    // Observe books from repository and update local state for Compose compatibility
    init {
        scope.launch {
            bookRepository.getAllBooksFlow().collect { bookList ->
                if (!suppressDatabaseFlow) {
                    _books.value = bookList
                }
            }
        }
        loadLibrary()
    }
    
    private fun loadLibrary() {
        scope.launch {
            try {
                // Books are now loaded from Room database via bookRepository
                
                // Load watched directories from Room database
                val watchedDirEntities = watchedDirectoryDao.getAllWatchedDirectories()
                _watchedDirectories.value = watchedDirEntities.toWatchedDirectories()
                
                // Load imported files from Room database
                val importedFileEntities = importedFileDao.getAllImportedFiles()
                _importedFiles.value = importedFileEntities.toImportedFiles()
                
                // Migrate from SharedPreferences if Room database is empty
                if (watchedDirEntities.isEmpty()) {
                    migrateWatchedDirectoriesFromPreferences()
                }
                
                if (importedFileEntities.isEmpty()) {
                    migrateImportedFilesFromPreferences()
                }
                
            } catch (e: Exception) {
                Log.e("LibraryManager", "Error loading library data: ${e.message}", e)
                _watchedDirectories.value = emptyList()
                _importedFiles.value = emptyList()
            }
        }
    }
    
    private suspend fun saveLibrary() {
        try {
            // Books are now persisted in Room database automatically
            
            // Save watched directories to Room database
            val watchedDirEntities = _watchedDirectories.value.map { it.toEntity() }
            watchedDirectoryDao.deleteAllWatchedDirectories()
            watchedDirectoryDao.insertWatchedDirectories(watchedDirEntities)
            
            // Save imported files to Room database
            val importedFileEntities = _importedFiles.value.map { it.toEntity() }
            importedFileDao.deleteAllImportedFiles()
            importedFileDao.insertImportedFiles(importedFileEntities)
            
        } catch (e: Exception) {
            Log.e("LibraryManager", "Error saving library data: ${e.message}", e)
        }
    }
    
    /**
     * Migrate watched directories from SharedPreferences to Room database
     */
    private suspend fun migrateWatchedDirectoriesFromPreferences() {
        try {
            val watchedDirsJson = preferencesManager.getWatchedDirectories()
            if (watchedDirsJson.isNotEmpty()) {
                val loadedDirs = Json.decodeFromString<List<WatchedDirectory>>(watchedDirsJson)
                val entities = loadedDirs.map { it.toEntity() }
                watchedDirectoryDao.insertWatchedDirectories(entities)
                _watchedDirectories.value = loadedDirs
                
                // Clear SharedPreferences after successful migration
                preferencesManager.saveWatchedDirectories("")
                Log.i("LibraryManager", "Migrated ${loadedDirs.size} watched directories from SharedPreferences to Room")
            }
        } catch (e: Exception) {
            Log.e("LibraryManager", "Error migrating watched directories from SharedPreferences: ${e.message}", e)
        }
    }
    
    /**
     * Migrate imported files from SharedPreferences to Room database
     */
    private suspend fun migrateImportedFilesFromPreferences() {
        try {
            val importedFilesJson = preferencesManager.getImportedFiles()
            if (importedFilesJson.isNotEmpty()) {
                val loadedFiles = Json.decodeFromString<List<ImportedFile>>(importedFilesJson)
                val entities = loadedFiles.map { it.toEntity() }
                importedFileDao.insertImportedFiles(entities)
                _importedFiles.value = loadedFiles
                
                // Clear SharedPreferences after successful migration
                preferencesManager.saveImportedFiles("")
                Log.i("LibraryManager", "Migrated ${loadedFiles.size} imported files from SharedPreferences to Room")
            }
        } catch (e: Exception) {
            Log.e("LibraryManager", "Error migrating imported files from SharedPreferences: ${e.message}", e)
        }
    }
    
    suspend fun importEpubFileForRescan(file: File, directoryPath: String): Result<Book> = withContext(Dispatchers.IO) {
        try {
            // Use file path as cache key first
            val cacheKey = file.absolutePath.hashCode().toString()
            
            // Check cache first using file path
            val cachedMetadata = epubMetadataCacheRepository.getCachedMetadata(cacheKey)
            val isValidCache = cachedMetadata != null && epubMetadataCacheRepository.isCacheValid(cacheKey, file.absolutePath)
            
            val parsedEpub = if (isValidCache) {
                Log.d("LibraryManager", "Using cached metadata for ${file.name}")
                cachedMetadata
            } else {
                Log.d("LibraryManager", "Cache miss or invalid, parsing ${file.name}")
                val startTime = System.currentTimeMillis()
                val epubResult = epubParser.parseEpubMetadataOnly(file)
                if (epubResult.isFailure) {
                    return@withContext Result.failure(epubResult.exceptionOrNull() ?: Exception("Failed to parse EPUB"))
                }
                
                val parsed = epubResult.getOrThrow()
                val parsingTime = System.currentTimeMillis() - startTime
                
                // Cache the parsed metadata using cache key
                epubMetadataCacheRepository.cacheMetadata(cacheKey, parsed, parsingTime)
                
                parsed
            }
            
            val bookId = UUID.randomUUID().toString()
            
            // Extract cover art if available
            val extractedCoverPath = try {
                val coverResult = epubParser.extractCoverArt(file, bookId, parsedEpub.opfPath)
                if (coverResult.isSuccess) {
                    coverResult.getOrNull()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w("LibraryManager", "Failed to extract cover art: ${e.message}")
                null
            }
            
            // Check for duplicate books before importing
            val existingBooks = bookRepository.getAllBooks()
            val duplicateBook = existingBooks.firstOrNull { existingBook ->
                existingBook.title.equals(parsedEpub.metadata.title, ignoreCase = true) &&
                existingBook.author.equals(parsedEpub.metadata.author ?: "Unknown Author", ignoreCase = true) &&
                existingBook.fileSize == file.length()
            }
            
            if (duplicateBook != null) {
                Log.d("LibraryManager", "Duplicate book detected in rescan: ${parsedEpub.metadata.title} by ${parsedEpub.metadata.author}")
                return@withContext Result.failure(Exception("This book is already in your library: \"${duplicateBook.title}\" by ${duplicateBook.author}"))
            }
            
            val book = Book(
                id = bookId,
                title = parsedEpub.metadata.title,
                author = parsedEpub.metadata.author ?: "Unknown Author",
                coverColor = generateCoverColor(parsedEpub.metadata.title, file.absolutePath),
                filePath = file.absolutePath, // This is the direct file path for rescan
                originalUri = null, // No URI for file-based rescans
                backupFilePath = null, // No backup needed for direct file access
                fileSize = file.length(),
                totalChapters = parsedEpub.chapterCount,
                description = parsedEpub.metadata.description,
                publisher = parsedEpub.metadata.publisher,
                language = parsedEpub.metadata.language,
                isbn = parsedEpub.metadata.isbn,
                publishedDate = parsedEpub.metadata.publishedDate,
                coverImagePath = extractedCoverPath,
                isImported = true,
                tags = emptyList(),
                originalMetadataTags = parsedEpub.metadata.subjects,
                explicitReadingStatus = ReadingStatus.UNREAD
            )
            
            // Track this as a rescan import from a watched directory
            val importedFile = ImportedFile(
                path = file.absolutePath,
                originalPath = file.absolutePath, // For rescans, original path is the file path
                importedAt = System.currentTimeMillis(),
                bookId = bookId,
                sourceType = "rescan", // Mark as rescan import
                sourceUri = directoryPath // Store the watched directory path
            )
            
            bookRepository.insertBook(book)
            
            // Store chapters for navigation
            chapterRepository.storeChaptersFromEpub(bookId, parsedEpub)
            
            val updatedImportedFiles = _importedFiles.value.toMutableList()
            updatedImportedFiles.add(importedFile)
            _importedFiles.value = updatedImportedFiles
            
            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importEpubFromUri(parsedEpub: ParsedEpub, originalUri: String, directoryUri: String, needsBackupCopy: Boolean = false): Result<Book> = withContext(Dispatchers.IO) {
        try {
            // Check for duplicates before creating the book
            val duplicateBook = _books.value.find { existingBook ->
                existingBook.title.equals(parsedEpub.metadata.title ?: "Unknown Title", ignoreCase = true) &&
                existingBook.author.equals(parsedEpub.metadata.author ?: "Unknown Author", ignoreCase = true)
            }
            
            if (duplicateBook != null) {
                Log.d("LibraryManager", "Duplicate book detected in URI import: ${parsedEpub.metadata.title} by ${parsedEpub.metadata.author}")
                return@withContext Result.failure(Exception("This book is already in your library: \"${duplicateBook.title}\" by ${duplicateBook.author}"))
            }
            
            val bookId = UUID.randomUUID().toString()
            
            // Calculate checksum for file change detection
            val fileChecksum = FileUtils.calculateFastChecksum(context, originalUri)
            Log.d("LibraryManager", "Calculated checksum for ${parsedEpub.metadata.title}: $fileChecksum")
            
            // Never create backup copies - use URI-only access for better performance and storage
            val backupFilePath: String? = null
            
            // Extract cover art if available (for URI-based imports, create temporary file)
            val extractedCoverPath = try {
                val tempFile = File.createTempFile("cover_extract_", ".epub", context.cacheDir)
                try {
                    // Copy URI content to temp file for cover extraction
                    context.contentResolver.openInputStream(Uri.parse(originalUri))?.use { inputStream ->
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    val coverResult = epubParser.extractCoverArt(tempFile, bookId, parsedEpub.opfPath)
                    if (coverResult.isSuccess) {
                        coverResult.getOrNull()
                    } else {
                        null
                    }
                } finally {
                    // Always clean up temp file
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                Log.w("LibraryManager", "Failed to extract cover art from URI: ${e.message}")
                null
            }
            
            val book = Book(
                id = bookId,
                title = parsedEpub.metadata.title,
                author = parsedEpub.metadata.author ?: "Unknown Author",
                coverColor = generateCoverColor(parsedEpub.metadata.title, backupFilePath ?: originalUri),
                filePath = null, // No direct file path for URI-based imports
                originalUri = originalUri, // Store the original URI
                backupFilePath = backupFilePath, // Store backup path if created
                fileSize = backupFilePath?.let { File(it).length() } ?: 0L,
                totalChapters = parsedEpub.chapterCount,
                description = parsedEpub.metadata.description,
                publisher = parsedEpub.metadata.publisher,
                language = parsedEpub.metadata.language,
                isbn = parsedEpub.metadata.isbn,
                publishedDate = parsedEpub.metadata.publishedDate,
                coverImagePath = extractedCoverPath,
                isImported = true,
                tags = emptyList(),
                originalMetadataTags = parsedEpub.metadata.subjects,
                explicitReadingStatus = ReadingStatus.UNREAD,
                fileChecksum = fileChecksum,
                userEditedMetadata = false
            )
            
            // Add to library
            Log.d("LibraryManager", "Inserting book into database: ${book.title}")
            bookRepository.insertBook(book)
            Log.d("LibraryManager", "Successfully inserted book into database: ${book.title}")
            
            // Cache the metadata after book insertion (using correct bookId)
            epubMetadataCacheRepository.cacheMetadata(bookId, parsedEpub)
            
            // Track imported file - use URI for path since no backup file exists
            val updatedImportedFiles = _importedFiles.value.toMutableList()
            updatedImportedFiles.add(ImportedFile(
                path = originalUri, // Use URI as path since no backup file
                originalPath = originalUri,
                importedAt = System.currentTimeMillis(),
                bookId = book.id,
                sourceType = "directory",
                sourceUri = directoryUri
            ))
            _importedFiles.value = updatedImportedFiles
            
            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importEpubFileForDirectory(file: File, originalUri: String, directoryUri: String): Result<Book> = withContext(Dispatchers.IO) {
        try {
            // Use file path as cache key
            val cacheKey = file.absolutePath.hashCode().toString()
            
            // Check cache first
            val cachedMetadata = epubMetadataCacheRepository.getCachedMetadata(cacheKey)
            val isValidCache = cachedMetadata != null && epubMetadataCacheRepository.isCacheValid(cacheKey, file.absolutePath)
            
            var parsingTime = 0L
            val parsedEpub = if (isValidCache) {
                Log.d("LibraryManager", "Using cached metadata for ${file.name}")
                cachedMetadata
            } else {
                Log.d("LibraryManager", "Cache miss or invalid, parsing ${file.name}")
                val startTime = System.currentTimeMillis()
                val epubResult = epubParser.parseEpubMetadataOnly(file)
                if (epubResult.isFailure) {
                    return@withContext Result.failure(epubResult.exceptionOrNull() ?: Exception("Failed to parse EPUB"))
                }
                
                val parsed = epubResult.getOrThrow()
                parsingTime = System.currentTimeMillis() - startTime
                
                // Cache will be done after book insertion with correct bookId
                
                parsed
            }
            
            val bookId = UUID.randomUUID().toString()
            
            // Extract cover art if available
            val extractedCoverPath = try {
                val coverResult = epubParser.extractCoverArt(file, bookId, parsedEpub.opfPath)
                if (coverResult.isSuccess) {
                    coverResult.getOrNull()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w("LibraryManager", "Failed to extract cover art: ${e.message}")
                null
            }
            
            // Check for duplicate books before importing
            val existingBooks = bookRepository.getAllBooks()
            val duplicateBook = existingBooks.firstOrNull { existingBook ->
                existingBook.title.equals(parsedEpub.metadata.title, ignoreCase = true) &&
                existingBook.author.equals(parsedEpub.metadata.author ?: "Unknown Author", ignoreCase = true) &&
                existingBook.fileSize == file.length()
            }
            
            if (duplicateBook != null) {
                Log.d("LibraryManager", "Duplicate book detected in directory import: ${parsedEpub.metadata.title} by ${parsedEpub.metadata.author}")
                return@withContext Result.failure(Exception("This book is already in your library: \"${duplicateBook.title}\" by ${duplicateBook.author}"))
            }
            
            val book = Book(
                id = bookId,
                title = parsedEpub.metadata.title,
                author = parsedEpub.metadata.author ?: "Unknown Author",
                coverColor = generateCoverColor(parsedEpub.metadata.title, file.absolutePath),
                filePath = file.absolutePath, // This is the backup file path for directory imports
                originalUri = originalUri, // Store the original URI
                backupFilePath = file.absolutePath, // Same as filePath for this case  
                fileSize = file.length(),
                totalChapters = parsedEpub.chapterCount,
                description = parsedEpub.metadata.description,
                publisher = parsedEpub.metadata.publisher,
                language = parsedEpub.metadata.language,
                isbn = parsedEpub.metadata.isbn,
                publishedDate = parsedEpub.metadata.publishedDate,
                coverImagePath = extractedCoverPath,
                isImported = true,
                tags = emptyList(),
                originalMetadataTags = parsedEpub.metadata.subjects,
                explicitReadingStatus = ReadingStatus.UNREAD
            )
            
            // Track this as a directory import with proper source information
            val importedFile = ImportedFile(
                path = file.absolutePath,
                originalPath = originalUri, // Store the original DocumentFile URI
                importedAt = System.currentTimeMillis(),
                bookId = bookId,
                sourceType = "directory",
                sourceUri = directoryUri // Store the directory URI
            )
            
            bookRepository.insertBook(book)
            
            // Cache the metadata after book insertion (using correct bookId)
            if (!isValidCache) {
                // Only cache if we actually parsed (not using cached data)
                epubMetadataCacheRepository.cacheMetadata(bookId, parsedEpub, parsingTime)
            }
            
            val updatedImportedFiles = _importedFiles.value.toMutableList()
            updatedImportedFiles.add(importedFile)
            _importedFiles.value = updatedImportedFiles
            
            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importEpubFile(file: File): Result<Book> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            
            // Use file path as cache key
            val cacheKey = file.absolutePath.hashCode().toString()
            
            // Check cache first
            val cachedMetadata = epubMetadataCacheRepository.getCachedMetadata(cacheKey)
            val isValidCache = cachedMetadata != null && epubMetadataCacheRepository.isCacheValid(cacheKey, file.absolutePath)
            
            var parsingTime = 0L
            val parsedEpub = if (isValidCache) {
                Log.d("LibraryManager", "Using cached metadata for ${file.name}")
                cachedMetadata
            } else {
                Log.d("LibraryManager", "Cache miss or invalid, parsing ${file.name}")
                val startTime = System.currentTimeMillis()
                val parseResult = epubParser.parseEpubMetadataOnly(file)
                if (parseResult.isFailure) {
                    return@withContext Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to parse EPUB"))
                }
                
                val parsed = parseResult.getOrThrow()
                parsingTime = System.currentTimeMillis() - startTime
                
                // Cache will be done after book insertion with correct bookId
                
                parsed
            }
            
            val bookId = UUID.randomUUID().toString()
            
            // For direct imports from temporary files, create a permanent copy
            val permanentFilePath = if (file.parent == context.cacheDir.absolutePath) {
                // This is a temporary file, create a permanent copy
                val epubDir = File(context.filesDir, "epubs")
                if (!epubDir.exists()) {
                    epubDir.mkdirs()
                }
                val permanentFile = File(epubDir, "direct_import_$bookId.epub")
                file.copyTo(permanentFile, overwrite = true)
                permanentFile.absolutePath
            } else {
                // This is already a permanent file path
                file.absolutePath
            }
            
            // Extract cover art if available (use permanent file path for extraction)
            val extractedCoverPath = try {
                val fileToExtractFrom = File(permanentFilePath)
                val coverResult = epubParser.extractCoverArt(fileToExtractFrom, bookId, parsedEpub.opfPath)
                if (coverResult.isSuccess) {
                    coverResult.getOrNull()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w("LibraryManager", "Failed to extract cover art: ${e.message}")
                null
            }
            
            // Check for duplicate books before importing (use in-memory state for consistency)
            val duplicateBook = _books.value.firstOrNull { existingBook ->
                existingBook.title.equals(parsedEpub.metadata.title, ignoreCase = true) &&
                existingBook.author.equals(parsedEpub.metadata.author ?: "Unknown Author", ignoreCase = true) &&
                existingBook.fileSize == file.length()
            }
            
            if (duplicateBook != null) {
                Log.d("LibraryManager", "Duplicate book detected: ${parsedEpub.metadata.title} by ${parsedEpub.metadata.author}")
                return@withContext Result.failure(Exception("This book is already in your library: \"${duplicateBook.title}\" by ${duplicateBook.author}"))
            }
            
            // Create Book from parsed EPUB
            val book = Book(
                id = bookId,
                title = parsedEpub.metadata.title,
                author = parsedEpub.metadata.author ?: "Unknown Author",
                coverColor = generateCoverColor(parsedEpub.metadata.title, file.absolutePath),
                filePath = permanentFilePath, // Use permanent file path
                originalUri = null, // No URI for direct file imports
                backupFilePath = null, // No backup needed - we have direct access
                fileSize = file.length(),
                totalChapters = parsedEpub.chapterCount,
                description = parsedEpub.metadata.description,
                publisher = parsedEpub.metadata.publisher,
                language = parsedEpub.metadata.language,
                isbn = parsedEpub.metadata.isbn,
                publishedDate = parsedEpub.metadata.publishedDate,
                coverImagePath = extractedCoverPath,
                isImported = true,
                originalMetadataTags = parsedEpub.metadata.subjects
            )
            
            // Add to library
            bookRepository.insertBook(book)
            
            // Cache the metadata after book insertion (using correct bookId)
            if (!isValidCache) {
                // Only cache if we actually parsed (not using cached data)
                epubMetadataCacheRepository.cacheMetadata(bookId, parsedEpub, parsingTime)
            }
            
            // Track imported file as individual import
            val updatedImportedFiles = _importedFiles.value.toMutableList()
            updatedImportedFiles.add(ImportedFile(
                path = permanentFilePath,
                originalPath = file.absolutePath, // Store original temp path for reference
                importedAt = System.currentTimeMillis(),
                bookId = book.id,
                sourceType = "individual", // Mark as individual import
                sourceUri = null // No directory URI for individual imports
            ))
            _importedFiles.value = updatedImportedFiles
            
            saveLibrary()
            
            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun importEpubsFromDirectory(directory: File, recursive: Boolean = true): Result<List<com.bsikar.helix.data.model.Book>> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val importedBooks = mutableListOf<Book>()
            val epubFiles = findEpubFiles(directory, recursive)
            
            _importProgress.value = ImportProgress(0, epubFiles.size, "Starting import...")
            
            epubFiles.forEachIndexed { index, file ->
                // Step 1: Starting file processing (0% of this file's progress)
                _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Starting ${file.name}", 0.0f)
                
                // Step 2: Parsing EPUB (33% of this file's progress)
                _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Parsing ${file.name}", 0.33f)
                
                // Step 3: Processing content (66% of this file's progress)
                _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Processing ${file.name}", 0.66f)
                
                val result = importEpubFile(file)
                if (result.isSuccess) {
                    importedBooks.add(result.getOrThrow())
                }
                
                // Step 4: Completed file (100% of this file's progress)
                _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Completed ${file.name}", 1.0f)
            }
            
            // Add/update watched directory
            val updatedWatchedDirs = _watchedDirectories.value.toMutableList()
            val existingDirIndex = updatedWatchedDirs.indexOfFirst { it.path == directory.absolutePath }
            
            val watchedDir = WatchedDirectory(
                path = directory.absolutePath,
                lastScanned = System.currentTimeMillis(),
                recursive = recursive,
                totalBooks = importedBooks.size
            )
            
            if (existingDirIndex >= 0) {
                updatedWatchedDirs[existingDirIndex] = watchedDir
            } else {
                updatedWatchedDirs.add(watchedDir)
            }
            _watchedDirectories.value = updatedWatchedDirs
            
            _importProgress.value = null
            Result.success(importedBooks)
        } catch (e: Exception) {
            _importProgress.value = null
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun importEpubsFromDirectory(directoryUri: String, context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d("LibraryManager", "Starting directory import from: $directoryUri")
            _isLoading.value = true
            val uri = Uri.parse(directoryUri)
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            
            if (documentFile == null || !documentFile.isDirectory) {
                Log.e("LibraryManager", "Invalid directory: documentFile=$documentFile, isDirectory=${documentFile?.isDirectory}")
                return@withContext Result.failure(Exception("Invalid directory"))
            }
            
            Log.d("LibraryManager", "Found directory: ${documentFile.name}")
            val epubFiles = findEpubFilesFromDocumentFile(documentFile)
            Log.d("LibraryManager", "Found ${epubFiles.size} EPUB files in directory")
            
            // Don't show progress if no files found
            if (epubFiles.isEmpty()) {
                _importProgress.value = null
                return@withContext Result.success(0)
            }
            
            // Log existing library state  
            Log.d("LibraryManager", "Current library has ${_books.value.size} books")
            val existingFilePaths = _books.value.mapNotNull { it.filePath }.toSet()
            val existingTitles = _books.value.map { "${it.title}:${it.author}" }.toSet()
            Log.d("LibraryManager", "Existing file paths: ${existingFilePaths.size}, existing titles: ${existingTitles.size}")
            
            val newFiles = epubFiles.filter { docFile ->
                // For URIs, we can't easily check filePath, so we'll check during import
                true // We'll do duplicate check during individual file processing
            }
            
            _importProgress.value = ImportProgress(0, newFiles.size, "Starting import...")
            
            var importedCount = 0
            var skippedCount = 0
            var failedCount = 0
            val successfullyImportedBooks = mutableListOf<Book>()
            
            epubFiles.forEachIndexed { index, docFile ->
                // Check if job was cancelled
                if (currentImportJob?.isCancelled == true) {
                    throw Exception("Import cancelled by user")
                }
                
                val fileName = docFile.name ?: "Unknown file"
                Log.d("LibraryManager", "Processing file ${index + 1}/${epubFiles.size}: $fileName (${docFile.length()} bytes)")
                
                try {
                    // 25 granular steps per file for smooth progress
                    val steps = listOf(
                        0.00f to "Starting", 0.04f to "Initializing", 0.08f to "Copying",
                        0.12f to "Transferring", 0.16f to "Copying data", 0.20f to "File copied",
                        0.24f to "Opening EPUB", 0.28f to "Reading container", 0.32f to "Parsing manifest",
                        0.36f to "Loading metadata", 0.40f to "Processing chapters", 0.44f to "Extracting content",
                        0.48f to "Reading structure", 0.52f to "Validating format", 0.56f to "Processing images",
                        0.60f to "Generating preview", 0.64f to "Creating book entry", 0.68f to "Updating database",
                        0.72f to "Adding to library", 0.76f to "Finalizing metadata", 0.80f to "Saving progress",
                        0.84f to "Updating index", 0.88f to "Cleaning up", 0.92f to "Almost done",
                        0.96f to "Finishing", 1.0f to "Completed"
                    )
                    
                    // Optimized import: Parse directly from URI without copying files
                    _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Parsing $fileName", 0.2f)
                    
                    // Parse directly from URI without copying - this is much faster!
                    var parsedEpub: ParsedEpub? = null
                    
                    try {
                        Log.d("LibraryManager", "Opening input stream for $fileName")
                        context.contentResolver.openInputStream(docFile.uri)?.use { inputStream ->
                            // Use the more reliable temp file approach like single imports
                            val fileSize = docFile.length()
                            Log.d("LibraryManager", "Copying $fileName to temp file for reliable parsing (${fileSize} bytes)")
                            
                            // Create temp file and copy content
                            val tempFile = File.createTempFile("epub_import_", ".epub", context.cacheDir)
                            try {
                                tempFile.outputStream().use { output ->
                                    inputStream.copyTo(output)
                                }
                                
                                // Use the same parsing method as single file imports for consistency
                                Log.d("LibraryManager", "Parsing EPUB metadata for $fileName")
                                val parseResult = epubParser.parseEpubMetadataOnly(tempFile)
                                
                                if (parseResult.isSuccess) {
                                    parsedEpub = parseResult.getOrThrow()
                                    Log.d("LibraryManager", "Successfully parsed $fileName: ${parsedEpub?.metadata?.title}")
                                    _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Processing $fileName", 0.6f)
                                } else {
                                    val error = parseResult.exceptionOrNull()
                                    Log.e("LibraryManager", "Failed to parse $fileName: ${error?.message}", error)
                                    throw error ?: Exception("Parse failed")
                                }
                            } finally {
                                // Clean up temp file
                                if (tempFile.exists()) {
                                    tempFile.delete()
                                }
                            }
                        } ?: throw Exception("Could not open input stream")
                    } catch (e: Exception) {
                        // If parsing fails, skip this file instead of copying
                        Log.e("LibraryManager", "Failed to process $fileName: ${e.message}", e)
                        failedCount++
                        _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Failed to parse: $fileName", 1.0f)
                        return@forEachIndexed
                    }
                    
                    val result = if (parsedEpub != null) {
                        // Successfully parsed from URI - create book with URI reference only
                        _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Adding $fileName", 0.8f)
                        importEpubFromUri(parsedEpub!!, docFile.uri.toString(), directoryUri, false) // Never copy
                    } else {
                        // This should never happen now since we return early on parse failure
                        Result.failure(Exception("Parsing failed"))
                    }
                    
                    if (result.isSuccess) {
                        val book = result.getOrThrow()
                        Log.d("LibraryManager", "Successfully imported book: ${book.title} by ${book.author}")
                        importedCount++
                        successfullyImportedBooks.add(book)
                        
                        // Finalization with progress updates
                        _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Completed $fileName", 1.0f)
                    } else {
                        // Handle failed imports (including duplicates)
                        val error = result.exceptionOrNull()
                        if (error?.message?.contains("already in your library") == true) {
                            Log.d("LibraryManager", "Skipping duplicate: $fileName")
                            skippedCount++
                            _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Skipped duplicate: $fileName", 1.0f)
                        } else {
                            Log.e("LibraryManager", "Import failed for $fileName: ${error?.message}", error)
                            failedCount++
                            _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Failed: $fileName", 1.0f)
                        }
                    }
                    
                    // File was never copied, nothing to clean up!
                } catch (e: Exception) {
                    if (e.message?.contains("cancelled") == true) {
                        throw e // Re-throw cancellation
                    }
                    _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Failed: $fileName", 1.0f)
                }
            }
            
            // Only add to watched directories if import completed successfully (not cancelled)
            if (currentImportJob?.isActive == true || currentImportJob == null) {
                val updatedWatchedDirs = _watchedDirectories.value.toMutableList()
                val existingDirIndex = updatedWatchedDirs.indexOfFirst { it.uri == directoryUri }
                
                val watchedDir = WatchedDirectory(
                    path = documentFile?.name ?: "Unknown Directory",
                    uri = directoryUri,
                    lastScanned = System.currentTimeMillis(),
                    recursive = true,
                    totalBooks = importedCount,
                    isUri = true
                )
                
                if (existingDirIndex >= 0) {
                    updatedWatchedDirs[existingDirIndex] = watchedDir
                } else {
                    updatedWatchedDirs.add(watchedDir)
                }
                _watchedDirectories.value = updatedWatchedDirs
            }
            
            // Log final results
            Log.d("LibraryManager", "Import completed: $importedCount imported, $skippedCount skipped, $failedCount failed")
            Log.d("LibraryManager", "Library now has ${_books.value.size} books total")
            
            // Save the library to persist the imported books
            saveLibrary()
            Log.d("LibraryManager", "Library saved to preferences")
            
            _importProgress.value = null
            Result.success(importedCount)
        } catch (e: Exception) {
            _importProgress.value = null
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    private fun findEpubFilesFromDocumentFile(directory: DocumentFile): List<DocumentFile> {
        val epubFiles = mutableListOf<DocumentFile>()
        
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                epubFiles.addAll(findEpubFilesFromDocumentFile(file))
            } else if (file.name?.lowercase()?.endsWith(".epub") == true) {
                epubFiles.add(file)
            }
        }
        
        return epubFiles
    }
    
    private fun findEpubFiles(directory: File, recursive: Boolean): List<File> {
        val epubFiles = mutableListOf<File>()
        
        if (!directory.exists() || !directory.isDirectory) {
            return epubFiles
        }
        
        directory.listFiles()?.forEach { file ->
            when {
                file.isFile && file.extension.equals("epub", ignoreCase = true) -> {
                    epubFiles.add(file)
                }
                file.isDirectory && recursive -> {
                    epubFiles.addAll(findEpubFiles(file, recursive))
                }
            }
        }
        
        return epubFiles
    }
    
    private fun generateCoverColor(title: String, filePath: String? = null): Long {
        // Expanded color palette with more vibrant and diverse colors
        val colors = listOf(
            // Blues
            Color(0xFF6B73FF), Color(0xFF3498DB), Color(0xFF1E88E5), Color(0xFF039BE5), Color(0xFF00ACC1),
            // Purples and Pinks
            Color(0xFF9B59B6), Color(0xFF8E24AA), Color(0xFFD81B60), Color(0xFFEC4899), Color(0xFFE91E63),
            // Greens
            Color(0xFF1ABC9C), Color(0xFF2ECC71), Color(0xFF43A047), Color(0xFF00C853), Color(0xFF4CAF50),
            // Oranges and Reds
            Color(0xFFF39C12), Color(0xFFE67E22), Color(0xFFFF6347), Color(0xFFE74C3C), Color(0xFFFF5722),
            // Yellows and Golds
            Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFF57F17), Color(0xFFFFB300),
            // Teals and Cyans
            Color(0xFF319795), Color(0xFF4FD1C7), Color(0xFF26A69A), Color(0xFF00BCD4), Color(0xFF00E5FF),
            // Browns and Earthy
            Color(0xFF744210), Color(0xFF8D6E63), Color(0xFF5D4037), Color(0xFFA1887F), Color(0xFF6D4C41),
            // Grays and Neutrals
            Color(0xFF34495E), Color(0xFF607D8B), Color(0xFF546E7A), Color(0xFF90A4AE), Color(0xFF718096),
            // Additional vibrant colors
            Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF009688), Color(0xFF795548)
        )
        
        // Use file name for more consistent hashing, fallback to title
        val hashInput = filePath?.let { 
            java.io.File(it).nameWithoutExtension 
        } ?: title
        
        // Use a more robust hashing approach
        val hash = hashInput.hashCode()
        val colorIndex = kotlin.math.abs(hash) % colors.size
        return colors[colorIndex].value.toLong()
    }
    
    fun updateBookProgress(bookId: String, chapterNumber: Int, pageNumber: Int, scrollPosition: Int) {
        val updatedBooks = _books.value.map { book ->
            if (book.id == bookId) {
                val totalPages = book.totalPages
                val currentPage = pageNumber
                
                // Calculate new progress based on book type
                val calculatedProgress = if (book.isImported) {
                    // For EPUB books, calculate progress based on chapters
                    val totalChapters = book.totalChapters
                    if (totalChapters > 0) {
                        chapterNumber.toFloat() / totalChapters
                    } else {
                        // Fallback to page-based calculation if chapters not available
                        if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
                    }
                } else {
                    // For regular books (PDFs, etc.), use page-based calculation
                    if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
                }
                
                // Smart progress logic:
                // - If book has no progress (0f), use calculated progress or minimum reading progress
                // - If book already has progress, maintain it or use calculated if it's higher
                // - Never decrease progress below existing unless it's 0
                val newProgress = when {
                    book.progress == 0f -> {
                        // Book hasn't been started - use calculated progress or minimum reading progress
                        if (calculatedProgress > 0f) calculatedProgress else 0.05f
                    }
                    calculatedProgress > book.progress -> {
                        // Reader has advanced further, use the higher progress
                        calculatedProgress
                    }
                    else -> {
                        // Maintain existing progress (don't go backwards)
                        book.progress
                    }
                }
                
                book.copy(
                    currentChapter = chapterNumber,
                    currentPage = currentPage,
                    scrollPosition = scrollPosition,
                    progress = newProgress.coerceIn(0f, 1f),
                    lastReadTimestamp = System.currentTimeMillis()
                    // Preserve explicitReadingStatus - don't modify it here
                )
            } else {
                book
            }
        }
        
        _books.value = updatedBooks
        scope.launch { saveLibrary() }
    }
    
    /**
     * Update a book's explicit reading status
     */
    fun updateBookStatus(bookId: String, status: ReadingStatus) {
        val updatedBooks = _books.value.map { book ->
            if (book.id == bookId) {
                book.copy(explicitReadingStatus = status)
            } else {
                book
            }
        }
        
        _books.value = updatedBooks
        scope.launch { saveLibrary() }
    }
    
    /**
     * Update book progress directly with a given progress value
     * This is used when starting reading or manually setting progress
     */
    fun updateBookProgressDirect(bookId: String, progress: Float) {
        val updatedBooks = _books.value.map { book ->
            if (book.id == bookId) {
                book.copy(
                    progress = progress.coerceIn(0f, 1f),
                    lastReadTimestamp = System.currentTimeMillis()
                )
            } else {
                book
            }
        }
        
        _books.value = updatedBooks
        scope.launch { saveLibrary() }
    }
    
    fun updateBookTags(bookId: String, tags: List<String>) {
        // Update in-memory state first
        val updatedBooks = _books.value.map { book ->
            if (book.id == bookId) {
                book.copy(tags = tags)
            } else {
                book
            }
        }
        _books.value = updatedBooks
        
        // Then persist to database with flicker prevention
        scope.launch {
            try {
                // Temporarily suppress database flow to prevent flicker
                suppressDatabaseFlow = true
                
                val updatedBook = updatedBooks.find { it.id == bookId }
                if (updatedBook != null) {
                    bookRepository.updateBook(updatedBook)
                    
                    // Wait a bit for database update to complete
                    kotlinx.coroutines.delay(100)
                    
                    saveLibrary()
                }
            } catch (e: Exception) {
                Log.e("LibraryManager", "Error updating book tags: ${e.message}", e)
            } finally {
                // Re-enable database flow
                suppressDatabaseFlow = false
            }
        }
    }
    
    fun updateBookSettings(updatedBook: com.bsikar.helix.data.model.Book) {
        println("LibraryManager.updateBookSettings: Updating book ${updatedBook.id}")
        println("  Title: ${updatedBook.title}")
        println("  Progress: ${updatedBook.progress}")
        println("  ExplicitReadingStatus: ${updatedBook.explicitReadingStatus}")
        println("  CoverDisplayMode: ${updatedBook.coverDisplayMode}")
        
        // Update in-memory state first
        val updatedBooks = _books.value.map { book ->
            if (book.id == updatedBook.id) updatedBook else book
        }
        _books.value = updatedBooks
        
        println("  Updated _books.value size: ${_books.value.size}")
        println("  Books with progress > 0: ${_books.value.count { it.progress > 0f }}")
        
        // Persist to database with flicker prevention
        scope.launch {
            try {
                // Temporarily suppress database flow to prevent flicker
                suppressDatabaseFlow = true
                
                // Update only the specific book in database
                bookRepository.updateBook(updatedBook)
                
                // Wait for database update to complete
                kotlinx.coroutines.delay(100)
                
                saveLibrary()
            } catch (e: Exception) {
                Log.e("LibraryManager", "Error updating book settings: ${e.message}", e)
            } finally {
                // Re-enable database flow
                suppressDatabaseFlow = false
            }
        }
    }
    
    fun updateBookMetadata(bookId: String, metadataUpdate: com.bsikar.helix.ui.components.BookMetadataUpdate) {
        val updatedBooks = _books.value.map { book ->
            if (book.id == bookId) {
                book.copy(
                    title = metadataUpdate.title,
                    author = metadataUpdate.author,
                    description = metadataUpdate.description,
                    publisher = metadataUpdate.publisher,
                    language = metadataUpdate.language,
                    isbn = metadataUpdate.isbn,
                    publishedDate = metadataUpdate.publishedDate,
                    coverDisplayMode = metadataUpdate.coverDisplayMode,
                    userSelectedColor = metadataUpdate.userSelectedColor?.value?.toLong(),
                    userEditedMetadata = true // Mark that user has edited metadata
                )
            } else {
                book
            }
        }
        
        _books.value = updatedBooks
        scope.launch { saveLibrary() }
    }
    
    fun removeBook(bookId: String) {
        scope.launch {
            // Get book before deletion to invalidate cache
            val book = bookRepository.getBookById(bookId)
            book?.let {
                // Invalidate cache for this book
                val cacheKey = when {
                    it.filePath != null -> it.filePath!!.hashCode().toString()
                    it.originalUri != null -> it.originalUri!!.hashCode().toString()
                    else -> bookId
                }
                epubMetadataCacheRepository.invalidateCache(cacheKey, "Book removed from library")
            }
            
            bookRepository.deleteBook(bookId)
        }
    }
    
    suspend fun getBookById(bookId: String): com.bsikar.helix.data.model.Book? {
        return bookRepository.getBookById(bookId)
    }
    
    suspend fun getEpubContent(book: com.bsikar.helix.data.model.Book): Result<ParsedEpub?> = withContext(Dispatchers.IO) {
        try {
            if (!book.isImported) {
                Log.d("LibraryManager", "Book ${book.title} is not imported, skipping EPUB loading")
                return@withContext Result.success(null)
            }
            
            Log.d("LibraryManager", "Loading EPUB content for book: ${book.title}")
            Log.d("LibraryManager", "Book filePath: ${book.filePath}")
            Log.d("LibraryManager", "Book originalUri: ${book.originalUri}")
            Log.d("LibraryManager", "Book backupFilePath: ${book.backupFilePath}")
            
            var lastException: Exception? = null
            
            // Strategy 1: Try original file path first (for direct file imports)
            book.filePath?.let { filePath ->
                Log.d("LibraryManager", "Trying Strategy 1: file path $filePath")
                val file = File(filePath)
                if (file.exists()) {
                    Log.d("LibraryManager", "File exists, parsing...")
                    val result = epubParser.parseEpub(file)
                    if (result.isSuccess) {
                        Log.d("LibraryManager", "Strategy 1 successful")
                        return@withContext Result.success(result.getOrNull())
                    } else {
                        lastException = Exception("Strategy 1 failed: ${result.exceptionOrNull()?.message}")
                        Log.w("LibraryManager", "Strategy 1 failed: parseEpub error: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    lastException = Exception("Strategy 1 failed: file does not exist")
                    Log.w("LibraryManager", "Strategy 1 failed: file does not exist")
                }
            }
            
            // Strategy 2: Try original URI (for SAF-based imports) - use new efficient parser
            book.originalUri?.let { uriString ->
                Log.d("LibraryManager", "Trying Strategy 2: URI $uriString")
                val result = epubParser.parseEpubFromUri(context, uriString)
                if (result.isSuccess) {
                    Log.d("LibraryManager", "Strategy 2 successful")
                    return@withContext Result.success(result.getOrNull())
                } else {
                    lastException = Exception("Strategy 2 failed: ${result.exceptionOrNull()?.message}")
                    Log.w("LibraryManager", "Strategy 2 failed: ${result.exceptionOrNull()?.message}")
                }
            }
            
            // Strategy 3: Try backup file path (copied files) - fallback for old imports
            book.backupFilePath?.let { backupPath ->
                Log.d("LibraryManager", "Trying Strategy 3: backup path $backupPath")
                val backupFile = File(backupPath)
                if (backupFile.exists()) {
                    Log.d("LibraryManager", "Backup file exists, parsing...")
                    val result = epubParser.parseEpub(backupFile)
                    if (result.isSuccess) {
                        Log.d("LibraryManager", "Strategy 3 successful")
                        return@withContext Result.success(result.getOrNull())
                    } else {
                        lastException = Exception("Strategy 3 failed: ${result.exceptionOrNull()?.message}")
                        Log.w("LibraryManager", "Strategy 3 failed: parseEpub error: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    lastException = Exception("Strategy 3 failed: backup file does not exist")
                    Log.w("LibraryManager", "Strategy 3 failed: backup file does not exist")
                }
            }
            
            // All strategies failed
            val errorMessage = "All strategies failed for book: ${book.title}. Last error: ${lastException?.message}"
            Log.e("LibraryManager", errorMessage)
            Result.failure(Exception(errorMessage))
            
        } catch (e: Exception) {
            Log.e("LibraryManager", "Exception in getEpubContent for book ${book.title}: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the physical file path for an EPUB book - needed for image rendering
     */
    fun getEpubFilePath(book: com.bsikar.helix.data.model.Book): String? {
        // Strategy 1: Direct file path
        book.filePath?.let { filePath ->
            val file = File(filePath)
            if (file.exists()) return filePath
        }
        
        // Strategy 2: Backup file path  
        book.backupFilePath?.let { backupPath ->
            val file = File(backupPath)
            if (file.exists()) return backupPath
        }
        
        // Strategy 3: For URI-based books, we need to create a temp file when needed
        // This will be handled by the image component on-demand
        return null
    }
    
    
    fun importEpubFileAsync(file: File, onComplete: (Boolean, String) -> Unit) {
        currentImportJob?.cancel() // Cancel any existing import
        currentImportJob = scope.launch {
            val result = importEpubFile(file)
            if (result.isSuccess) {
                onComplete(true, "EPUB imported successfully!")
            } else {
                onComplete(false, "Failed to import EPUB: ${result.exceptionOrNull()?.message}")
            }
            currentImportJob = null
        }
    }
    
    fun addDirectoryToWatchListAsync(directoryUri: String, context: Context, onComplete: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val uri = Uri.parse(directoryUri)
                val documentFile = DocumentFile.fromTreeUri(context, uri)
                
                if (documentFile == null || !documentFile.isDirectory) {
                    onComplete(false, "Invalid directory selected")
                    return@launch
                }
                
                // Check if directory is already watched
                val existingDir = _watchedDirectories.value.find { it.uri == directoryUri }
                if (existingDir != null) {
                    onComplete(false, "Directory is already being watched")
                    return@launch
                }
                
                // Add directory to watched list with requiresRescan = true
                val updatedWatchedDirs = _watchedDirectories.value.toMutableList()
                val watchedDir = WatchedDirectory(
                    path = documentFile.name ?: "Unknown Directory",
                    uri = directoryUri,
                    lastScanned = 0L, // Never scanned yet
                    recursive = true,
                    totalBooks = 0,
                    isUri = true,
                    requiresRescan = true
                )
                
                updatedWatchedDirs.add(watchedDir)
                _watchedDirectories.value = updatedWatchedDirs
                saveLibrary()
                
                onComplete(true, "Directory added to watch list. Rescan to import books.")
            } catch (e: Exception) {
                onComplete(false, "Failed to add directory: ${e.message}")
            }
        }
    }
    
    fun importEpubsFromDirectoryAsync(directoryUri: String, context: Context, onComplete: (Boolean, String) -> Unit) {
        currentImportJob?.cancel() // Cancel any existing import
        currentImportJob = scope.launch {
            val result = importEpubsFromDirectory(directoryUri, context)
            if (result.isSuccess) {
                val importedCount = result.getOrThrow()
                onComplete(true, "Successfully imported $importedCount EPUB files from directory!")
            } else {
                onComplete(false, "Failed to import from directory: ${result.exceptionOrNull()?.message}")
            }
            currentImportJob = null
        }
    }
    
    fun clearLibraryAsync(onComplete: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                clearLibrary()
                onComplete(true, "Library cleared successfully!")
            } catch (e: Exception) {
                onComplete(false, "Failed to clear library: ${e.message}")
            }
        }
    }
    
    fun clearLastImportResult() {
        _lastImportResult.value = null
    }
    
    fun clearLibrary() {
        scope.launch { 
            try {
                Log.d("LibraryManager", "Starting complete library clear...")
                
                // Delete all data from database (this will cascade delete chapters, progress, etc.)
                bookRepository.deleteAllBooks()
                Log.d("LibraryManager", "Deleted all books from database")
                
                // Delete all watched directories
                watchedDirectoryDao.deleteAllWatchedDirectories()
                Log.d("LibraryManager", "Deleted all watched directories")
                
                // Delete all imported file records
                importedFileDao.deleteAllImportedFiles()
                Log.d("LibraryManager", "Deleted all imported files")
                
                // Clean up all cache entries
                epubMetadataCacheRepository.cleanupInvalidCaches()
                Log.d("LibraryManager", "Cleaned up metadata cache")
                
                // Clear in-memory state
                _books.value = emptyList()
                _watchedDirectories.value = emptyList()
                _importedFiles.value = emptyList()
                Log.d("LibraryManager", "Cleared in-memory state")
                
                // Save the empty state
                saveLibrary()
                Log.d("LibraryManager", "Library completely cleared successfully")
                
            } catch (e: Exception) {
                Log.e("LibraryManager", "Failed to clear library: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clean up all copied EPUB files to free storage space
     * This is safe to call since we now use URI-only access
     */
    suspend fun cleanupCopiedFiles(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val epubDir = File(context.filesDir, "epubs")
            var deletedCount = 0
            var freedSpace = 0L
            
            if (epubDir.exists()) {
                epubDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".epub")) {
                        freedSpace += file.length()
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
                
                // Remove the directory if it's empty
                if (epubDir.listFiles()?.isEmpty() == true) {
                    epubDir.delete()
                }
            }
            
            val freedSpaceMB = freedSpace / (1024 * 1024)
            Result.success("Deleted $deletedCount copied files, freed ${freedSpaceMB}MB of storage")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun cleanupCopiedFilesAsync(onComplete: (Boolean, String) -> Unit) {
        scope.launch {
            val result = cleanupCopiedFiles()
            if (result.isSuccess) {
                onComplete(true, result.getOrThrow())
            } else {
                onComplete(false, "Failed to cleanup files: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    suspend fun rescanWatchedDirectories(): Result<List<com.bsikar.helix.data.model.Book>> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val newBooks = mutableListOf<Book>()
            val failures = mutableListOf<ImportFailure>()
            var successCount = 0
            var skippedCount = 0
            val existingFilePaths = _books.value.mapNotNull { it.filePath }.toSet()
            val existingTitles = _books.value.map { "${it.title}:${it.author}" }.toSet()
            
            _watchedDirectories.value.forEach { watchedDir ->
                if (watchedDir.isUri && watchedDir.uri != null) {
                    // Handle URI-based directories
                    val uri = Uri.parse(watchedDir.uri)
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                    
                    if (documentFile != null && documentFile.isDirectory) {
                        val epubFiles = findEpubFilesFromDocumentFile(documentFile)
                        
                        // Don't show progress if no files to scan
                        if (epubFiles.isEmpty()) {
                            return@forEach
                        }
                        
                        _importProgress.value = ImportProgress(0, epubFiles.size, "Scanning ${watchedDir.path}...")
                        
                        epubFiles.forEachIndexed { index, docFile ->
                            val fileName = docFile.name ?: "Unknown file"
                            
                            try {
                                val safeSubProgress = if (epubFiles.size > 0) index.toFloat() / epubFiles.size else 0f
                                _importProgress.value = ImportProgress(index + 1, epubFiles.size, "Checking $fileName", safeSubProgress)
                                
                                // Try to parse directly from URI first, only copy if needed for rescan
                                var parsedEpub: ParsedEpub? = null
                                var permanentFile: File? = null
                                
                                try {
                                    context.contentResolver.openInputStream(docFile.uri)?.use { inputStream ->
                                        // Use streaming parser for rescan to avoid copying huge files
                                        val fileSize = docFile.length()
                                        val parseResult = epubParser.parseEpubMetadataFromStream(inputStream, fileSize, fileName)
                                        
                                        if (parseResult.isSuccess) {
                                            parsedEpub = parseResult.getOrThrow()
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Parsing from URI failed, we'll need to copy the file
                                }
                                
                                // If parsing from URI failed, track as failure
                                if (parsedEpub == null) {
                                    val relativePath = try {
                                        // Try to get relative path from the watched directory
                                        val watchedDirPath = watchedDir.path
                                        val fullPath = docFile.uri.toString()
                                        if (fullPath.contains(watchedDirPath)) {
                                            fullPath.substringAfter(watchedDirPath).removePrefix("/")
                                        } else {
                                            fileName
                                        }
                                    } catch (e: Exception) {
                                        fileName
                                    }
                                    
                                    failures.add(ImportFailure(
                                        fileName = fileName,
                                        filePath = relativePath,
                                        errorMessage = "Failed to parse EPUB file - file may be corrupted or invalid"
                                    ))
                                    Log.w("LibraryManager", "Failed to parse EPUB: $fileName")
                                    return@forEachIndexed
                                }
                                parsedEpub?.let { epub ->
                                    // Calculate checksum for this file
                                    val fileChecksum = FileUtils.calculateFastChecksum(context, docFile.uri.toString())
                                    Log.d("LibraryManager", "Rescan: Calculated checksum for ${epub.metadata.title}: $fileChecksum")
                                    
                                    // Check if this book already exists based on checksum or URI
                                    val existingBook = _books.value.find { existingBook ->
                                        // First check by checksum if available
                                        if (fileChecksum != null && existingBook.fileChecksum != null) {
                                            existingBook.fileChecksum == fileChecksum
                                        } else {
                                            // Fallback to URI comparison for existing books without checksums
                                            existingBook.originalUri == docFile.uri.toString()
                                        }
                                    }
                                    
                                    if (existingBook != null) {
                                        Log.d("LibraryManager", "Found existing book: ${existingBook.title}")
                                        skippedCount++
                                        
                                        // Check if file has changed (different checksum)
                                        if (fileChecksum != null && existingBook.fileChecksum != null && 
                                            existingBook.fileChecksum != fileChecksum) {
                                            Log.d("LibraryManager", "File changed, updating metadata for: ${existingBook.title}")
                                            
                                            // Invalidate cache for the changed file
                                            val cacheKey = docFile.uri.toString().hashCode().toString()
                                            epubMetadataCacheRepository.invalidateCache(cacheKey, "File checksum changed during rescan")
                                            
                                            // File has changed - update only if user hasn't edited metadata
                                            if (!existingBook.userEditedMetadata) {
                                                // Update with new metadata but preserve user data
                                                val updatedBook = existingBook.copy(
                                                    // Update technical metadata from file
                                                    fileChecksum = fileChecksum,
                                                    totalChapters = epub.chapterCount,
                                                    coverImagePath = epub.coverImagePath,
                                                    
                                                    // Only update content metadata if user hasn't edited it
                                                    description = epub.metadata.description ?: existingBook.description,
                                                    publisher = epub.metadata.publisher ?: existingBook.publisher,
                                                    language = epub.metadata.language ?: existingBook.language,
                                                    isbn = epub.metadata.isbn ?: existingBook.isbn,
                                                    publishedDate = epub.metadata.publishedDate ?: existingBook.publishedDate,
                                                    
                                                    // Update original metadata tags but preserve user tags
                                                    originalMetadataTags = epub.metadata.subjects
                                                )
                                                
                                                // Update the existing book in the library
                                                val updatedBooks = _books.value.map { book ->
                                                    if (book.id == existingBook.id) updatedBook else book
                                                }
                                                _books.value = updatedBooks
                                                Log.d("LibraryManager", "Updated existing book: ${updatedBook.title}")
                                            } else {
                                                Log.d("LibraryManager", "User has edited metadata, preserving edits for: ${existingBook.title}")
                                                // Just update the checksum to reflect file change
                                                val updatedBook = existingBook.copy(fileChecksum = fileChecksum)
                                                val updatedBooks = _books.value.map { book ->
                                                    if (book.id == existingBook.id) updatedBook else book
                                                }
                                                _books.value = updatedBooks
                                            }
                                        } else {
                                            Log.d("LibraryManager", "File unchanged, skipping: ${existingBook.title}")
                                        }
                                    } else {
                                        Log.d("LibraryManager", "New book found, importing: ${epub.metadata.title}")
                                        // This is a new book - import it
                                        val result = importEpubFromUri(epub, docFile.uri.toString(), watchedDir.uri!!, false)
                                        if (result.isSuccess) {
                                            newBooks.add(result.getOrThrow())
                                            successCount++
                                            Log.d("LibraryManager", "Successfully added new book to newBooks list: ${epub.metadata.title} (total: ${newBooks.size})")
                                        } else {
                                            val relativePath = try {
                                                val watchedDirPath = watchedDir.path
                                                val fullPath = docFile.uri.toString()
                                                if (fullPath.contains(watchedDirPath)) {
                                                    fullPath.substringAfter(watchedDirPath).removePrefix("/")
                                                } else {
                                                    fileName
                                                }
                                            } catch (e: Exception) {
                                                fileName
                                            }
                                            
                                            failures.add(ImportFailure(
                                                fileName = fileName,
                                                filePath = relativePath,
                                                errorMessage = "Failed to import: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                            ))
                                            Log.e("LibraryManager", "Failed to import new book ${epub.metadata.title}: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                }
                                
                                // No files copied during rescan, nothing to clean up
                            } catch (e: Exception) {
                                Log.e("LibraryManager", "Exception processing file $fileName during rescan: ${e.message}", e)
                                // Continue with other files
                            }
                        }
                    }
                } else {
                    // Handle file-based directories (legacy)
                    val directory = File(watchedDir.path)
                    if (directory.exists() && directory.isDirectory) {
                        val epubFiles = findEpubFiles(directory, watchedDir.recursive)
                        val newFiles = epubFiles.filter { it.absolutePath !in existingFilePaths }
                        
                        // Don't show progress if no new files to scan
                        if (newFiles.isEmpty()) {
                            return@forEach
                        }
                        
                        _importProgress.value = ImportProgress(0, newFiles.size, "Scanning ${directory.name}...")
                        
                        newFiles.forEachIndexed { index, file ->
                            val safeSubProgress = if (newFiles.size > 0) index.toFloat() / newFiles.size else 0f
                            _importProgress.value = ImportProgress(index + 1, newFiles.size, "Importing ${file.name}", safeSubProgress)
                            
                            // Check for duplicates BEFORE importing to prevent adding duplicates to library
                            val parseResult = epubParser.parseEpubMetadataOnly(file)
                            if (parseResult.isSuccess) {
                                val parsedEpub = parseResult.getOrThrow()
                                
                                // Check if this book already exists based on title + author + file path
                                val isDuplicate = _books.value.any { existingBook ->
                                    existingBook.title == parsedEpub.metadata.title && 
                                    existingBook.author == (parsedEpub.metadata.author ?: "Unknown Author")
                                } || _importedFiles.value.any { importedFile ->
                                    importedFile.originalPath == file.absolutePath
                                }
                                
                                if (!isDuplicate) {
                                    val result = importEpubFileForRescan(file, watchedDir.path)
                                    if (result.isSuccess) {
                                        newBooks.add(result.getOrThrow())
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Update last scanned time and clear requiresRescan flag
                val updatedWatchedDirs = _watchedDirectories.value.map { dir ->
                    if (dir.path == watchedDir.path && dir.uri == watchedDir.uri) {
                        dir.copy(
                            lastScanned = System.currentTimeMillis(),
                            requiresRescan = false
                        )
                    } else {
                        dir
                    }
                }
                _watchedDirectories.value = updatedWatchedDirs
            }
            
            _importProgress.value = null
            saveLibrary()
            // Set the import result
            val importResult = ImportResult(
                successCount = successCount,
                skippedCount = skippedCount,
                failedCount = failures.size,
                failures = failures
            )
            _lastImportResult.value = importResult
            
            Log.d("LibraryManager", "Rescan completed. ${importResult.getDisplayMessage()}")
            Log.d("LibraryManager", "Returning ${newBooks.size} new books: ${newBooks.map { it.title }}")
            Result.success(newBooks)
        } catch (e: Exception) {
            _importProgress.value = null
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    fun removeWatchedDirectory(path: String) {
        val updatedWatchedDirs = _watchedDirectories.value.filter { it.path != path }
        _watchedDirectories.value = updatedWatchedDirs
        scope.launch { saveLibrary() }
    }
    
    fun resetPartiallyImportedDirectory(directoryUri: String) {
        // Remove all books that were imported from this directory
        val booksToRemove = _books.value.filter { book ->
            _importedFiles.value.any { importedFile ->
                importedFile.bookId == book.id && 
                (importedFile.sourceUri == directoryUri || importedFile.sourceType == "directory" && importedFile.sourceUri == directoryUri)
            }
        }
        
        // Remove the books
        val updatedBooks = _books.value.filter { book ->
            booksToRemove.none { it.id == book.id }
        }
        _books.value = updatedBooks
        
        // Remove the imported file records
        val updatedImportedFiles = _importedFiles.value.filter { importedFile ->
            !(importedFile.sourceUri == directoryUri && importedFile.sourceType == "directory")
        }
        _importedFiles.value = updatedImportedFiles
        
        // Remove the watched directory
        val updatedWatchedDirs = _watchedDirectories.value.filter { it.uri != directoryUri }
        _watchedDirectories.value = updatedWatchedDirs
        
        scope.launch { saveLibrary() }
    }
    
    fun rescanWatchedDirectoriesAsync(onComplete: (Boolean, String, Int) -> Unit) {
        currentImportJob?.cancel() // Cancel any existing import
        currentImportJob = scope.launch {
            val result = rescanWatchedDirectories()
            if (result.isSuccess) {
                val newBooks = result.getOrThrow()
                onComplete(true, "Scan complete", newBooks.size)
            } else {
                onComplete(false, "Scan failed: ${result.exceptionOrNull()?.message}", 0)
            }
            currentImportJob = null
        }
    }
}

data class ImportProgress(
    val current: Int,
    val total: Int,
    val currentFile: String,
    val subProgress: Float = 0f // 0.0 to 1.0 for within-file progress
) {
    val percentage: Int get() = if (total > 0 && current >= 0) {
        val fileProgress = current.toFloat() / total.toFloat()
        val totalProgress = fileProgress + (subProgress / total.toFloat())
        (totalProgress * 100).toInt().coerceIn(0, 100)
    } else 0
    val isComplete: Boolean get() = current >= total && subProgress >= 1f
    
    // Safer display values that handle edge cases
    val displayCurrent: Int get() = if (total > 0) current.coerceIn(0, total) else 0
    val displayTotal: Int get() = total.coerceAtLeast(0)
}

@Serializable
data class ImportFailure(
    val fileName: String,
    val filePath: String,
    val errorMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ImportResult(
    val successCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val failures: List<ImportFailure> = emptyList()
) {
    fun getDisplayMessage(): String {
        return buildString {
            append("$successCount imported")
            if (skippedCount > 0) append(", $skippedCount skipped")
            if (failedCount > 0) append(", $failedCount failed")
        }
    }
}