package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE bookId = :bookId AND isActive = 1")
    fun getActiveByBook(bookId: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    // --- M3 新增 ---

    @Query("SELECT * FROM budgets WHERE bookId = :bookId AND isActive = 1")
    suspend fun getByBook(bookId: String): List<Budget>

    @Query("SELECT * FROM budgets WHERE bookId = :bookId AND categoryId = :categoryId AND isActive = 1 LIMIT 1")
    suspend fun getByCategory(bookId: String, categoryId: String): Budget?
}
