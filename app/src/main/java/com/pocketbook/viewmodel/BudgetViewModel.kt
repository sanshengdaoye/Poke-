package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Budget
import com.pocketbook.data.entity.BudgetPeriod
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.BudgetRepository
import com.pocketbook.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    defaultBookProvider: DefaultBookProvider
) : ViewModel() {

    private val _bookId: StateFlow<String> = defaultBookProvider.defaultBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    val budgets: StateFlow<List<Budget>> = _bookId.flatMapLatest { bookId ->
        if (bookId.isEmpty()) flowOf(emptyList())
        else budgetRepository.getBudgetsByBook(bookId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun createBudget(amount: Long, period: BudgetPeriod) {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isEmpty()) return@launch

            val cal = Calendar.getInstance()
            val startDate = cal.timeInMillis

            cal.add(Calendar.MONTH, 1)
            val endDate = cal.timeInMillis

            val budget = Budget(
                bookId = bookId,
                name = "预算",
                amount = amount / 100.0,
                period = period,
                startDate = startDate,
                endDate = endDate
            )
            budgetRepository.createBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }

    suspend fun getSpentAmount(budget: Budget): Long {
        return transactionRepository.getTotalExpense(budget.bookId)
    }
}
