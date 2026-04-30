package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Transaction
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.CategoryRepository
import com.pocketbook.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
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

    // Category name lookup cache
    private val _categoryNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val categoryNames: StateFlow<Map<String, String>> = _categoryNames.asStateFlow()

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
            transactions.collect { txs ->
                val bookId = _bookId.value
                if (bookId.isNotEmpty()) {
                    _totalIncome.value = repository.getTotalIncome(bookId)
                    _totalExpense.value = repository.getTotalExpense(bookId)
                }
                // Build category name cache
                val names = mutableMapOf<String, String>()
                txs.forEach { t ->
                    t.categoryId?.let { catId ->
                        if (!names.containsKey(catId)) {
                            val cat = categoryRepository.getCategoryById(catId)
                            names[catId] = cat?.name ?: "未分类"
                        }
                    }
                }
                _categoryNames.value = names
            }
        }
    }

    fun getCategoryName(categoryId: String?): String {
        if (categoryId == null) return "未分类"
        return _categoryNames.value[categoryId] ?: "未分类"
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
