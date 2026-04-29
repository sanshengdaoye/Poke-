package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferences?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preferences: UserPreferences)

    @Update
    suspend fun update(preferences: UserPreferences)
}
