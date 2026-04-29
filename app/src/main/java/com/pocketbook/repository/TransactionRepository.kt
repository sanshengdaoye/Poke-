package com.pocketbook.repository

import com.pocketbook.data.dao.TransactionDao
import com.pocketbook.data.entity.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getTransactionsByBook(bookId: String): Flow<List<Transaction>> =
        transactionDao.getByBook(bookId)

    fun getTransactionsByDateRange(bookId: String, start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getByBookAndDateRange(bookId, start, end)

    suspend fun getTransactionById(id: String): Transaction? = transactionDao.getById(id)

    suspend fun createTransaction(transaction: Transaction) = transactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: Transaction) = transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction)

    suspend fun getTotalIncome(bookId: String): Long = transactionDao.getTotalIncome(bookId) ?: 0L

    suspend fun getTotalExpense(bookId: String): Long = transactionDao.getTotalExpense(bookId) ?: 0L

    suspend fun getTransactionsByBookAndDateRange(bookId: String, start: Long, end: Long): List<Transaction> =
        transactionDao.getByBookAndDateRange(bookId, start, end)

    // --- M3 新增 ---

    suspend fun getExpenseByCategory(bookId: String, start: Long, end: Long): List<TransactionDao.CategoryExpense> =
        transactionDao.getExpenseByCategory(bookId, start, end)

    suspend fun getMonthlyExpenseTrend(bookId: String, start: Long, limit: Int = 6): List<TransactionDao.MonthlyTotal> =
        transactionDao.getMonthlyExpenseTrend(bookId, start, limit)

    suspend fun getMonthlyIncomeTrend(bookId: String, start: Long, limit: Int = 6): List<TransactionDao.MonthlyTotal> =
        transactionDao.getMonthlyIncomeTrend(bookId, start, limit)

    suspend fun getDailyExpenseForCalendar(bookId: String, start: Long, end: Long): List<TransactionDao.DailyTotal> =
        transactionDao.getDailyExpenseForCalendar(bookId, start, end)

    suspend fun getMaxDailyExpense(bookId: String, start: Long, end: Long): TransactionDao.DailyTotal? =
        transactionDao.getMaxDailyExpense(bookId, start, end)

    suspend fun getCategoryExpenseInPeriod(bookId: String, categoryId: String, start: Long, end: Long): Long =
        transactionDao.getCategoryExpenseInPeriod(bookId, categoryId, start, end) ?: 0L
}
