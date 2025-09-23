package com.bsikar.helix.data.repository

import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.source.dao.BookDao
import com.bsikar.helix.data.mapper.toBook
import com.bsikar.helix.data.mapper.toBookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {

    override fun getAllBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>> = 
        bookDao.getAllBooksFlow().map { entities -> entities.map { it.toBook() } }

    override suspend fun getAllBooks(): List<com.bsikar.helix.data.model.Book> = 
        bookDao.getAllBooks().map { it.toBook() }

    override suspend fun getBookById(id: String): com.bsikar.helix.data.model.Book? = 
        bookDao.getBookById(id)?.toBook()

    override suspend fun insertBook(book: com.bsikar.helix.data.model.Book) {
        bookDao.insertBook(book.toBookEntity())
    }

    override suspend fun updateBook(book: com.bsikar.helix.data.model.Book) {
        bookDao.updateBook(book.toBookEntity())
    }

    override suspend fun deleteBook(id: String) {
        bookDao.deleteBookById(id)
    }

    override suspend fun deleteAllBooks() {
        bookDao.deleteAllBooks()
    }

    override fun getReadingBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>> = 
        bookDao.getReadingBooksFlow().map { entities -> entities.map { it.toBook() } }

    override fun getPlanToReadBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>> = 
        bookDao.getPlanToReadBooksFlow().map { entities -> entities.map { it.toBook() } }

    override fun getCompletedBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>> = 
        bookDao.getCompletedBooksFlow().map { entities -> entities.map { it.toBook() } }

    override fun getRecentBooksFlow(): Flow<List<com.bsikar.helix.data.model.Book>> = 
        bookDao.getRecentBooksFlow().map { entities -> entities.map { it.toBook() } }
}