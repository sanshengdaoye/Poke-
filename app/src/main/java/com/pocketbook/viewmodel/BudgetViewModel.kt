package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Budget
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.di.DefaultBookProvider
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
    defaultBookProvider: DefaultBookProvider
) : ViewModel() {

    private val _bookId: StateFlow<String> = defaultBookProvider.defaultBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    // 预算列表
    val budgets: StateFlow<List<Budget>> = _bookId.flatMapLatest { bookId ->
        if (bookId.isEmpty()) flowOf(emptyList())
        else budgetRepository.getActiveBudgets(bookId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalBudget: StateFlow<Long> = budgets.map { list ->
        list.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val totalUsed: StateFlow<Long> = combine(_bookId, budgets) { bookId, _ ->
        if (bookId.isEmpty()) 0L
        else transactionRepository.getTotalExpense(bookId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val categoryBudgets: StateFlow<List<CategoryBudgetDisplay>> = combine(_bookId, budgets) { bookId, budgetList ->
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

        budgetList.mapNotNull { budget ->
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
        }
    }

    fun updateBudget(budgetId: String, amount: Long) {
        viewModelScope.launch {
            val existing = budgets.value.find { it.id == budgetId }
            if (existing != null) {
                val updated = existing.copy(amount = amount)
                budgetRepository.updateBudget(updated)
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val budget = budgets.value.find { it.id == budgetId }
            if (budget != null) {
                budgetRepository.deleteBudget(budget)
            }
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
