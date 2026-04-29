package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["templateTransactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("templateTransactionId"),
        Index("isActive"),
        Index("nextTriggerDate")
    ]
)
data class RecurringTransaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val templateTransactionId: String, // 关联的交易模板
    val frequency: RecurringFrequency,
    val interval: Int = 1, // 间隔：每N天/周/月/年
    val nextTriggerDate: Long, // 下次触发时间戳
    val isActive: Boolean = true,
    val endDate: Long? = null, // 可选的结束日期
    val createdAt: Long = System.currentTimeMillis()
)

enum class RecurringFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}
