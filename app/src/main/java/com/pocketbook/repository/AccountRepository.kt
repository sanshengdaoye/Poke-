package com.pocketbook.repository

import com.pocketbook.data.dao.AccountDao
import com.pocketbook.data.entity.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getActiveAccounts(): Flow<List<Account>> = accountDao.getActive()

    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAll()

    suspend fun getAccountById(id: String): Account? = accountDao.getById(id)

    suspend fun createAccount(account: Account) = accountDao.insert(account)

    suspend fun updateAccount(account: Account) = accountDao.update(account)

    suspend fun deleteAccount(account: Account) = accountDao.delete(account)

    suspend fun adjustBalance(id: String, delta: Double) = accountDao.adjustBalance(id, delta)
}
