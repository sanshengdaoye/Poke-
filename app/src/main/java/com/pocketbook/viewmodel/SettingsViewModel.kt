package com.pocketbook.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.ThemeMode
import com.pocketbook.data.entity.UserPreferences
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.UserPreferencesRepository
import com.pocketbook.service.ExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportService: ExportService,
    private val defaultBookProvider: DefaultBookProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.getPreferences()
        .map { it?.themeMode ?: ThemeMode.SYSTEM }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    private val _exportResult = MutableStateFlow<Result<File>?>(null)
    val exportResult: StateFlow<Result<File>?> = _exportResult.asStateFlow()

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            val current = userPreferencesRepository.getPreferences().firstOrNull()
            val newTheme = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
            val prefs = current?.copy(themeMode = newTheme) ?: UserPreferences(themeMode = newTheme)
            userPreferencesRepository.savePreferences(prefs)
        }
    }

    fun exportCSV() {
        viewModelScope.launch {
            val bookId = defaultBookProvider.defaultBookId.firstOrNull() ?: ""
            if (bookId.isNotEmpty()) {
                _exportResult.value = exportService.exportToCSV(bookId, context)
            } else {
                _exportResult.value = Result.failure(Exception("No default book found"))
            }
        }
    }

    fun exportJSON() {
        viewModelScope.launch {
            val bookId = defaultBookProvider.defaultBookId.firstOrNull() ?: ""
            if (bookId.isNotEmpty()) {
                _exportResult.value = exportService.exportToJSON(bookId, context)
            } else {
                _exportResult.value = Result.failure(Exception("No default book found"))
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }
}
