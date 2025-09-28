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
import com.bsikar.helix.data.repository.AudioChapterRepository
import com.bsikar.helix.data.parser.M4bParser
import com.bsikar.helix.data.model.AudioChapter
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ImportResult
import com.bsikar.helix.data.model.ImportStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Background worker for importing M4B audiobook files without blocking the UI.
 * Provides progress updates via notifications and work progress.
 */
@HiltWorker
class M4bImportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val bookRepository: BookRepository,
    private val audioChapterRepository: AudioChapterRepository,
    private val m4bParser: M4bParser,
    private val importedFileDao: com.bsikar.helix.data.source.dao.ImportedFileDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_FILE_URI = "file_uri"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_IMPORT_ID = "import_id"
        
        private const val NOTIFICATION_CHANNEL_ID = "m4b_import_channel"
        private const val NOTIFICATION_ID = 1002
        private const val PROGRESS_MAX = 100
        
        /**
         * Create work request for M4B import
         */
        fun createWorkRequest(fileUri: String, fileName: String): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(KEY_FILE_URI, fileUri)
                .putString(KEY_FILE_NAME, fileName)
                .putString(KEY_IMPORT_ID, UUID.randomUUID().toString())
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            return OneTimeWorkRequestBuilder<M4bImportWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .addTag("audiobook_import")
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val fileUri = inputData.getString(KEY_FILE_URI) 
            ?: return@withContext Result.failure(createErrorData("File URI is missing"))
        val fileName = inputData.getString(KEY_FILE_NAME) 
            ?: return@withContext Result.failure(createErrorData("File name is missing"))
        val importId = inputData.getString(KEY_IMPORT_ID) 
            ?: return@withContext Result.failure(createErrorData("Import ID is missing"))

        try {
            // Create notification channel
            createNotificationChannel()
            
            // Show initial notification
            showProgressNotification(fileName, 0)
            
            // Update import status to IN_PROGRESS
            updateImportStatus(importId, ImportStatus.IN_PROGRESS, 5, "Starting M4B analysis")
            
            // Copy M4B file to app storage for processing
            val tempFile = copyFileToTemp(Uri.parse(fileUri), fileName) { progress ->
                val progressPercent = (progress * 20).toInt() + 5 // 5-25%
                showProgressNotification(fileName, progressPercent)
            }
            
            try {
                // Parse M4B file
                updateImportStatus(importId, ImportStatus.IN_PROGRESS, 30, "Analyzing audiobook metadata")
                showProgressNotification(fileName, 30)
                
                val parseResult = m4bParser.parseM4b(tempFile) { progress, operation ->
                    val progressPercent = (progress * 40).toInt() + 30 // 30-70%
                    showProgressNotification(fileName, progressPercent)
                }
                
                if (parseResult.isFailure) {
                    val error = parseResult.exceptionOrNull()?.message ?: "Unknown error"
                    setProgress(
                        Data.Builder()
                            .putString("import_id", importId)
                            .putString("status", ImportStatus.FAILED.name)
                            .putInt("progress", 0)
                            .putString("message", "Failed to parse M4B: $error")
                            .build()
                    )
                    return@withContext Result.failure(createErrorData("Failed to parse M4B: $error"))
                }
                
                val parsedM4b = parseResult.getOrThrow()
                
                // Create Book entity
                updateImportStatus(importId, ImportStatus.IN_PROGRESS, 75, "Creating audiobook entry")
                showProgressNotification(fileName, 75)
                
                val bookId = UUID.randomUUID().toString()
                val book = m4bParser.createBookFromParsedM4b(parsedM4b, bookId)
                
                // Save book to database
                bookRepository.insertBook(book)
                
                // Save chapters to database
                updateImportStatus(importId, ImportStatus.IN_PROGRESS, 85, "Saving chapter information")
                showProgressNotification(fileName, 85)
                
                val chaptersWithBookId = parsedM4b.chapters.map { chapter ->
                    chapter.copy(bookId = bookId)
                }
                audioChapterRepository.insertChapters(chaptersWithBookId)
                
                // Record successful import
                updateImportStatus(importId, ImportStatus.IN_PROGRESS, 95, "Finalizing import")
                showProgressNotification(fileName, 95)
                
                val importedFile = com.bsikar.helix.data.source.entities.ImportedFileEntity(
                    path = fileUri,
                    originalPath = parsedM4b.filePath,
                    importedAt = System.currentTimeMillis(),
                    bookId = bookId,
                    sourceType = "individual",
                    sourceUri = fileUri
                )
                importedFileDao.insertImportedFile(importedFile)
                
                // Complete import
                updateImportStatus(importId, ImportStatus.COMPLETED, 100, "Audiobook imported successfully")
                showCompletionNotification(fileName, true)
                
                val successData = Data.Builder()
                    .putString("book_id", bookId)
                    .putString("title", book.title)
                    .putString("author", book.author)
                    .putInt("chapters", parsedM4b.chapters.size)
                    .build()
                
                Result.success(successData)
                
            } finally {
                // Clean up temp file
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
            
        } catch (e: Exception) {
            val errorMessage = "Failed to import M4B: ${e.message}"
            setProgress(
                Data.Builder()
                    .putString("import_id", importId)
                    .putString("status", ImportStatus.FAILED.name)
                    .putInt("progress", 0)
                    .putString("message", errorMessage)
                    .build()
            )
            showCompletionNotification(fileName, false, errorMessage)
            Result.failure(createErrorData(errorMessage))
        }
    }

    private suspend fun copyFileToTemp(
        sourceUri: Uri, 
        fileName: String,
        progressCallback: (Float) -> Unit
    ): File {
        val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}_$fileName")
        
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var totalBytes = 0L
                var bytesRead: Int
                
                // Try to get file size for progress calculation
                val fileSize = try {
                    context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                        pfd.statSize
                    } ?: -1L
                } catch (e: Exception) {
                    -1L
                }
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    
                    if (fileSize > 0) {
                        val progress = totalBytes.toFloat() / fileSize.toFloat()
                        progressCallback(progress)
                    }
                }
            }
        } ?: throw Exception("Failed to open input stream for M4B file")
        
        return tempFile
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "M4B Import",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audiobook import progress notifications"
                setSound(null, null)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(fileName: String, progress: Int) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Importing Audiobook")
            .setContentText(fileName)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(PROGRESS_MAX, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setForegroundAsync(
                ForegroundInfo(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            )
        } else {
            setForegroundAsync(ForegroundInfo(NOTIFICATION_ID, notification))
        }
    }

    private fun showCompletionNotification(fileName: String, success: Boolean, errorMessage: String? = null) {
        val title = if (success) "Audiobook Imported" else "Import Failed"
        val text = if (success) fileName else (errorMessage ?: "Unknown error")
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(if (success) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setSilent(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private suspend fun updateImportStatus(
        importId: String,
        status: ImportStatus,
        progress: Int,
        message: String
    ) {
        // Update work progress
        setProgress(
            Data.Builder()
                .putString("import_id", importId)
                .putString("status", status.name)
                .putInt("progress", progress)
                .putString("message", message)
                .build()
        )
    }

    private fun createErrorData(message: String): Data {
        return Data.Builder()
            .putString("error", message)
            .build()
    }
}