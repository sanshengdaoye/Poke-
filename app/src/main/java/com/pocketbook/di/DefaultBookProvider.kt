package com.pocketbook.di

import com.pocketbook.data.entity.UserPreferences
import com.pocketbook.repository.BookRepository
import com.pocketbook.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBookProvider @Inject constructor(
    bookRepository: BookRepository,
    userPreferencesRepository: UserPreferencesRepository
) {
    val defaultBookId: Flow<String> = userPreferencesRepository.getPreferences()
        .map { prefs ->
            prefs?.defaultBookId ?: bookRepository.getDefaultBook()?.id ?: ""
        }
        .filter { it.isNotEmpty() }
        .distinctUntilChanged()
}
