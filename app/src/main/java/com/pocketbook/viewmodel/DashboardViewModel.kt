package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Insight
import com.pocketbook.data.entity.Transaction
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.BudgetRepository
import com.pocketbook.repository.InsightRepository
import com.pocketbook.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val insightRepository: InsightRepository,
    defaultBookProvider: DefaultBookProvider
) : ViewModel() {

    private val _bookId: StateFlow<String> = defaultBookProvider.defaultBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val _todayExpense = MutableStateFlow(0L)
    val todayExpense: StateFlow<Long> = _todayExpense.asStateFlow()

    private val _todayIncome = MutableStateFlow(0L)
    val todayIncome: StateFlow<Long> = _todayIncome.asStateFlow()

    private val _weekExpense = MutableStateFlow(0L)
    val weekExpense: StateFlow<Long> = _weekExpense.asStateFlow()

    private val _monthExpense = MutableStateFlow(0L)
    val monthExpense: StateFlow<Long> = _monthExpense.asStateFlow()

    private val _monthIncome = MutableStateFlow(0L)
    val monthIncome: StateFlow<Long> = _monthIncome.asStateFlow()

    private val _monthBudget = MutableStateFlow(0L)
    val monthBudget: StateFlow<Long> = _monthBudget.asStateFlow()

    private val _monthBudgetUsed = MutableStateFlow(0L)
    val monthBudgetUsed: StateFlow<Long> = _monthBudgetUsed.asStateFlow()

    val recentTransactions: StateFlow<List<Transaction>> = _bookId
        .flatMapLatest { bookId ->
            if (bookId.isEmpty()) flowOf(emptyList())
            else transactionRepository.getTransactionsByBook(bookId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val insights: StateFlow<List<Insight>> = _bookId
        .flatMapLatest { bookId ->
            if (bookId.isEmpty()) flowOf(emptyList())
            else insightRepository.getUnreadInsights(bookId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _bookId.collect { bookId ->
                if (bookId.isNotEmpty()) {
                    refreshDashboard(bookId)
                }
            }
        }
    }

    private suspend fun refreshDashboard(bookId: String) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Today range
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val todayEnd = cal.timeInMillis

        val todayTransactions = transactionRepository.getTransactionsByBookAndDateRange(bookId, todayStart, todayEnd)
        _todayExpense.value = todayTransactions.filter { it.type == com.pocketbook.data.entity.TransactionType.EXPENSE }.sumOf { it.amount }
        _todayIncome.value = todayTransactions.filter { it.type == com.pocketbook.data.entity.TransactionType.INCOME }.sumOf { it.amount }

        // Week range (start of week)
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val weekStart = cal.timeInMillis

        val weekTransactions = transactionRepository.getTransactionsByBookAndDateRange(bookId, weekStart, todayEnd)
        _weekExpense.value = weekTransactions.filter { it.type == com.pocketbook.data.entity.TransactionType.EXPENSE }.sumOf { it.amount }

        // Month range
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val monthStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val monthEnd = cal.timeInMillis

        val monthTransactions = transactionRepository.getTransactionsByBookAndDateRange(bookId, monthStart, monthEnd)
        _monthExpense.value = monthTransactions.filter { it.type == com.pocketbook.data.entity.TransactionType.EXPENSE }.sumOf { it.amount }
        _monthIncome.value = monthTransactions.filter { it.type == com.pocketbook.data.entity.TransactionType.INCOME }.sumOf { it.amount }

        // Budget
        val budgets = budgetRepository.getBudgetsByBook(bookId)
        val totalBudget = budgets.filter { it.isActive }.sumOf { it.amount }
        _monthBudget.value = totalBudget
        _monthBudgetUsed.value = _monthExpense.value
    }
}
