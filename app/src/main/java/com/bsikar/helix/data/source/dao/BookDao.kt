package com.bsikar.helix.data.source.dao

import androidx.room.*
import com.bsikar.helix.data.source.entities.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    
    @Query("SELECT * FROM books")
    fun getAllBooksFlow(): Flow<List<BookEntity>>
    
    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>
    
    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?
    
    @Query("SELECT * FROM books WHERE progress > 0 AND progress < 1 ORDER BY lastReadTimestamp DESC")
    fun getReadingBooksFlow(): Flow<List<BookEntity>>
    
    @Query("SELECT * FROM books WHERE progress = 0 ORDER BY title ASC")
    fun getPlanToReadBooksFlow(): Flow<List<BookEntity>>
    
    @Query("SELECT * FROM books WHERE progress >= 1 ORDER BY title ASC")
    fun getCompletedBooksFlow(): Flow<List<BookEntity>>
    
    @Query("SELECT * FROM books WHERE lastReadTimestamp > 0 ORDER BY lastReadTimestamp DESC")
    fun getRecentBooksFlow(): Flow<List<BookEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)
    
    @Update
    suspend fun updateBook(book: BookEntity)
    
    @Delete
    suspend fun deleteBook(book: BookEntity)
    
    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: String)
    
    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()
}