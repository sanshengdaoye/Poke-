package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Transaction
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.CategoryRepository
import com.pocketbook.repository.TransactionRepository
import com.pocketbook.ui.screens.CalendarDayItem
import com.pocketbook.ui.screens.CategoryBreakdownItem
import com.pocketbook.ui.screens.StatsUiState
import com.pocketbook.ui.screens.TrendDataItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
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

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _transactions.collect { txs ->
                updateStats(txs)
            }
        }
    }

    private suspend fun updateStats(transactions: List<Transaction>) {
        val bookId = _bookId.value
        if (bookId.isEmpty()) return

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val startOfMonth = getStartOfMonth(year, month)
        val endOfMonth = getEndOfMonth(year, month)

        val monthTransactions = transactions.filter { it.date in startOfMonth..endOfMonth }

        val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val dailyAvg = if (today > 0) expense / today else 0
        val savingRate = if (income > 0) (income - expense).toFloat() / income.toFloat() else 0f

        // 分类排行
        val expenseByCategory = monthTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val categoryBreakdown = expenseByCategory.map { (catId, amount) ->
            val category = categoryRepository.getCategoryById(catId)
            val totalExpense = expense.coerceAtLeast(1)
            CategoryBreakdownItem(
                categoryName = category?.name ?: "未分类",
                emoji = getCategoryEmoji(category?.name ?: "未分类"),
                amount = amount,
                percent = amount.toFloat() / totalExpense.toFloat(),
                transactionCount = monthTransactions.count { it.categoryId == catId }
            )
        }

        // 趋势数据（最近7天）
        val trendData = (6 downTo 0).map { dayOffset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_MONTH, -dayOffset)
            val start = getStartOfDay(cal)
            val end = getEndOfDay(cal)
            val dayTxs = transactions.filter { it.date in start..end }
            val dayIncome = dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val dayExpense = dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            TrendDataItem(
                label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}",
                income = dayIncome,
                expense = dayExpense
            )
        }

        // 日历数据
        val calendarData = (1..daysInMonth).map { day ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, day)
            val start = getStartOfDay(cal)
            val end = getEndOfDay(cal)
            val dayTxs = transactions.filter { it.date in start..end && it.type == TransactionType.EXPENSE }
            val dayExpense = dayTxs.sumOf { it.amount }
            CalendarDayItem(
                day = day,
                amount = dayExpense,
                hasData = dayExpense > 0
            )
        }

        _uiState.value = StatsUiState(
            monthIncome = income,
            monthExpense = expense,
            dailyAverageExpense = dailyAvg,
            savingRate = savingRate.coerceIn(0f, 1f),
            totalTransactionCount = transactions.size,
            categoryBreakdown = categoryBreakdown,
            trendData = trendData,
            calendarData = calendarData
        )
    }

    private fun getStartOfMonth(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonth(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    private fun getStartOfDay(cal: Calendar = Calendar.getInstance()): Long {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(cal: Calendar = Calendar.getInstance()): Long {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
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
