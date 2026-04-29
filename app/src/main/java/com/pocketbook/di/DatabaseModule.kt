package com.pocketbook.di

import android.content.Context
import com.pocketbook.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideBookDao(database: AppDatabase) = database.bookDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase) = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase) = database.categoryDao()

    @Provides
    fun provideAccountDao(database: AppDatabase) = database.accountDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase) = database.budgetDao()

    @Provides
    fun provideInsightDao(database: AppDatabase) = database.insightDao()

    @Provides
    fun provideUserPreferencesDao(database: AppDatabase) = database.userPreferencesDao()
}
