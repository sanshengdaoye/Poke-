package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val icon: String? = null,
    val color: Int? = null,
    val note: String? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AccountType {
    CASH, DEBIT_CARD, CREDIT_CARD, ALIPAY, WECHAT_PAY, SAVINGS, INVESTMENT, OTHER
}
