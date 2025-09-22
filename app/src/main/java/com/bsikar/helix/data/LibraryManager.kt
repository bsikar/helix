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

@Serializable
data class WatchedDirectory(
    val path: String,
    val uri: String? = null, // Store both URI and file path for compatibility
    val lastScanned: Long = System.currentTimeMillis(),
    val recursive: Boolean = true,
    val totalBooks: Int = 0,
    val isUri: Boolean = uri != null // Flag to know which type to use
)

@Serializable 
data class ImportedFile(
    val path: String,
    val importedAt: Long = System.currentTimeMillis(),
    val bookId: String? = null
)

class LibraryManager(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private val epubParser = EpubParser(context)
    
    private val _books = mutableStateOf<List<Book>>(emptyList())
    val books: State<List<Book>> = _books
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _importProgress = mutableStateOf<ImportProgress?>(null)
    val importProgress: State<ImportProgress?> = _importProgress
    
    private val _watchedDirectories = mutableStateOf<List<WatchedDirectory>>(emptyList())
    val watchedDirectories: State<List<WatchedDirectory>> = _watchedDirectories
    
    private val _importedFiles = mutableStateOf<List<ImportedFile>>(emptyList())
    val importedFiles: State<List<ImportedFile>> = _importedFiles
    
    private var currentImportJob: Job? = null
    
    fun cancelCurrentImport() {
        currentImportJob?.cancel()
        currentImportJob = null
        _isLoading.value = false
        _importProgress.value = null
    }
    
    init {
        loadLibrary()
    }
    
    private fun loadLibrary() {
        scope.launch {
            try {
                val libraryJson = preferencesManager.getLibraryData()
                if (libraryJson.isNotEmpty()) {
                    val loadedBooks = Json.decodeFromString<List<Book>>(libraryJson)
                    _books.value = loadedBooks
                }
                
                // Load watched directories
                val watchedDirsJson = preferencesManager.getWatchedDirectories()
                if (watchedDirsJson.isNotEmpty()) {
                    val loadedDirs = Json.decodeFromString<List<WatchedDirectory>>(watchedDirsJson)
                    _watchedDirectories.value = loadedDirs
                }
                
                // Load imported files
                val importedFilesJson = preferencesManager.getImportedFiles()
                if (importedFilesJson.isNotEmpty()) {
                    val loadedFiles = Json.decodeFromString<List<ImportedFile>>(importedFilesJson)
                    _importedFiles.value = loadedFiles
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _books.value = emptyList()
                _watchedDirectories.value = emptyList()
                _importedFiles.value = emptyList()
            }
        }
    }
    
    private suspend fun saveLibrary() {
        try {
            val libraryJson = Json.encodeToString(_books.value)
            preferencesManager.saveLibraryData(libraryJson)
            
            val watchedDirsJson = Json.encodeToString(_watchedDirectories.value)
            preferencesManager.saveWatchedDirectories(watchedDirsJson)
            
            val importedFilesJson = Json.encodeToString(_importedFiles.value)
            preferencesManager.saveImportedFiles(importedFilesJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun importEpubFile(file: File): Result<Book> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            
            val parseResult = epubParser.parseEpub(file)
            if (parseResult.isFailure) {
                return@withContext Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to parse EPUB"))
            }
            
            val parsedEpub = parseResult.getOrThrow()
            
            // Create Book from parsed EPUB
            val book = Book(
                id = UUID.randomUUID().toString(),
                title = parsedEpub.metadata.title,
                author = parsedEpub.metadata.author ?: "Unknown Author",
                coverColor = generateCoverColor(parsedEpub.metadata.title),
                filePath = file.absolutePath,
                fileSize = file.length(),
                totalChapters = parsedEpub.totalChapters,
                description = parsedEpub.metadata.description,
                publisher = parsedEpub.metadata.publisher,
                language = parsedEpub.metadata.language,
                isbn = parsedEpub.metadata.isbn,
                publishedDate = parsedEpub.metadata.publishedDate,
                coverImagePath = parsedEpub.coverImagePath,
                isImported = true,
                originalMetadataTags = parsedEpub.metadata.subjects
            )
            
            // Add to library
            val updatedBooks = _books.value.toMutableList()
            updatedBooks.add(book)
            _books.value = updatedBooks
            
            // Track imported file
            val updatedImportedFiles = _importedFiles.value.toMutableList()
            updatedImportedFiles.add(ImportedFile(
                path = file.absolutePath,
                importedAt = System.currentTimeMillis(),
                bookId = book.id
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
    
    suspend fun importEpubsFromDirectory(directory: File, recursive: Boolean = true): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val importedBooks = mutableListOf<Book>()
            val epubFiles = findEpubFiles(directory, recursive)
            
            _importProgress.value = ImportProgress(0, epubFiles.size, "Starting import...")
            
            epubFiles.forEachIndexed { index, file ->
                // Step 1: Starting file processing (0% of this file's progress)
                _importProgress.value = ImportProgress(index, epubFiles.size, "Starting ${file.name}", 0.0f)
                
                // Step 2: Parsing EPUB (33% of this file's progress)
                _importProgress.value = ImportProgress(index, epubFiles.size, "Parsing ${file.name}", 0.33f)
                
                // Step 3: Processing content (66% of this file's progress)
                _importProgress.value = ImportProgress(index, epubFiles.size, "Processing ${file.name}", 0.66f)
                
                val result = importEpubFile(file)
                if (result.isSuccess) {
                    importedBooks.add(result.getOrThrow())
                }
                
                // Step 4: Completed file (100% of this file's progress)
                _importProgress.value = ImportProgress(index, epubFiles.size, "Completed ${file.name}", 1.0f)
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
            _isLoading.value = true
            val uri = Uri.parse(directoryUri)
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            
            if (documentFile == null || !documentFile.isDirectory) {
                return@withContext Result.failure(Exception("Invalid directory"))
            }
            
            val epubFiles = findEpubFilesFromDocumentFile(documentFile)
            
            // Filter out files that are already imported (duplicate detection)
            val existingFilePaths = _books.value.mapNotNull { it.filePath }.toSet()
            val existingTitles = _books.value.map { "${it.title}:${it.author}" }.toSet()
            
            val newFiles = epubFiles.filter { docFile ->
                // For URIs, we can't easily check filePath, so we'll check during import
                true // We'll do duplicate check during individual file processing
            }
            
            _importProgress.value = ImportProgress(0, newFiles.size, "Starting import...")
            
            var importedCount = 0
            var skippedCount = 0
            epubFiles.forEachIndexed { index, docFile ->
                val fileName = docFile.name ?: "Unknown file"
                
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
                    
                    // File copying with progress updates
                    val tempFile = File(context.cacheDir, "temp_epub_${System.currentTimeMillis()}.epub")
                    
                    steps.take(6).forEach { (progress, status) ->
                        _importProgress.value = ImportProgress(index, epubFiles.size, "$status $fileName", progress)
                        kotlinx.coroutines.delay(10) // Small delay for smooth animation
                    }
                    
                    context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // EPUB processing with progress updates
                    steps.drop(6).take(13).forEach { (progress, status) ->
                        _importProgress.value = ImportProgress(index, epubFiles.size, "$status $fileName", progress)
                        kotlinx.coroutines.delay(10)
                    }
                    
                    val result = importEpubFile(tempFile)
                    
                    if (result.isSuccess) {
                        val book = result.getOrThrow()
                        val bookKey = "${book.title}:${book.author}"
                        
                        // Check for duplicates before adding
                        if (bookKey in existingTitles) {
                            skippedCount++
                            _importProgress.value = ImportProgress(index, epubFiles.size, "Skipped duplicate: $fileName", 0.95f)
                        } else {
                            importedCount++
                            
                            // Finalization with progress updates
                            steps.drop(19).forEach { (progress, status) ->
                                _importProgress.value = ImportProgress(index, epubFiles.size, "$status $fileName", progress)
                                kotlinx.coroutines.delay(10)
                            }
                        }
                    } else {
                        // Finalization for failed imports
                        _importProgress.value = ImportProgress(index, epubFiles.size, "Failed: $fileName", 1.0f)
                    }
                    
                    tempFile.delete()
                } catch (e: Exception) {
                    _importProgress.value = ImportProgress(index, epubFiles.size, "Failed: $fileName", 1.0f)
                }
            }
            
            // Add watched directory (check for duplicates)
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
    
    private fun generateCoverColor(title: String): Long {
        val colors = listOf(
            Color(0xFF6B73FF), Color(0xFF9B59B6), Color(0xFF3498DB),
            Color(0xFF1ABC9C), Color(0xFF2ECC71), Color(0xFFF39C12),
            Color(0xFFE74C3C), Color(0xFF34495E), Color(0xFFE67E22)
        )
        
        val hash = title.hashCode()
        val colorIndex = kotlin.math.abs(hash) % colors.size
        return colors[colorIndex].value.toLong()
    }
    
    fun updateBookProgress(bookId: String, chapterNumber: Int, pageNumber: Int, scrollPosition: Int) {
        val updatedBooks = _books.value.map { book ->
            if (book.id == bookId) {
                val totalPages = book.totalPages
                val currentPage = pageNumber
                val progress = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
                
                book.copy(
                    currentChapter = chapterNumber,
                    currentPage = currentPage,
                    scrollPosition = scrollPosition,
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
        val updatedBooks = _books.value.map { book ->
            if (book.id == bookId) {
                book.copy(tags = tags)
            } else {
                book
            }
        }
        
        _books.value = updatedBooks
        scope.launch { saveLibrary() }
    }
    
    fun removeBook(bookId: String) {
        val updatedBooks = _books.value.filter { it.id != bookId }
        _books.value = updatedBooks
        scope.launch { saveLibrary() }
    }
    
    fun getBookById(bookId: String): Book? {
        return _books.value.find { it.id == bookId }
    }
    
    suspend fun getEpubContent(book: Book): ParsedEpub? {
        if (!book.isImported || book.filePath == null) return null
        
        return try {
            val file = File(book.filePath)
            if (file.exists()) {
                epubParser.parseEpub(file).getOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun addFakeData() {
        val fakeBooks = listOf(
            // Plan to Read Books (progress = 0) - Most books start here
            Book(
                title = "Akane-Banashi",
                author = "Yuki Suenaga",
                coverColor = 0xFF4169E1,
                progress = 0f,
                tags = listOf("shounen", "drama", "slice-of-life", "ongoing", "manga"),
                originalMetadataTags = listOf("Shounen", "Drama", "Traditional Arts", "Rakugo"),
                isImported = false
            ),
            Book(
                title = "Dandadan",
                author = "Yukinobu Tatsu",
                coverColor = 0xFF32CD32,
                progress = 0f,
                tags = listOf("shounen", "supernatural", "comedy", "romance", "ongoing", "manga"),
                originalMetadataTags = listOf("Supernatural", "Comedy", "School", "Aliens", "Ghosts"),
                isImported = false
            ),
            Book(
                title = "Jujutsu Kaisen",
                author = "Gege Akutami",
                coverColor = 0xFF8A2BE2,
                progress = 0f,
                tags = listOf("shounen", "action", "supernatural", "completed", "manga"),
                originalMetadataTags = listOf("Supernatural", "School", "Action", "Curses"),
                isImported = false
            ),
            Book(
                title = "Tokyo Ghoul",
                author = "Sui Ishida",
                coverColor = 0xFF696969,
                progress = 0f,
                tags = listOf("seinen", "supernatural", "dark", "completed", "manga"),
                originalMetadataTags = listOf("Dark Fantasy", "Supernatural", "Horror", "Tragedy"),
                isImported = false
            ),
            Book(
                title = "Monster",
                author = "Naoki Urasawa",
                coverColor = 0xFF8B0000,
                progress = 0f,
                tags = listOf("seinen", "thriller", "psychological", "completed", "manga"),
                originalMetadataTags = listOf("Psychological", "Thriller", "Mystery", "Medical"),
                isImported = false
            ),
            Book(
                title = "20th Century Boys",
                author = "Naoki Urasawa",
                coverColor = 0xFF4682B4,
                progress = 0f,
                tags = listOf("seinen", "mystery", "thriller", "completed", "manga"),
                originalMetadataTags = listOf("Mystery", "Sci-Fi", "Thriller", "Friendship"),
                isImported = false
            ),
            Book(
                title = "Pluto",
                author = "Naoki Urasawa",
                coverColor = 0xFF2F4F4F,
                progress = 0f,
                tags = listOf("seinen", "sci-fi", "mystery", "completed", "manga"),
                originalMetadataTags = listOf("Sci-Fi", "Mystery", "Robots", "Philosophy"),
                isImported = false
            ),
            
            // Currently Reading Books (0 < progress < 1)
            Book(
                title = "Chainsaw Man",
                author = "Tatsuki Fujimoto",
                coverColor = 0xFFDC143C,
                progress = 0.45f,
                lastReadTimestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000), // 2 days ago
                tags = listOf("shounen", "action", "supernatural", "ongoing", "manga"),
                originalMetadataTags = listOf("Action", "Supernatural", "Gore", "Dark Comedy"),
                isImported = false
            ),
            Book(
                title = "One Piece",
                author = "Eiichiro Oda",
                coverColor = 0xFFFF6347,
                progress = 0.72f,
                lastReadTimestamp = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000), // 5 days ago
                tags = listOf("shounen", "adventure", "action", "ongoing", "manga"),
                originalMetadataTags = listOf("Adventure", "Friendship", "Pirates", "Comedy"),
                isImported = false
            ),
            Book(
                title = "Berserk",
                author = "Kentaro Miura",
                coverColor = 0xFF800080,
                progress = 0.38f,
                lastReadTimestamp = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                tags = listOf("seinen", "dark-fantasy", "action", "ongoing", "manga"),
                originalMetadataTags = listOf("Dark Fantasy", "Medieval", "Action", "Mature"),
                isImported = false
            ),
            Book(
                title = "Vagabond",
                author = "Takehiko Inoue",
                coverColor = 0xFF8B4513,
                progress = 0.61f,
                lastReadTimestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // 1 week ago
                tags = listOf("seinen", "historical", "action", "martial-arts", "hiatus", "manga"),
                originalMetadataTags = listOf("Historical", "Samurai", "Martial Arts", "Philosophy"),
                isImported = false
            ),
            Book(
                title = "Attack on Titan",
                author = "Hajime Isayama",
                coverColor = 0xFF8FBC8F,
                progress = 0.89f,
                lastReadTimestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                tags = listOf("shounen", "action", "drama", "completed", "manga"),
                originalMetadataTags = listOf("Action", "Drama", "Military", "Titans"),
                isImported = false
            ),
            
            // Completed Books (progress = 1.0)
            Book(
                title = "Death Note",
                author = "Tsugumi Ohba",
                coverColor = 0xFF000000,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000), // 2 weeks ago
                tags = listOf("shounen", "psychological", "thriller", "completed", "manga"),
                originalMetadataTags = listOf("Psychological", "Supernatural", "Crime", "Justice"),
                isImported = false
            ),
            Book(
                title = "Fullmetal Alchemist",
                author = "Hiromu Arakawa",
                coverColor = 0xFFB8860B,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000), // 1 month ago
                tags = listOf("shounen", "adventure", "action", "completed", "manga"),
                originalMetadataTags = listOf("Adventure", "Military", "Alchemy", "Brotherhood"),
                isImported = false
            ),
            Book(
                title = "Hunter x Hunter",
                author = "Yoshihiro Togashi",
                coverColor = 0xFF228B22,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (21 * 24 * 60 * 60 * 1000), // 3 weeks ago
                tags = listOf("shounen", "adventure", "action", "hiatus", "manga"),
                originalMetadataTags = listOf("Adventure", "Supernatural", "Strategic", "Complex"),
                isImported = false
            ),
            Book(
                title = "Spirited Away",
                author = "Hayao Miyazaki",
                coverColor = 0xFF9370DB,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (45 * 24 * 60 * 60 * 1000), // 1.5 months ago
                tags = listOf("family", "fantasy", "adventure", "completed", "manga"),
                originalMetadataTags = listOf("Fantasy", "Family", "Magic", "Coming of Age"),
                isImported = false
            ),
            Book(
                title = "Your Name",
                author = "Makoto Shinkai",
                coverColor = 0xFFFF69B4,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (60 * 24 * 60 * 60 * 1000), // 2 months ago
                tags = listOf("romance", "supernatural", "drama", "completed", "manga"),
                originalMetadataTags = listOf("Romance", "Time Travel", "Drama", "Slice of Life"),
                isImported = false
            ),
            Book(
                title = "Akira",
                author = "Katsuhiro Otomo",
                coverColor = 0xFF483D8B,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000), // 3 months ago
                tags = listOf("seinen", "sci-fi", "action", "completed", "manga"),
                originalMetadataTags = listOf("Cyberpunk", "Post-Apocalyptic", "Psychic Powers", "Classic"),
                isImported = false
            ),
            Book(
                title = "Ghost in the Shell",
                author = "Masamune Shirow",
                coverColor = 0xFF708090,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (120 * 24 * 60 * 60 * 1000), // 4 months ago
                tags = listOf("seinen", "sci-fi", "action", "completed", "manga"),
                originalMetadataTags = listOf("Cyberpunk", "Philosophy", "AI", "Technology"),
                isImported = false
            ),
            
            // Additional popular titles to round out the collection
            Book(
                title = "Clockwork Planet",
                author = "Yuu Kamiya",
                coverColor = 0xFFFFD700,
                progress = 0.3f,
                lastReadTimestamp = System.currentTimeMillis() - (4 * 24 * 60 * 60 * 1000), // 4 days ago
                tags = listOf("light-novel", "sci-fi", "action", "ongoing"),
                originalMetadataTags = listOf("Science Fiction", "Clockwork", "Adventure"),
                isImported = false
            ),
            Book(
                title = "No Game No Life",
                author = "Yuu Kamiya",
                coverColor = 0xFF9B59B6,
                progress = 0.7f,
                lastReadTimestamp = System.currentTimeMillis() - (6 * 24 * 60 * 60 * 1000), // 6 days ago
                tags = listOf("light-novel", "comedy", "fantasy", "ongoing"),
                originalMetadataTags = listOf("Game", "Fantasy", "Comedy", "Isekai"),
                isImported = false
            ),
            Book(
                title = "Overlord",
                author = "Kugane Maruyama",
                coverColor = 0xFF34495E,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000), // 2+ weeks ago
                tags = listOf("light-novel", "fantasy", "action", "completed"),
                originalMetadataTags = listOf("Dark Fantasy", "Isekai", "RPG", "Undead"),
                isImported = false
            )
        )
        
        val updatedBooks = _books.value.toMutableList()
        updatedBooks.addAll(fakeBooks)
        _books.value = updatedBooks
        
        scope.launch { saveLibrary() }
    }
    
    fun addFakeDataAsync(onComplete: (Boolean, String) -> Unit) {
        currentImportJob?.cancel() // Cancel any existing import
        currentImportJob = scope.launch {
            try {
                addFakeData()
                onComplete(true, "Fake data imported successfully!")
            } catch (e: Exception) {
                onComplete(false, "Failed to import fake data: ${e.message}")
            }
            currentImportJob = null
        }
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
    
    fun clearLibrary() {
        _books.value = emptyList()
        _watchedDirectories.value = emptyList()
        _importedFiles.value = emptyList()
        scope.launch { saveLibrary() }
    }
    
    suspend fun rescanWatchedDirectories(): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val newBooks = mutableListOf<Book>()
            val existingFilePaths = _books.value.mapNotNull { it.filePath }.toSet()
            val existingTitles = _books.value.map { "${it.title}:${it.author}" }.toSet()
            
            _watchedDirectories.value.forEach { watchedDir ->
                if (watchedDir.isUri && watchedDir.uri != null) {
                    // Handle URI-based directories
                    _importProgress.value = ImportProgress(0, 0, "Scanning ${watchedDir.path}...")
                    
                    val uri = Uri.parse(watchedDir.uri)
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                    
                    if (documentFile != null && documentFile.isDirectory) {
                        val epubFiles = findEpubFilesFromDocumentFile(documentFile)
                        
                        epubFiles.forEachIndexed { index, docFile ->
                            val fileName = docFile.name ?: "Unknown file"
                            
                            try {
                                _importProgress.value = ImportProgress(index, epubFiles.size, "Checking $fileName", index.toFloat() / epubFiles.size)
                                
                                val tempFile = File(context.cacheDir, "temp_rescan_${System.currentTimeMillis()}.epub")
                                context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                                    tempFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                
                                val result = importEpubFile(tempFile)
                                if (result.isSuccess) {
                                    val book = result.getOrThrow()
                                    val bookKey = "${book.title}:${book.author}"
                                    
                                    if (bookKey !in existingTitles) {
                                        newBooks.add(book)
                                    }
                                }
                                
                                tempFile.delete()
                            } catch (e: Exception) {
                                // Continue with other files
                            }
                        }
                    }
                } else {
                    // Handle file-based directories (legacy)
                    val directory = File(watchedDir.path)
                    if (directory.exists() && directory.isDirectory) {
                        _importProgress.value = ImportProgress(0, 0, "Scanning ${directory.name}...")
                        
                        val epubFiles = findEpubFiles(directory, watchedDir.recursive)
                        val newFiles = epubFiles.filter { it.absolutePath !in existingFilePaths }
                        
                        newFiles.forEachIndexed { index, file ->
                            _importProgress.value = ImportProgress(index, newFiles.size, "Importing ${file.name}", index.toFloat() / newFiles.size)
                            
                            val result = importEpubFile(file)
                            if (result.isSuccess) {
                                val book = result.getOrThrow()
                                val bookKey = "${book.title}:${book.author}"
                                
                                if (bookKey !in existingTitles) {
                                    newBooks.add(book)
                                }
                            }
                        }
                    }
                }
                
                // Update last scanned time
                val updatedWatchedDirs = _watchedDirectories.value.map { dir ->
                    if (dir.path == watchedDir.path && dir.uri == watchedDir.uri) {
                        dir.copy(lastScanned = System.currentTimeMillis())
                    } else {
                        dir
                    }
                }
                _watchedDirectories.value = updatedWatchedDirs
            }
            
            _importProgress.value = null
            saveLibrary()
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
    val percentage: Int get() = if (total > 0) {
        val fileProgress = current.toFloat() / total
        val totalProgress = fileProgress + (subProgress / total)
        (totalProgress * 100).toInt().coerceIn(0, 100)
    } else 0
    val isComplete: Boolean get() = current >= total && subProgress >= 1f
}