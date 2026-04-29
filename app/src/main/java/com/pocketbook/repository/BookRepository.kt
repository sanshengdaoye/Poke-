package com.pocketbook.repository

import com.pocketbook.data.dao.BookDao
import com.pocketbook.data.entity.Book
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao
) {
    fun getAllBooks(): Flow<List<Book>> = bookDao.getAll()

    suspend fun getDefaultBook(): Book? = bookDao.getDefault()

    suspend fun getBookById(id: String): Book? = bookDao.getById(id)

    suspend fun createBook(book: Book) = bookDao.insert(book)

    suspend fun updateBook(book: Book) = bookDao.update(book)

    suspend fun deleteBook(book: Book) = bookDao.delete(book)

    suspend fun setDefaultBook(id: String) {
        bookDao.clearDefault()
        bookDao.setDefault(id)
    }
}
