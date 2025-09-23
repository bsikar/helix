package com.bsikar.helix.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.bsikar.helix.workers.EpubImportWorker
import com.bsikar.helix.data.repository.BookRepository
import com.bsikar.helix.data.repository.ChapterRepository
import com.bsikar.helix.data.parser.EpubParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom WorkerFactory for Hilt dependency injection in Workers
 */
@Singleton
class HiltWorkerFactory @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val epubParser: EpubParser
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            EpubImportWorker::class.java.name -> {
                EpubImportWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    bookRepository = bookRepository,
                    chapterRepository = chapterRepository,
                    epubParser = epubParser
                )
            }
            else -> null
        }
    }
}