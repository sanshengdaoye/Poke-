package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Transaction
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    defaultBookProvider: DefaultBookProvider
) : ViewModel() {

    private val _bookId: StateFlow<String> = defaultBookProvider.defaultBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val transactions: StateFlow<List<Transaction>> = _bookId
        .flatMapLatest { bookId ->
            if (bookId.isEmpty()) flowOf(emptyList())
            else {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis

                repository.getTransactionsByDateRange(bookId, start, end)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _totalIncome = MutableStateFlow(0L)
    val totalIncome: StateFlow<Long> = _totalIncome.asStateFlow()

    private val _totalExpense = MutableStateFlow(0L)
    val totalExpense: StateFlow<Long> = _totalExpense.asStateFlow()

    init {
        viewModelScope.launch {
            _bookId.collect { bookId ->
                if (bookId.isNotEmpty()) {
                    _totalIncome.value = repository.getTotalIncome(bookId)
                    _totalExpense.value = repository.getTotalExpense(bookId)
                }
            }
        }

        viewModelScope.launch {
            transactions.collect {
                val bookId = _bookId.value
                if (bookId.isNotEmpty()) {
                    _totalIncome.value = repository.getTotalIncome(bookId)
                    _totalExpense.value = repository.getTotalExpense(bookId)
                }
            }
        }
    }

    fun createTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.createTransaction(transaction)
            refreshTotals()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            refreshTotals()
        }
    }

    private fun refreshTotals() {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isNotEmpty()) {
                _totalIncome.value = repository.getTotalIncome(bookId)
                _totalExpense.value = repository.getTotalExpense(bookId)
            }
        }
    }
}
