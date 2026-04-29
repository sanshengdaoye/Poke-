package com.pocketbook.repository

import com.pocketbook.data.dao.InsightDao
import com.pocketbook.data.entity.Insight
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepository @Inject constructor(
    private val insightDao: InsightDao
) {
    fun getActiveInsights(): Flow<List<Insight>> = insightDao.getActiveInsights()

    fun getUnreadInsights(): Flow<List<Insight>> = insightDao.getUnreadInsights()

    suspend fun getUnreadCount(): Int = insightDao.getUnreadCount()

    fun getInsightsByType(type: String): Flow<List<Insight>> = insightDao.getByType(type)

    suspend fun saveInsight(insight: Insight) = insightDao.insert(insight)

    suspend fun saveInsights(insights: List<Insight>) = insightDao.insertAll(insights)

    suspend fun markAsRead(id: String) = insightDao.markAsRead(id)

    suspend fun dismiss(id: String) = insightDao.dismiss(id)

    suspend fun deleteOldInsights(beforeTime: Long) = insightDao.deleteOldInsights(beforeTime)

    suspend fun deleteAll() = insightDao.deleteAll()
}
