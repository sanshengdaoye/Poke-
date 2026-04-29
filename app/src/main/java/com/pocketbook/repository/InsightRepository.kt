package com.pocketbook.repository

import com.pocketbook.data.dao.InsightDao
import com.pocketbook.data.entity.Insight
import com.pocketbook.data.entity.InsightType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepository @Inject constructor(
    private val insightDao: InsightDao
) {
    fun getInsightsByBook(bookId: String): Flow<List<Insight>> =
        insightDao.getByBook(bookId)

    fun getUnreadInsights(bookId: String): Flow<List<Insight>> =
        insightDao.getUnreadByBook(bookId)

    suspend fun getLatestByType(bookId: String, type: InsightType): Insight? =
        insightDao.getLatestByType(bookId, type)

    suspend fun createInsight(insight: Insight) = insightDao.insert(insight)

    suspend fun createInsights(insights: List<Insight>) = insightDao.insertAll(insights)

    suspend fun markAsRead(id: String) = insightDao.markAsRead(id)

    suspend fun deleteInsight(insight: Insight) = insightDao.delete(insight)

    suspend fun deleteByBook(bookId: String) = insightDao.deleteByBook(bookId)

    // --- M3 新增 ---

    suspend fun deleteByType(bookId: String, type: InsightType) =
        insightDao.deleteByType(bookId, type)

    suspend fun getTopUnread(bookId: String): List<Insight> =
        insightDao.getTopUnread(bookId)
}
