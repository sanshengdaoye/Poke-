package com.pocketbook.service

import com.pocketbook.data.entity.*
import com.pocketbook.repository.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Singleton
class InsightEngine @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val insightRepository: InsightRepository,
    private val accountRepository: AccountRepository
) {

    /**
     * 生成消费健康评分（0-100）
     * 基于：预算执行率、消费波动性、储蓄率
     */
    suspend fun generateHealthScore(bookId: String): Int {
        val calendar = Calendar.getInstance()
        val monthStart = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthEnd = calendar.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val totalIncome = transactionRepository.getTotalIncome(bookId) ?: 0L
        val totalExpense = transactionRepository.getTotalExpense(bookId) ?: 0L

        // 1. 储蓄率得分 (0-40)
        val savingRate = if (totalIncome > 0) {
            (totalIncome - totalExpense).toDouble() / totalIncome.toDouble()
        } else 0.0
        val savingScore = when {
            savingRate >= 0.3 -> 40
            savingRate >= 0.2 -> 32
            savingRate >= 0.1 -> 24
            savingRate >= 0.0 -> 16
            else -> 0
        }

        // 2. 预算执行率得分 (0-35)
        val budgets = budgetRepository.getBudgetsByBook(bookId)
        var budgetScore = 35
        if (budgets.isNotEmpty()) {
            var totalBudgetUsed = 0.0
            var totalBudgetAmount = 0.0
            budgets.forEach { budget ->
                val used = transactionRepository.getCategoryExpenseInPeriod(
                    bookId, budget.categoryId ?: "", monthStart, monthEnd
                ) ?: 0L
                totalBudgetUsed += used
                totalBudgetAmount += budget.amount
            }
            val budgetUsage = if (totalBudgetAmount > 0) totalBudgetUsed / totalBudgetAmount else 0.0
            budgetScore = when {
                budgetUsage <= 0.7 -> 35
                budgetUsage <= 0.9 -> 28
                budgetUsage <= 1.0 -> 21
                budgetUsage <= 1.2 -> 14
                else -> 7
            }
        }

        // 3. 消费波动性得分 (0-25)
        val dailyExpenses = transactionRepository.getDailyExpenseForCalendar(bookId, monthStart, monthEnd)
        val volatilityScore = if (dailyExpenses.size >= 7) {
            val amounts = dailyExpenses.map { it.total }
            val avg = amounts.average()
            val variance = amounts.map { (it - avg) * (it - avg) }.average()
            val stdDev = kotlin.math.sqrt(variance)
            val cv = if (avg > 0) stdDev / avg else 0.0
            when {
                cv < 0.5 -> 25
                cv < 1.0 -> 20
                cv < 1.5 -> 15
                cv < 2.0 -> 10
                else -> 5
            }
        } else 20 // 数据不足，给中等分

        return min(100, savingScore + budgetScore + volatilityScore)
    }

    /**
     * 检测冲动消费
     * 触发条件：深夜大额(22-06点, >500元)、连续大额(3天内2笔>均值200%)、与历史均值偏差>200%
     */
    suspend fun detectImpulseSpending(bookId: String, transaction: Transaction): Insight? {
        val amount = transaction.amount
        val hour = Calendar.getInstance().apply { timeInMillis = transaction.date }.get(Calendar.HOUR_OF_DAY)
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - 3 * 86400000L

        // 条件1：深夜大额消费 (22:00 - 06:00，单笔 > 50000分=500元)
        val isLateNight = hour >= 22 || hour < 6
        val isLargeAmount = amount > 50000

        // 条件2：连续大额消费（近3天内已有另一笔大额支出）
        val recentTransactions = transactionRepository.getTransactionsByBookAndDateRange(bookId, threeDaysAgo, now)
        val largeCount = recentTransactions.count { it.amount > amount * 0.5 && it.id != transaction.id }
        val isConsecutiveLarge = largeCount >= 1

        // 条件3：与历史均值偏差 > 200%
        val categoryExpenses = transactionRepository.getExpenseByCategory(bookId, now - 90 * 86400000L, now)
        val avgCategoryExpense = if (categoryExpenses.isNotEmpty()) {
            categoryExpenses.map { it.total }.average()
        } else 0.0
        val isAnomaly = avgCategoryExpense > 0 && amount > avgCategoryExpense * 2.0

        return when {
            isLateNight && isLargeAmount -> Insight(
                bookId = bookId,
                type = InsightType.ANOMALY_DETECTION,
                title = "🌙 深夜大额消费提醒",
                content = "凌晨${hour}点消费 ¥${formatAmount(amount)}，深夜大额支出容易被忽略，建议确认是否为必要消费。"
            )
            isConsecutiveLarge && isLargeAmount -> Insight(
                bookId = bookId,
                type = InsightType.ANOMALY_DETECTION,
                title = "⚡ 连续大额消费预警",
                content = "近3天内已有大额支出，本次又消费 ¥${formatAmount(amount)}，注意控制消费节奏。"
            )
            isAnomaly -> Insight(
                bookId = bookId,
                type = InsightType.ANOMALY_DETECTION,
                title = "📈 异常消费检测",
                content = "本次消费 ¥${formatAmount(amount)} 远超该类目历史均值，建议确认交易真实性。"
            )
            else -> null
        }
    }

    /**
     * 预测预算燃烧率
     * 基于本月已用天数和已用预算，预测月底是否会超支
     */
    suspend fun predictBudgetBurnRate(bookId: String): Insight? {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthProgress = today.toDouble() / daysInMonth.toDouble()

        val monthStart = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthEnd = calendar.apply {
            set(Calendar.DAY_OF_MONTH, daysInMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val totalIncome = transactionRepository.getTotalIncome(bookId) ?: 0L
        val totalExpense = transactionRepository.getTotalExpense(bookId) ?: 0L

        if (totalIncome <= 0) return null

        val budgets = budgetRepository.getBudgetsByBook(bookId)
        if (budgets.isEmpty()) {
            // 无预算设置，基于收入支出比预测
            val expenseRatio = totalExpense.toDouble() / totalIncome.toDouble()
            val projectedRatio = if (monthProgress > 0) expenseRatio / monthProgress else 0.0

            return when {
                projectedRatio > 1.2 -> Insight(
                    bookId = bookId,
                    type = InsightType.BUDGET_ALERT,
                    title = "🔥 支出严重超标预警",
                    content = "按当前速度，月底支出将达收入的 ${(projectedRatio * 100).toInt()}%，建议立即控制消费。"
                )
                projectedRatio > 1.0 -> Insight(
                    bookId = bookId,
                    type = InsightType.BUDGET_ALERT,
                    title = "⚠️ 支出即将超收入",
                    content = "按当前速度，月底支出将与收入持平。剩余日均可用 ¥${formatAmount(max(0, (totalIncome - totalExpense) / (daysInMonth - today + 1)))}。"
                )
                projectedRatio > 0.8 -> Insight(
                    bookId = bookId,
                    type = InsightType.BUDGET_ALERT,
                    title = "💡 支出偏快提醒",
                    content = "已用 ${(monthProgress * 100).toInt()}% 的时间，支出占收入 ${(expenseRatio * 100).toInt()}%。建议适当节省。"
                )
                else -> null
            }
        } else {
            // 有预算设置，基于预算执行率预测
            var totalBudget = 0L
            var totalUsed = 0L
            var overBudgetCategories = mutableListOf<String>()

            budgets.filter { it.isActive }.forEach { budget ->
                totalBudget += budget.amount
                val used = transactionRepository.getCategoryExpenseInPeriod(
                    bookId, budget.categoryId ?: "", monthStart, monthEnd
                ) ?: 0L
                totalUsed += used
                if (used > budget.amount) {
                    overBudgetCategories.add(budget.name)
                }
            }

            val budgetUsage = if (totalBudget > 0) totalUsed.toDouble() / totalBudget.toDouble() else 0.0
            val projectedUsage = if (monthProgress > 0) budgetUsage / monthProgress else 0.0

            return when {
                overBudgetCategories.isNotEmpty() -> Insight(
                    bookId = bookId,
                    type = InsightType.BUDGET_ALERT,
                    title = "🚨 预算超支",
                    content = "${overBudgetCategories.joinToString("、")} 已超预算，本月剩余可用 ¥${formatAmount(max(0, totalBudget - totalUsed))}。"
                )
                projectedUsage > 1.0 -> Insight(
                    bookId = bookId,
                    type = InsightType.BUDGET_ALERT,
                    title = "🔥 预算即将耗尽",
                    content = "按当前速度，月底将超预算 ${((projectedUsage - 1.0) * 100).toInt()}%。建议控制 ${budgets.firstOrNull()?.name ?: "支出"}。"
                )
                projectedUsage > 0.85 -> Insight(
                    bookId = bookId,
                    type = InsightType.BUDGET_ALERT,
                    title = "⚠️ 预算消耗偏快",
                    content = "已用 ${(monthProgress * 100).toInt()}% 时间，预算消耗 ${(budgetUsage * 100).toInt()}%。"
                )
                else -> null
            }
        }
    }

    private fun formatAmount(amount: Long): String {
        return String.format("%.2f", amount / 100.0)
    }

    private fun formatAmount(amount: Double): String {
        return String.format("%.2f", amount / 100.0)
    }
}
