package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "insights")
data class Insight(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val type: InsightType,
    val title: String,
    val content: String,
    val severity: InsightSeverity? = null,
    val periodStart: Long? = null,
    val periodEnd: Long? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InsightType {
    SPENDING_PATTERN,    // 消费模式分析
    BUDGET_ALERT,        // 预算预警
    SAVING_SUGGESTION,   // 储蓄建议
    ANOMALY_DETECTION,   // 异常消费检测
    MONTHLY_SUMMARY,     // 月度总结
    OVERSPEND_WARNING,   // 超支预警
    SAVING_MILESTONE,    // 储蓄里程碑
    IMPULSE_DETECTION,   // 冲动消费检测
    BUDGET_HEALTH,       // 预算健康度
    DAILY_BUDGET_ALERT   // 日均预算预警
}

enum class InsightSeverity {
    LOW,      // 低
    MEDIUM,   // 中
    HIGH,     // 高
    WARNING,  // 警告
    CRITICAL  // 严重
}
