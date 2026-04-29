package com.pocketbook.repository

import com.pocketbook.data.dao.UserPreferencesDao
import com.pocketbook.data.entity.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao
) {
    fun getPreferences(): Flow<UserPreferences?> = userPreferencesDao.getPreferences()

    suspend fun savePreferences(preferences: UserPreferences) = userPreferencesDao.insert(preferences)

    suspend fun updatePreferences(preferences: UserPreferences) = userPreferencesDao.update(preferences)
}
