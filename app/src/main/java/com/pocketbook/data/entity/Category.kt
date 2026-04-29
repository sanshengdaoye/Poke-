package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String? = null,
    val color: Int? = null,
    val type: CategoryType,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CategoryType {
    EXPENSE, INCOME
}
