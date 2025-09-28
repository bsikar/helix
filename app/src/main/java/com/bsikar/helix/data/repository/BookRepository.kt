package com.bsikar.helix.data.repository

import com.bsikar.helix.data.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>>
    suspend fun getAllBooks(): List<com.bsikar.helix.data.model.Book>
    suspend fun getBookById(id: String): com.bsikar.helix.data.model.Book?
    suspend fun insertBook(book: com.bsikar.helix.data.model.Book)
    suspend fun updateBook(book: com.bsikar.helix.data.model.Book)
    suspend fun deleteBook(id: String)
    suspend fun deleteAllBooks()
    fun getReadingBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>>
    fun getPlanToReadBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>>
    fun getCompletedBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>>
    fun getRecentBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>>
    suspend fun updatePlaybackPosition(bookId: String, positionMs: Long, playbackSpeed: Float)
}