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

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    val totalBalance: StateFlow<Long> = _accounts.map { list ->
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

    init {
        viewModelScope.launch {
            _bookId.collect { bookId ->
                if (bookId.isNotEmpty()) {
                    _accounts.value = accountRepository.getAccountsByBook(bookId)
                        .sortedByDescending { it.isDefault }
                }
            }
        }
    }

    fun createAccount(name: String, type: AccountType) {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isEmpty()) return@launch

            val account = Account(
                name = name,
                type = type,
                balance = 0
            )
            accountRepository.createAccount(account)
            _accounts.value = accountRepository.getAccountsByBook(bookId)
                .sortedByDescending { it.isDefault }
        }
    }

    fun updateAccount(accountId: String, name: String, type: AccountType) {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isEmpty()) return@launch

            val existing = _accounts.value.find { it.id == accountId }
            if (existing != null) {
                val updated = existing.copy(name = name, type = type)
                accountRepository.updateAccount(updated)
                _accounts.value = accountRepository.getAccountsByBook(bookId)
                    .sortedByDescending { it.isDefault }
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            val bookId = _bookId.value
            if (bookId.isEmpty()) return@launch

            accountRepository.deleteAccount(accountId)
            _accounts.value = accountRepository.getAccountsByBook(bookId)
                .sortedByDescending { it.isDefault }

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
