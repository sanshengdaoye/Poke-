package com.pocketbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketbook.data.entity.Account
import com.pocketbook.data.entity.AccountType
import com.pocketbook.data.entity.Transaction
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.di.DefaultBookProvider
import com.pocketbook.repository.AccountRepository
import com.pocketbook.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    defaultBookProvider: DefaultBookProvider
) : ViewModel() {

    private val _bookId: StateFlow<String> = defaultBookProvider.defaultBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    // 所有账户（不区分bookId，Repository层没有按bookId过滤的方法）
    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalBalance: StateFlow<Long> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()

    val accountTransactions: StateFlow<List<Transaction>> = combine(
        _selectedAccount,
        _bookId
    ) { account, bookId ->
        if (account == null || bookId.isEmpty()) emptyList()
        else {
            transactionRepository.getTransactionsByBookAndDateRange(bookId, 0, Long.MAX_VALUE)
                .filter { it.accountId == account.id }
                .sortedByDescending { it.date }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createAccount(name: String, type: AccountType) {
        viewModelScope.launch {
            val account = Account(
                name = name,
                type = type,
                balance = 0
            )
            accountRepository.createAccount(account)
        }
    }

    fun updateAccount(accountId: String, name: String, type: AccountType) {
        viewModelScope.launch {
            val existing = accounts.value.find { it.id == accountId }
            if (existing != null) {
                val updated = existing.copy(name = name, type = type)
                accountRepository.updateAccount(updated)
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            val account = accounts.value.find { it.id == accountId }
            if (account != null) {
                accountRepository.deleteAccount(account)
            }
            if (_selectedAccount.value?.id == accountId) {
                _selectedAccount.value = null
            }
        }
    }

    fun selectAccount(account: Account) {
        _selectedAccount.value = if (_selectedAccount.value?.id == account.id) null else account
    }

    fun clearSelection() {
        _selectedAccount.value = null
    }
}
