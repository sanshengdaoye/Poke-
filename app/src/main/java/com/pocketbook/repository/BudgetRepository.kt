package com.pocketbook.repository

import com.pocketbook.data.dao.BudgetDao
import com.pocketbook.data.entity.Budget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    fun getActiveBudgets(bookId: String): Flow<List<Budget>> =
        budgetDao.getActiveByBook(bookId)

    suspend fun getBudgetById(id: String): Budget? = budgetDao.getById(id)

    suspend fun getBudgetsByBook(bookId: String): List<Budget> = budgetDao.getByBook(bookId)

    suspend fun createBudget(budget: Budget) = budgetDao.insert(budget)

    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    // --- M3 新增 ---

    suspend fun getBudgetByCategory(bookId: String, categoryId: String): Budget? =
        budgetDao.getByCategory(bookId, categoryId)
}
