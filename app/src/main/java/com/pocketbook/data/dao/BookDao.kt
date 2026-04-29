package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): Book?

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<Book>)

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("UPDATE books SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE books SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)
}
