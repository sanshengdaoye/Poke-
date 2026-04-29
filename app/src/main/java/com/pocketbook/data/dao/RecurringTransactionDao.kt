package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.RecurringTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextTriggerDate ASC")
    fun getActive(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextTriggerDate <= :now ORDER BY nextTriggerDate ASC")
    suspend fun getDueBefore(now: Long): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: String): RecurringTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringTransaction)

    @Update
    suspend fun update(recurring: RecurringTransaction)

    @Delete
    suspend fun delete(recurring: RecurringTransaction)

    @Query("UPDATE recurring_transactions SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)
}
