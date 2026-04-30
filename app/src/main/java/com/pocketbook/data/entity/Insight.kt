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
    SPENDING_PATTERN,
    BUDGET_ALERT,
    SAVING_SUGGESTION,
    ANOMALY_DETECTION,
    MONTHLY_SUMMARY,
    OVERSPEND_WARNING,
    SAVING_MILESTONE,
    IMPULSE_DETECTION,
    BUDGET_HEALTH,
    DAILY_BUDGET_ALERT
}

enum class InsightSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
