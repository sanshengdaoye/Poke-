package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferences?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesSync(): UserPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preferences: UserPreferences)

    @Update
    suspend fun update(preferences: UserPreferences)

    @Query("UPDATE user_preferences SET themeMode = :themeMode, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateThemeMode(themeMode: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_preferences SET defaultBookId = :bookId, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateDefaultBookId(bookId: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_preferences SET reminderEnabled = :enabled, dailyReminderHour = :hour, dailyReminderMinute = :minute, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int, updatedAt: Long = System.currentTimeMillis())
}
