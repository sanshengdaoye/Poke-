package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.Insight
import com.pocketbook.data.entity.InsightType
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getByBook(bookId: String): Flow<List<Insight>>

    @Query("SELECT * FROM insights WHERE bookId = :bookId AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadByBook(bookId: String): Flow<List<Insight>>

    @Query("SELECT * FROM insights WHERE type = :type AND bookId = :bookId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByType(bookId: String, type: InsightType): Insight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: Insight)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(insights: List<Insight>)

    @Query("UPDATE insights SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Delete
    suspend fun delete(insight: Insight)

    @Query("DELETE FROM insights WHERE bookId = :bookId")
    suspend fun deleteByBook(bookId: String)

    // --- M3 新增 ---

    @Query("DELETE FROM insights WHERE bookId = :bookId AND type = :type")
    suspend fun deleteByType(bookId: String, type: InsightType)

    @Query("SELECT * FROM insights WHERE bookId = :bookId AND isRead = 0 ORDER BY createdAt DESC LIMIT 3")
    suspend fun getTopUnread(bookId: String): List<Insight>
}
