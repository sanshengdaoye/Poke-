package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE bookId = :bookId ORDER BY date DESC")
    fun getByBook(bookId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE bookId = :bookId AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByBookAndDateRange(bookId: String, start: Long, end: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE bookId = :bookId AND type = 'INCOME'")
    suspend fun getTotalIncome(bookId: String): Long?

    @Query("SELECT SUM(amount) FROM transactions WHERE bookId = :bookId AND type = 'EXPENSE'")
    suspend fun getTotalExpense(bookId: String): Long?

    @Query("SELECT SUM(amount) FROM transactions WHERE bookId = :bookId AND categoryId = :categoryId AND type = 'EXPENSE'")
    suspend fun getExpenseByCategory(bookId: String, categoryId: String): Long?

    @Query("SELECT COUNT(*) FROM transactions WHERE bookId = :bookId")
    suspend fun getTransactionCount(bookId: String): Int
}
