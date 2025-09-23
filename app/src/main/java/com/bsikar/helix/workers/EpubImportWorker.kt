package com.bsikar.helix.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bsikar.helix.R
import com.bsikar.helix.data.repository.BookRepository
import com.bsikar.helix.data.repository.ChapterRepository
import com.bsikar.helix.data.parser.EpubParser
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ImportResult
import com.bsikar.helix.data.model.ImportStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * Background worker for importing EPUB files without blocking the UI.
 * Provides progress updates via notifications and work progress.
 */
@HiltWorker
class EpubImportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val epubParser: EpubParser
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_FILE_URI = "file_uri"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_IMPORT_ID = "import_id"
        
        private const val NOTIFICATION_CHANNEL_ID = "epub_import_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PROGRESS_MAX = 100
        
        /**
         * Create work request for EPUB import
         */
        fun createWorkRequest(fileUri: String, fileName: String): OneTimeWorkRequest {
            val importId = UUID.randomUUID().toString()
            val inputData = workDataOf(
                KEY_FILE_URI to fileUri,
                KEY_FILE_NAME to fileName,
                KEY_IMPORT_ID to importId
            )
            
            return OneTimeWorkRequestBuilder<EpubImportWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .addTag("epub_import")
                .addTag(importId)
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val fileUri = inputData.getString(KEY_FILE_URI) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return@withContext Result.failure()
        val importId = inputData.getString(KEY_IMPORT_ID) ?: return@withContext Result.failure()

        try {
            // Create notification channel
            createNotificationChannel()
            
            // Start foreground service for long-running operation
            setForeground(createForegroundInfo(fileName, 0))
            
            // Parse the EPUB file with progress updates
            val result = importEpubFile(fileUri, fileName, importId)
            
            // Show completion notification
            showCompletionNotification(fileName, result)
            
            // Return success with result data
            val outputData = workDataOf(
                "import_id" to importId,
                "success" to result.success,
                "book_id" to (result.book?.id ?: ""),
                "error_message" to (result.errorMessage ?: "")
            )
            
            Result.success(outputData)
            
        } catch (e: Exception) {
            // Show error notification
            showErrorNotification(fileName, e.message ?: "Unknown error")
            
            val outputData = workDataOf(
                "import_id" to importId,
                "success" to false,
                "error_message" to (e.message ?: "Import failed")
            )
            
            Result.failure(outputData)
        }
    }

    /**
     * Import EPUB file with progress tracking
     */
    private suspend fun importEpubFile(
        fileUriString: String,
        fileName: String,
        importId: String
    ): ImportResult {
        return try {
            val fileUri = Uri.parse(fileUriString)
            
            // Update progress: Starting import
            updateProgress(fileName, 10, "Starting import...")
            
            // Parse EPUB metadata and content
            updateProgress(fileName, 20, "Parsing EPUB file...")
            val result = epubParser.parseEpubFromUri(context, fileUriString)
            val parsedEpub = result.getOrThrow()
            
            // Update progress: Creating book entry
            updateProgress(fileName, 40, "Creating book entry...")
            
            // Create book object
            val book = Book(
                id = UUID.randomUUID().toString(),
                title = parsedEpub.metadata.title,
                author = parsedEpub.metadata.author ?: "Unknown Author",
                coverColor = 0xFF6200EAL, // Default purple color
                description = parsedEpub.metadata.description,
                filePath = fileUriString,
                originalUri = fileUriString,
                coverImagePath = parsedEpub.coverImagePath,
                totalPages = parsedEpub.chapters.size,
                totalChapters = parsedEpub.chapters.size,
                currentPage = 1,
                isImported = true,
                dateAdded = System.currentTimeMillis(),
                language = parsedEpub.metadata.language,
                publisher = parsedEpub.metadata.publisher,
                isbn = parsedEpub.metadata.isbn
            )
            
            // Update progress: Saving to database
            updateProgress(fileName, 60, "Saving to database...")
            
            // Save book to database
            bookRepository.insertBook(book)
            val savedBook = book
            
            // Update progress: Processing chapters
            updateProgress(fileName, 70, "Processing chapters...")
            
            // Store chapters
            val chaptersStored = chapterRepository.storeChaptersFromEpub(book.id, parsedEpub)
            
            // Update progress: Finalizing
            updateProgress(fileName, 90, "Finalizing import...")
            
            // Cache metadata for faster loading
            if (parsedEpub.coverImagePath != null) {
                // Cover image already extracted during parsing
                updateProgress(fileName, 95, "Processing cover image...")
            }
            
            // Complete
            updateProgress(fileName, 100, "Import completed!")
            
            ImportResult(
                success = true,
                book = savedBook,
                importId = importId,
                chaptersImported = parsedEpub.chapters.size,
                status = ImportStatus.COMPLETED
            )
            
        } catch (e: Exception) {
            ImportResult(
                success = false,
                errorMessage = e.message ?: "Unknown error during import",
                importId = importId,
                status = ImportStatus.FAILED
            )
        }
    }

    /**
     * Update progress with notification
     */
    private suspend fun updateProgress(fileName: String, progress: Int, message: String) {
        // Update WorkManager progress
        setProgress(workDataOf(
            "progress" to progress,
            "message" to message,
            "file_name" to fileName
        ))
        
        // Update notification
        setForeground(createForegroundInfo(fileName, progress, message))
    }

    /**
     * Create foreground info for the notification
     */
    private fun createForegroundInfo(
        fileName: String, 
        progress: Int, 
        message: String = "Importing EPUB..."
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Importing EPUB")
            .setContentText("$fileName - $message")
            .setSmallIcon(R.drawable.ic_book) // Make sure this icon exists
            .setProgress(PROGRESS_MAX, progress, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Show completion notification
     */
    private fun showCompletionNotification(fileName: String, result: ImportResult) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = if (result.success) {
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Import Completed")
                .setContentText("Successfully imported: $fileName")
                .setSmallIcon(R.drawable.ic_check) // Make sure this icon exists
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()
        } else {
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Import Failed")
                .setContentText("Failed to import: $fileName")
                .setSmallIcon(R.drawable.ic_error) // Make sure this icon exists
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .build()
        }
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Show error notification
     */
    private fun showErrorNotification(fileName: String, errorMessage: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Import Error")
            .setContentText("Error importing $fileName: $errorMessage")
            .setSmallIcon(R.drawable.ic_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "EPUB Import",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for EPUB file imports"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Calculate estimated reading time based on content length
     */
    private fun calculateReadingTime(contentLength: Int): Int {
        // Average reading speed: 200-300 words per minute
        // Assuming average word length of 5 characters
        val estimatedWords = contentLength / 5
        return (estimatedWords / 250).coerceAtLeast(1) // 250 WPM average
    }
}

/**
 * Data classes for import results
 */
data class ImportResult(
    val success: Boolean,
    val book: Book? = null,
    val errorMessage: String? = null,
    val importId: String,
    val chaptersImported: Int = 0,
    val status: ImportStatus = ImportStatus.IN_PROGRESS
)

enum class ImportStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}