package com.pocketbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1, // 单例表，只有一条记录
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultBookId: String? = null,
    val dailyReminderHour: Int = 21,
    val dailyReminderMinute: Int = 0,
    val reminderEnabled: Boolean = true,
    val currency: String = "CNY",
    val firstDayOfWeek: Int = 1, // 1=周一
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
