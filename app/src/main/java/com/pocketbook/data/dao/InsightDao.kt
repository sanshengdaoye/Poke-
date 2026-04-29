package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.Insight
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights WHERE isDismissed = 0 ORDER BY generatedAt DESC")
    fun getActiveInsights(): Flow<List<Insight>>

    @Query("SELECT * FROM insights WHERE isDismissed = 0 AND isRead = 0 ORDER BY generatedAt DESC")
    fun getUnreadInsights(): Flow<List<Insight>>

    @Query("SELECT COUNT(*) FROM insights WHERE isDismissed = 0 AND isRead = 0")
    suspend fun getUnreadCount(): Int

    @Query("SELECT * FROM insights WHERE type = :type AND isDismissed = 0 ORDER BY generatedAt DESC")
    fun getByType(type: String): Flow<List<Insight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: Insight)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(insights: List<Insight>)

    @Query("UPDATE insights SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE insights SET isDismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: String)

    @Query("DELETE FROM insights WHERE generatedAt < :beforeTime")
    suspend fun deleteOldInsights(beforeTime: Long)

    @Query("DELETE FROM insights")
    suspend fun deleteAll()
}
