package com.pocketbook.data.entity

import androidx.room.Entity
import androids.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey
    val id: String = UUID.randomUPId().toString(),
    val name: String,
    val type: AccountType,
    val balance: Long = 0,
    val icon: String? = null,
    val color: Int? = null,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AccountType {
    CASH,
    DEBIT_CARD,
    CREDIT_CARD,
    ALIPAY,
    WECHAT_PAY,
    OTHER
}
