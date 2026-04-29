package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Insight
import com.pocketbook.data.entity.InsightType
import com.pocketbook.repository.InsightRepository
import com.pocketbook.repository.TransactionRepository
import com.pocketbook.repository.BudgetRepository
import com.pocketbook.repository.AccountRepository
import com.pocketbook.service.InsightEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class InsightEngineViewModel(
    private val transactionRepository: TransactionRepository,
    private val insightRepository: InsightRepository,
    private val budgetRepository: BudgetRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _latestInsights = MutableStateFlow<List<Insight>>(emptyList())
    val latestInsights: StateFlow<List<Insight>> = _latestInsights

    fun generateInsightsAfterTransaction(bookId: String = "default") {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentMonthStart = calendar.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val currentMonthEnd = calendar.apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayEnd = todayStart + 86400000 - 1

            val insights = mutableListOf<Insight>()

            val budgetAlert = checkBudgetAlert(bookId, currentMonthStart, currentMonthEnd)
            budgetAlert?.let { insights.add(it) }

            val anomaly = checkAnomalyDetection(bookId, todayStart, todayEnd)
            anomaly?.let { insights.add(it) }

            val savingSuggestion = generateSavingSuggestion(bookId, currentMonthStart, currentMonthEnd)
            savingSuggestion?.let { insights.add(it) }

            val healthScore = generateHealthScore(bookId, currentMonthStart, currentMonthEnd)
            healthScore?.let { insights.add(it) }

            if (insights.isNotEmpty()) {
                insightRepository.createInsights(insights)
                _latestInsights.value = insights
            }
        }
    }

    private suspend fun checkBudgetAlert(
        bookId: String,
        monthStart: Long,
        monthEnd: Long
    ): Insight? {
        val totalExpense = transactionRepository.getTotalExpense(bookId)
        val totalIncome = transactionRepository.getTotalIncome(bookId)

        if (totalIncome > 0 && totalExpense > totalIncome * 0.8) {
            val ratio = (totalExpense / totalIncome * 100).toInt()
            return Insight(
                bookId = bookId,
                type = InsightType.BUDGET_ALERT,
                title = "⚠️ 支出预警",
                content = "本月支出已达收入的 ${ratio}%，建议控制消费。剩余预算 ¥${String.format("%.2f", (totalIncome - totalExpense) / 100.0)}"
            )
        }

        return null
    }

    private suspend fun checkAnomalyDetection(
        bookId: String,
        todayStart: Long,
        todayEnd: Long
    ): Insight? {
        val maxDaily = transactionRepository.getMaxDailyExpense(bookId, todayStart - 30L * 86400000, todayEnd)

        return maxDaily?.let {
            val amount = it.total
            if (amount > 50000) {
                Insight(
                    bookId = bookId,
                    type = InsightType.ANOMALY_DETECTION,
                    title = "🔍 异常消费检测",
                    content = "检测到单日大额支出 ¥${String.format("%.2f", amount / 100.0)}，高于近期平均水平，请确认是否为必要支出。"
                )
            } else null
        }
    }

    private suspend fun generateSavingSuggestion(
        bookId: String,
        monthStart: Long,
        monthEnd: Long
    ): Insight? {
        val categoryExpenses = transactionRepository.getExpenseByCategory(bookId, monthStart, monthEnd)

        if (categoryExpenses.isEmpty()) return null

        val topCategory = categoryExpenses.firstOrNull() ?: return null

        val suggestions = listOf(
            "餐饮支出占比较高，尝试自己做饭可节省 30-50%",
            "交通费用较多，考虑公共交通月卡或骑行",
            "娱乐支出偏高，寻找免费或低价替代活动",
            "购物支出较大，建议设置「冷静期」避免冲动消费",
            "订阅服务累积费用不低，检查是否有闲置会员"
        )

        return Insight(
            bookId = bookId,
            type = InsightType.SAVING_SUGGESTION,
            title = "💡 省钱建议",
            content = suggestions.random()
        )
    }

    private suspend fun generateHealthScore(
        bookId: String,
        monthStart: Long,
        monthEnd: Long
    ): Insight? {
        val engine = InsightEngine(transactionRepository, budgetRepository, insightRepository, accountRepository)
        val score = engine.generateHealthScore(bookId)

        val evaluation = when {
            score >= 90 -> "优秀！储蓄率很高，继续保持"
            score >= 75 -> "良好，还有优化空间"
            score >= 60 -> "一般，建议审视支出结构"
            else -> "需要关注，支出接近或超过收入"
        }

        return Insight(
            bookId = bookId,
            type = InsightType.SPENDING_PATTERN,
            title = "📊 消费健康评分",
            content = "本月评分：${score}分。$evaluation"
        )
    }

    fun loadUnreadInsights(bookId: String = "default") {
        viewModelScope.launch {
            val unread = insightRepository.getTopUnread(bookId)
            _latestInsights.value = unread
        }
    }

    fun markInsightAsRead(insightId: String) {
        viewModelScope.launch {
            insightRepository.markAsRead(insightId)
            _latestInsights.value = _latestInsights.value.filter { it.id != insightId }
        }
    }

    fun dismissAllInsights() {
        viewModelScope.launch {
            _latestInsights.value.forEach { insight ->
                insightRepository.markAsRead(insight.id)
            }
            _latestInsights.value = emptyList()
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val insightRepository: InsightRepository,
        private val budgetRepository: BudgetRepository,
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InsightEngineViewModel(transactionRepository, insightRepository, budgetRepository, accountRepository) as T
        }
    }
}