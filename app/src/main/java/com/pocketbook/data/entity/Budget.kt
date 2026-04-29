package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("bookId"),
        Index("categoryId")
    ]
)
data class Budget(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val name: String,
    val amount: Double,
    val categoryId: String? = null,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BudgetPeriod {
    WEEKLY, MONTHLY, YEARLY, CUSTOM
}
