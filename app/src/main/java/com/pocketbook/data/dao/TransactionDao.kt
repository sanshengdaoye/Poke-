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

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE bookId = :bookId AND type = 'INCOME'")
    suspend fun getTotalIncome(bookId: String): Long?

    @Query("SELECT SUM(amount) FROM transactions WHERE bookId = :bookId AND type = 'EXPENSE'")
    suspend fun getTotalExpense(bookId: String): Long?

    // --- M3 新增 ---

    @Query("""
        SELECT SUM(amount) as total, categoryId 
        FROM transactions 
        WHERE bookId = :bookId AND type = 'EXPENSE' AND date BETWEEN :start AND :end 
        GROUP BY categoryId
        ORDER BY total DESC
    """)
    suspend fun getExpenseByCategory(bookId: String, start: Long, end: Long): List<CategoryExpense>

    @Query("""
        SELECT SUM(amount) as total, strftime('%Y-%m', date/1000, 'unixepoch') as month
        FROM transactions
        WHERE bookId = :bookId AND type = 'EXPENSE' AND date >= :start
        GROUP BY month
        ORDER BY month DESC
        LIMIT :limit
    """)
    suspend fun getMonthlyExpenseTrend(bookId: String, start: Long, limit: Int): List<MonthlyTotal>

    @Query("""
        SELECT SUM(amount) as total, strftime('%Y-%m', date/1000, 'unixepoch') as month
        FROM transactions
        WHERE bookId = :bookId AND type = 'INCOME' AND date >= :start
        GROUP BY month
        ORDER BY month DESC
        LIMIT :limit
    """)
    suspend fun getMonthlyIncomeTrend(bookId: String, start: Long, limit: Int): List<MonthlyTotal>

    @Query("""
        SELECT SUM(amount) as total, strftime('%Y-%m-%d', date/1000, 'unixepoch') as day
        FROM transactions
        WHERE bookId = :bookId AND type = 'EXPENSE' AND date BETWEEN :start AND :end
        GROUP BY day
        ORDER BY day
    """)
    suspend fun getDailyExpenseForCalendar(bookId: String, start: Long, end: Long): List<DailyTotal>

    @Query("""
        SELECT SUM(amount) as total, strftime('%Y-%m-%d', date/1000, 'unixepoch') as day
        FROM transactions
        WHERE bookId = :bookId AND type = 'EXPENSE' AND date BETWEEN :start AND :end
        GROUP BY day
        ORDER BY total DESC
        LIMIT 1
    """)
    suspend fun getMaxDailyExpense(bookId: String, start: Long, end: Long): DailyTotal?

    @Query("""
        SELECT SUM(amount) as total FROM transactions
        WHERE bookId = :bookId AND type = 'EXPENSE' AND categoryId = :categoryId
        AND date BETWEEN :start AND :end
    """)
    suspend fun getCategoryExpenseInPeriod(bookId: String, categoryId: String, start: Long, end: Long): Long?

    data class CategoryExpense(
        val total: Long,
        val categoryId: String?
    )

    data class MonthlyTotal(
        val total: Long,
        val month: String
    )

    data class DailyTotal(
        val total: Long,
        val day: String
    )
}
