package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
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
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("bookId"),
        Index("categoryId"),
        Index("accountId"),
        Index("date")
    ]
)
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val type: TransactionType,
    val amount: Long,
    val categoryId: String? = null,
    val accountId: String? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}
