package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Book
import com.pocketbook.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    val books: StateFlow<List<Book>> = repository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createBook(name: String, description: String? = null) {
        viewModelScope.launch {
            val book = Book(name = name, description = description)
            repository.createBook(book)
        }
    }

    fun setDefaultBook(id: String) {
        viewModelScope.launch {
            repository.setDefaultBook(id)
        }
    }
}
