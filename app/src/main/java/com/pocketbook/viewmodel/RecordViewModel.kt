package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Account
import com.pocketbook.data.entity.Insight
import com.pocketbook.data.entity.Transaction
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.AccountRepository
import com.pocketbook.repository.TransactionRepository
import com.pocketbook.service.InsightEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val insightEngine: InsightEngine,
    defaultBookProvider: DefaultBookProvider
) : ViewModel() {

    private val _bookId: StateFlow<String> = defaultBookProvider.defaultBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val accounts: StateFlow<List<Account>> = accountRepository.getActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createTransaction(
        amountStr: String,
        type: TransactionType,
        categoryId: String?,
        accountId: String?,
        date: Long,
        note: String?,
        onComplete: (List<Insight>) -> Unit
    ) {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isEmpty()) return@launch

            val amountInCents = (amountStr.toDoubleOrNull() ?: 0.0) * 100

            val transaction = Transaction(
                bookId = bookId,
                type = type,
                amount = amountInCents.toLong(),
                categoryId = categoryId,
                accountId = accountId,
                date = date,
                note = note
            )

            transactionRepository.createTransaction(transaction)

            // Update account balance
            accountId?.let { accId ->
                val delta = if (type == TransactionType.EXPENSE) -amountInCents else amountInCents
                accountRepository.adjustBalance(accId, delta)
            }

            // Generate insights
            val insights = insightEngine.generateInsights(bookId, transaction)
            onComplete(insights)
        }
    }
}
