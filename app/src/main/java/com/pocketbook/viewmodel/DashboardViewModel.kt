package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.InsightType
import com.pocketbook.repository.*
import com.pocketbook.ui.screens.InsightType as UiInsightType
import com.pocketbook.ui.screens.TransactionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val insightRepository: InsightRepository,
    private val bookRepository: BookRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                val defaultBook = bookRepository.getDefaultBook()
                val bookId = defaultBook?.id ?: return@launch

                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)

                val startOfMonth = getStartOfMonth(year, month)
                val endOfMonth = getEndOfMonth(year, month)
                val startOfDay = getStartOfDay()
                val endOfDay = getEndOfDay()

                // 月度收支
                val monthIncome = transactionRepository.getTotalIncome(bookId)
                val monthExpense = transactionRepository.getTotalExpense(bookId)
                val monthBalance = monthIncome - monthExpense

                // 今日支出
                val todayTransactions = transactionRepository.getTransactionsByBookAndDateRange(bookId, startOfDay, endOfDay)
                    .filter { it.type == com.pocketbook.data.entity.TransactionType.EXPENSE }
                val todayExpense = todayTransactions.sumOf { it.amount }
                val todayCount = todayTransactions.size

                // 最近3笔交易
                val recentTransactions = transactionRepository.getTransactionsByBookAndDateRange(bookId, startOfMonth, endOfMonth)
                    .sortedByDescending { it.date }
                    .take(3)
                    .map { t ->
                        val category = t.categoryId?.let { categoryRepository.getCategoryById(it) }
                        TransactionItem(
                            id = t.id,
                            emoji = getCategoryEmoji(category?.name ?: "其他"),
                            category = category?.name ?: "其他",
                            amount = t.amount
                        )
                    }

                // 预算使用情况
                val budgets = budgetRepository.getBudgetsByBook(bookId)
                val totalBudget = budgets.sumOf { it.amount }
                val totalExpenseInPeriod = transactionRepository.getTotalExpense(bookId)
                val budgetUsed = if (totalBudget > 0) {
                    (totalExpenseInPeriod.toFloat() / totalBudget.toFloat()).coerceIn(0f, 2f)
                } else 0f

                // 健康评分
                val healthScore = calculateHealthScore(
                    budgetExecution = budgetUsed,
                    savingRate = if (monthIncome > 0) (monthBalance.toFloat() / monthIncome.toFloat()) else 0f,
                    spendingVolatility = 0.2f,
                    impulseCount = 0
                )

                // 最新洞察 - 使用 getTopUnread
                val latestInsight = insightRepository.getTopUnread(bookId).firstOrNull()?.let { insight ->
                    DashboardInsight(
                        title = insight.title,
                        description = insight.content,
                        type = when (insight.type) {
                            com.pocketbook.data.entity.InsightType.OVERSPEND_WARNING -> UiInsightType.OVERSPEND_WARNING
                            com.pocketbook.data.entity.InsightType.SAVING_SUGGESTION -> UiInsightType.SAVING_SUGGESTION
                            com.pocketbook.data.entity.InsightType.SAVING_MILESTONE -> UiInsightType.SAVING_MILESTONE
                            com.pocketbook.data.entity.InsightType.IMPULSE_DETECTION -> UiInsightType.IMPULSE_DETECTION
                            else -> UiInsightType.TREND_ANALYSIS
                        }
                    )
                }

                _uiState.value = DashboardUiState(
                    monthBalance = monthBalance,
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    budgetUsedPercent = budgetUsed,
                    isBudgetOver = budgetUsed > 1f,
                    healthScore = healthScore,
                    budgetExecutionRate = budgetUsed,
                    spendingVolatility = getVolatilityLabel(0.2f),
                    savingRate = if (monthIncome > 0) (monthBalance.toFloat() / monthIncome.toFloat()) else 0f,
                    todayExpense = todayExpense,
                    todayTransactionCount = todayCount,
                    recentTransactions = recentTransactions,
                    latestInsight = latestInsight
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateHealthScore(
        budgetExecution: Float,
        savingRate: Float,
        spendingVolatility: Float,
        impulseCount: Int
    ): Int {
        val w1 = 0.3f
        val w2 = 0.3f
        val w3 = 0.2f
        val w4 = 0.2f

        val s1 = (1 - budgetExecution.coerceIn(0f, 1f)) * 100
        val s2 = savingRate.coerceIn(0f, 1f) * 100
        val s3 = (1 - spendingVolatility.coerceIn(0f, 1f)) * 100
        val s4 = (100 - impulseCount * 10f).coerceIn(0f, 100f)

        return (s1 * w1 + s2 * w2 + s3 * w3 + s4 * w4).toInt().coerceIn(0, 100)
    }

    private fun calculateVolatility(bookId: String, year: Int, month: Int): Float {
        // 简化实现：计算日均支出标准差/均值
        return 0.2f // 占位，实际需按日统计
    }

    private fun getVolatilityLabel(volatility: Float): String {
        return when {
            volatility < 0.3f -> "低"
            volatility < 0.6f -> "中"
            else -> "高"
        }
    }

    private fun getStartOfMonth(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonth(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun getCategoryEmoji(name: String): String {
        return when {
            name.contains("餐饮") || name.contains("餐") || name.contains("饭") || name.contains("食") -> "🍽️"
            name.contains("交通") || name.contains("车") || name.contains("地铁") || name.contains("公交") -> "🚌"
            name.contains("购物") || name.contains("买") -> "🛍️"
            name.contains("娱乐") || name.contains("玩") || name.contains("电影") -> "🎬"
            name.contains("医疗") || name.contains("药") -> "💊"
            name.contains("教育") || name.contains("学") -> "📚"
            name.contains("住房") || name.contains("房") || name.contains("租") -> "🏠"
            name.contains("通讯") || name.contains("话") || name.contains("网") -> "📱"
            name.contains("人情") || name.contains("礼") -> "🎁"
            name.contains("工资") || name.contains("薪") || name.contains("收入") -> "💰"
            else -> "💸"
        }
    }
}

data class DashboardUiState(
    val monthBalance: Long = 0,
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
    val budgetUsedPercent: Float = 0f,
    val isBudgetOver: Boolean = false,
    val healthScore: Int = 0,
    val budgetExecutionRate: Float = 0f,
    val spendingVolatility: String = "低",
    val savingRate: Float = 0f,
    val todayExpense: Long = 0,
    val todayTransactionCount: Int = 0,
    val recentTransactions: List<TransactionItem> = emptyList(),
    val latestInsight: DashboardInsight? = null
)

data class DashboardInsight(
    val title: String,
    val description: String,
    val type: com.pocketbook.ui.screens.InsightType
)
