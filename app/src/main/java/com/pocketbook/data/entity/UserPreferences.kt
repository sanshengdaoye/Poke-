package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val currency: String = "CNY",
    val defaultBookId: String? = null,
    val reminderTime: String? = "21:00",
    val insightEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
