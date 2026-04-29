package com.pocketbook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketbook.data.dao.*
import com.pocketbook.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        Book::class,
        Transaction::class,
        Category::class,
        Account::class,
        Budget::class,
        Insight::class,
        UserPreferences::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun insightDao(): InsightDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val passphrase = SQLiteDatabase.getBytes("jiyi_secure_key_2024".toCharArray())
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "pocketbook_encrypted.db"
            )
                .openHelperFactory(factory)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.seedPresets()
                        }
                    }
                })
                .build()
        }
    }

    suspend fun seedPresets() {
        if (categoryDao().getPresetCount() > 0) return

        val expenseCategories = listOf(
            Category(name = "餐饮", type = CategoryType.EXPENSE, icon = "dining", sortOrder = 1, isPreset = true),
            Category(name = "交通", type = CategoryType.EXPENSE, icon = "transport", sortOrder = 2, isPreset = true),
            Category(name = "购物", type = CategoryType.EXPENSE, icon = "shopping", sortOrder = 3, isPreset = true),
            Category(name = "娱乐", type = CategoryType.EXPENSE, icon = "entertainment", sortOrder = 4, isPreset = true),
            Category(name = "医疗", type = CategoryType.EXPENSE, icon = "medical", sortOrder = 5, isPreset = true),
            Category(name = "教育", type = CategoryType.EXPENSE, icon = "education", sortOrder = 6, isPreset = true),
            Category(name = "住房", type = CategoryType.EXPENSE, icon = "housing", sortOrder = 7, isPreset = true),
            Category(name = "通讯", type = CategoryType.EXPENSE, icon = "communication", sortOrder = 8, isPreset = true),
            Category(name = "人情", type = CategoryType.EXPENSE, icon = "social", sortOrder = 9, isPreset = true),
            Category(name = "其他支出", type = CategoryType.EXPENSE, icon = "other", sortOrder = 10, isPreset = true)
        )

        val incomeCategories = listOf(
            Category(name = "工资", type = CategoryType.INCOME, icon = "salary", sortOrder = 1, isPreset = true),
            Category(name = "奖金", type = CategoryType.INCOME, icon = "bonus", sortOrder = 2, isPreset = true),
            Category(name = "投资", type = CategoryType.INCOME, icon = "investment", sortOrder = 3, isPreset = true),
            Category(name = "兼职", type = CategoryType.INCOME, icon = "parttime", sortOrder = 4, isPreset = true),
            Category(name = "红包", type = CategoryType.INCOME, icon = "gift", sortOrder = 5, isPreset = true),
            Category(name = "其他收入", type = CategoryType.INCOME, icon = "other_income", sortOrder = 6, isPreset = true)
        )

        categoryDao().insertAll(expenseCategories + incomeCategories)

        val defaultBook = Book(name = "默认账本", isDefault = true)
        bookDao().insert(defaultBook)

        val defaultPrefs = UserPreferences(
            theme = ThemeMode.SYSTEM,
            currency = "CNY",
            reminderTime = "21:00"
        )
        userPreferencesDao().insert(defaultPrefs)
    }
}
