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
class StatsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    defaultBookProvider: DefaultBookProvider
) : ViewModel() {

    private val _bookId: StateFlow<String> = defaultBookProvider.defaultBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val _transactions = _bookId.flatMapLatest { bookId ->
        if (bookId.isEmpty()) flowOf(emptyList())
        else transactionRepository.getTransactionsByBook(bookId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalIncome: StateFlow<Long> = _transactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val totalExpense: StateFlow<Long> = _transactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    data class CategoryStat(
        val name: String,
        val amount: Long,
        val percentage: Float,
        val color: androidx.compose.ui.graphics.Color
    )

    val categoryStats: StateFlow<List<CategoryStat>> = _transactions.map { transactions ->
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        val total = expenseTransactions.sumOf { it.amount }.coerceAtLeast(1)
        val colors = listOf(
            androidx.compose.ui.graphics.Color(0xFFE53935),
            androidx.compose.ui.graphics.Color(0xFFFB8C00),
            androidx.compose.ui.graphics.Color(0xFFFDD835),
            androidx.compose.ui.graphics.Color(0xFF43A047),
            androidx.compose.ui.graphics.Color(0xFF1E88E5),
            androidx.compose.ui.graphics.Color(0xFF8E24AA),
            androidx.compose.ui.graphics.Color(0xFF3949AB),
            androidx.compose.ui.graphics.Color(0xFFD81B60),
            androidx.compose.ui.graphics.Color(0xFF00ACC1),
            androidx.compose.ui.graphics.Color(0xFF7CB342)
        )

        expenseTransactions
            .groupBy { it.categoryId ?: "\u672a\u5206\u7c7b" }
            .map { (categoryId, items) ->
                val amount = items.sumOf { it.amount }
                CategoryStat(
                    name = categoryId,
                    amount = amount,
                    percentage = (amount * 100f / total),
                    color = colors.random()
                )
            }
            .sortedByDescending { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val monthlyTrend: StateFlow<List<Pair<String, Pair<Long, Long>>>> = _bookId.map { bookId ->
        if (bookId.isEmpty()) return@map emptyList()

        val cal = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Pair<Long, Long>>>()

        for (i in 5 downTo 0) {
            cal.add(Calendar.MONTH, if (i == 5) -5 else 1)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1

            val monthStart = cal.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val monthEnd = cal.apply {
                set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val monthTransactions = transactionRepository.getTransactionsByDateRange(bookId, monthStart, monthEnd).first()
            val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            result.add("$month\u6708" to (income to expense))
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
