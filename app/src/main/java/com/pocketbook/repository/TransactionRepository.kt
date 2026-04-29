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

    suspend fun getExpenseByCategory(bookId: String, categoryId: String): Long =
        transactionDao.getExpenseByCategory(bookId, categoryId) ?: 0L

    suspend fun getTransactionCount(bookId: String): Int = transactionDao.getTransactionCount(bookId)
}
