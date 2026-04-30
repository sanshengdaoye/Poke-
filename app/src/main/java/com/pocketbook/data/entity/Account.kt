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
    val balance: Long = 0, // 单位：分，避免浮点误差
    val icon: String? = null,
    val color: Int? = null,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AccountType {
    CASH,          // 现金
    DEBIT_CARD,  // 银行卡（借记卡）
    CREDIT_CARD, // 信用卡
    ALIPAY,      // 支付宝
    WECHAT_PAY,  // 微信支付
    OTHER        // 其他
}
