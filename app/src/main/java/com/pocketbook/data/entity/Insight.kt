package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "insights")
data class Insight(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: InsightType,
    val title: String,
    val description: String,
    val severity: InsightSeverity? = null,
    val relatedCategoryId: String? = null,
    val relatedAmount: Long? = null,
    val dataSnapshot: String? = null,
    val isRead: Boolean = false,
    val isDismissed: Boolean = false,
    val generatedAt: Long = System.currentTimeMillis()
)

enum class InsightType {
    PATTERN,
    ANOMALY,
    FORECAST,
    RECOMMENDATION,
    SCORE
}

enum class InsightSeverity {
    INFO,
    WARNING,
    CRITICAL
}
