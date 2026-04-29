package com.pocketbook.service

import com.pocketbook.data.entity.*
import com.pocketbook.repository.*
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class InsightEngine @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val insightRepository: InsightRepository,
    private val categoryRepository: CategoryRepository
) {
    /**
     * Generate insights after a transaction is recorded.
     */
    suspend fun generateInsights(bookId: String, newTransaction: Transaction): List<Insight> {
        val insights = mutableListOf<Insight>()

        // 1. Monthly category spending check
        val categoryId = newTransaction.categoryId
        if (categoryId != null) {
            val monthlyExpense = transactionRepository.getExpenseByCategory(bookId, categoryId)
            val category = categoryRepository.getCategoryById(categoryId)

            // Category anomaly: if this category spent more than ¥1000
            if (monthlyExpense > 100000 && category != null) {
                insights.add(
                    Insight(
                        type = InsightType.ANOMALY,
                        title = "本月${category.name}支出偏高",
                        description = "本月${category.name}已支出 ¥${monthlyExpense / 100}.${String.format("%02d", monthlyExpense % 100)}，建议检查是否有非心要开支",
                        severity = InsightSeverity.WARNING,
                        relatedCategoryId = categoryId,
                        relatedAmount = monthlyExpense
                    )
                )
            }

            // Spending trend: compare with previous month
            val lastMonthExpense = getLastMonthCategoryExpense(bookId, categoryId)
            if (lastMonthExpense > 0 && monthlyExpense > lastMonthExpense * 1.5) {
                val increase = ((monthlyExpense - lastMonthExpense) * 100 / lastMonthExpense).toInt()
                insights.add(
                    Insight(
                        type = InsightType.ANOMALY,
                        title = "${category.name}支出比上月增长 ${increase}%",
                        description = "本月${category.name}比上月多支出了 ¥${(monthlyExpense - lastMonthExpense) / 100}.${String.format("%02d", (monthlyExpense - lastMonthExpense) % 100)}",
                        severity = InsightSeverity.WARNING,
                        relatedCategoryId = categoryId,
                        relatedAmount = monthlyExpense
                    )
                )
            }
        }

        // 2. Daily anomaly check
        val todayExpense = getTodayExpense(bookId)
        if (todayExpense > 50000) { // > ¥500
            insights.add(
                Insight(
                    type = InsightType.ANOMALY,
                    title = "今日支出较多",
                    description = "今日已支出 ¥${todayExpense / 100}.${String.format("%02d", todayExpense % 100)}，超过平日水平",
                    severity = InsightSeverity.WARNING,
                    relatedAmount = todayExpense
                )
            )
        }

        // 3. Monthly budget/income comparison
        val totalExpense = transactionRepository.getTotalExpense(bookId)
        val totalIncome = transactionRepository.getTotalIncome(bookId)

        if (totalIncome > 0) {
            val spendRatio = (totalExpense * 100 / totalIncome).toInt()
            when {
                spendRatio > 100 -> {
                    insights.add(
                        Insight(
                            type = InsightType.FORECAST,
                            title = "本月已超支",
                            description = "支出已超过收入 ¥${(totalExpense - totalIncome) / 100}.${String.format("%02d", (totalExpense - totalIncome) % 100)}，建议控制开支",
                            severity = InsightSeverity.CRITICAL,
                            relatedAmount = totalExpense
                        )
                    )
                }
                spendRatio > 80 -> {
                    insights.add(
                        Insight(
                            type = InsightType.FORECAST,
                            title = "本月支出达收入 ${spendRatio}%",
                            description = "剩余可用 ¥${(totalIncome - totalExpense) / 100}.${String.format("%02d", (totalIncome - totalExpense) % 100)}，建议控制开支",
                            severity = InsightSeverity.WARNING,
                            relatedAmount = totalExpense
                        )
                    )
                }
            }
        }

        // 4. Saving recommendation (simple version for MVP)
        if (totalIncome > totalExpense) {
            val savings = totalIncome - totalExpense
            insights.add(
                Insight(
                    type = InsightType.RECOMMENDATION,
                    title = "本月可存 ¥${savings / 100}.${String.format("%02d", savings % 100)}",
                    description = "当前结余率 ${((savings * 100 / totalIncome))}%，继续保持！",
                    severity = InsightSeverity.INFO,
                    relatedAmount = savings
                )
            )
        }

        // Save insights
        insights.forEach { insightRepository.saveInsight(it) }

        return insights
    }

    /**
     * Generate monthly health score report
     */
    suspend fun generateHealthScore(bookId: String): Insight? {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)

        val totalIncome = transactionRepository.getTotalIncome(bookId)
        val totalExpense = transactionRepository.getTotalExpense(bookId)
        val txCount = transactionRepository.getTransactionCount(bookId)

        // Simple scoring (MVP version)
        val budgetScore = if (totalIncome > 0) {
            (totalExpense * 100 / totalIncome).coerceAtMost(100)
        } else 100

        val activityScore = (txCount * 100 / 30).coerceAtMost(100)
        val savingsRate = if (totalIncome > 0) {
            ((totalIncome - totalExpense) * 100 / totalIncome).coerceAtLeast(0)
        } else 0

        val totalScore = (budgetScore * 0.4 + activityScore * 0.3 + savingsRate * 0.3).toInt()

        return Insight(
            type = InsightType.SCORE,
            title = "${currentMonth + 1}月消费健康评分: $totalScore",
            description = "预算执行: $budgetScore | 记账额度: $activityScore | 储蓄率: $savingsRate%",
            severity = when {
                totalScore >= 80 -> InsightSeverity.INFO
                totalScore >= 60 -> InsightSeverity.WARNING
                else -> InsightSeverity.CRITICAL
            },
            relatedAmount = totalExpense
        ).also { insightRepository.saveInsight(it) }
    }

    private suspend fun getTodayExpense(bookId: String): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        return transactionRepository.getTransactionsByDateRange(bookId, start, end)
            .firstOrNull()
            ?.filter { it.type == TransactionType.EXPENSE }
            ?.sumOf { it.amount }
            ?: 0L
    }

    private suspend fun getLastMonthCategoryExpense(bookId: String, categoryId: String): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        return transactionRepository.getTransactionsByDateRange(bookId, start, end)
            .firstOrNull()
            ?.filter { it.type == TransactionType.EXPENSE && it.categoryId == categoryId }
            ?.sumOf { it.amount }
            ?: 0L
    }
}
