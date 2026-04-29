package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Category
import com.pocketbook.data.entity.CategoryType
import com.pocketbook.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getCategoriesByType(type: CategoryType): StateFlow<List<Category>> {
        return repository.getCategoriesByType(type)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun recordCategoryUsage(categoryId: String) {
        viewModelScope.launch {
            // Placeholder for usage tracking
        }
    }
}
