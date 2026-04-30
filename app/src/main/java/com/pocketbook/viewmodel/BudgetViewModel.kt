package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Account
import com.pocketbook.data.entity.Budget
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.AccountRepository
import com.pocketbook.repository.BudgetRepository
import com.pocketbook.repository.CategoryRepository
import com.pocketbook.repository.TransactionRepository
import com.pocketbook.ui.screens.CategoryBudgetDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    defaultBookProvider: DefaultBookProvider
) : ViewModel() {

    private val _bookId: StateFlow<String> = defaultBookProvider.defaultBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    val totalBudget: StateFlow<Long> = _budgets.map { list ->
        list.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val totalUsed: StateFlow<Long> = combine(_bookId, _budgets) { bookId, budgets ->
        if (bookId.isEmpty()) 0L
        else transactionRepository.getTotalExpense(bookId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val categoryBudgets: StateFlow<List<CategoryBudgetDisplay>> = combine(_bookId, _budgets) { bookId, budgets ->
        if (bookId.isEmpty()) return@combine emptyList()

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

        val transactions = transactionRepository.getTransactionsByBookAndDateRange(bookId, start, end)
            .filter { it.type == TransactionType.EXPENSE }

        budgets.mapNotNull { budget ->
            val category = budget.categoryId?.let { categoryRepository.getCategoryById(it) }
            val used = if (budget.categoryId != null) {
                transactions.filter { it.categoryId == budget.categoryId }.sumOf { it.amount }
            } else {
                transactions.sumOf { it.amount }
            }

            CategoryBudgetDisplay(
                categoryName = category?.name ?: "总预算",
                emoji = getCategoryEmoji(category?.name ?: "总预算"),
                budgetAmount = budget.amount,
                usedAmount = used,
                percent = if (budget.amount > 0) used.toFloat() / budget.amount.toFloat() else 0f,
                rawBudget = budget
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            _bookId.collect { bookId ->
                if (bookId.isNotEmpty()) {
                    _budgets.value = budgetRepository.getBudgetsByBook(bookId)
                }
            }
        }
    }

    fun createBudget(categoryId: String, amount: Long) {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isEmpty()) return@launch

            val budget = Budget(
                bookId = bookId,
                categoryId = categoryId.takeIf { it.isNotEmpty() },
                amount = amount,
                startDate = System.currentTimeMillis()
            )
            budgetRepository.createBudget(budget)
            _budgets.value = budgetRepository.getBudgetsByBook(bookId)
        }
    }

    fun updateBudget(budgetId: String, amount: Long) {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isEmpty()) return@launch

            val existing = _budgets.value.find { it.id == budgetId }
            if (existing != null) {
                val updated = existing.copy(amount = amount)
                budgetRepository.updateBudget(updated)
                _budgets.value = budgetRepository.getBudgetsByBook(bookId)
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isEmpty()) return@launch

            budgetRepository.deleteBudget(budgetId)
            _budgets.value = budgetRepository.getBudgetsByBook(bookId)
        }
    }

    private fun getCategoryEmoji(name: String): String {
        return when {
            name.contains("餐饮") || name.contains("餐") || name.contains("饭") || name.contains("食") || name.contains("咖啡") -> "🍽️"
            name.contains("交通") || name.contains("车") || name.contains("地铁") || name.contains("公交") || name.contains("打车") -> "🚌"
            name.contains("购物") || name.contains("买") || name.contains("淘宝") || name.contains("京东") -> "🛍️"
            name.contains("娱乐") || name.contains("玩") || name.contains("电影") || name.contains("游戏") || name.contains("奶茶") -> "🎬"
            name.contains("医疗") || name.contains("药") || name.contains("医") -> "💊"
            name.contains("教育") || name.contains("学") || name.contains("书") || name.contains("课") -> "📚"
            name.contains("住房") || name.contains("房") || name.contains("租") || name.contains("物业") || name.contains("水电") -> "🏠"
            name.contains("通讯") || name.contains("话") || name.contains("网") || name.contains("流量") || name.contains("手机") -> "📱"
            name.contains("人情") || name.contains("礼") || name.contains("红包") || name.contains("请客") -> "🎁"
            name.contains("工资") || name.contains("薪") || name.contains("收入") || name.contains("奖金") -> "💰"
            else -> "💸"
        }
    }
}
