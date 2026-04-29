package com.pocketbook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketbook.data.dao.*
import com.pocketbook.data.entity.*
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        Book::class,
        Transaction::class,
        Category::class,
        Account::class,
        Budget::class,
        Insight::class,
        UserPreferences::class,
        Tag::class,
        RecurringTransaction::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun insightDao(): InsightDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun tagDao(): TagDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 创建 Account 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        balance INTEGER NOT NULL DEFAULT 0,
                        icon TEXT,
                        color INTEGER,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 2. 创建 Tag 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tags (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 3. 创建 RecurringTransaction 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_transactions (
                        id TEXT PRIMARY KEY NOT NULL,
                        templateTransactionId TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        interval INTEGER NOT NULL DEFAULT 1,
                        nextTriggerDate INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        endDate INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (templateTransactionId) REFERENCES transactions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_templateTransactionId ON recurring_transactions(templateTransactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_isActive ON recurring_transactions(isActive)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_nextTriggerDate ON recurring_transactions(nextTriggerDate)")

                // 4. Transaction 表新增列：accountId, tagIds, isRecurring
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN tagIds TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")

                // 5. 为 Transaction 新增 accountId 索引
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val passphrase = net.sqlcipher.database.SQLiteDatabase.getBytes("jiyi_secure_key_2024".toCharArray())
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "pocketbook_encrypted.db"
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedWithSql(db)
                    }
                })
                .build()
        }

        private fun seedWithSql(db: SupportSQLiteDatabase) {
            db.beginTransaction()
            try {
                val cursor = db.query("SELECT COUNT(*) FROM categories WHERE isPreset = 1")
                val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                cursor.close()

                if (count == 0) {
                    val expenseCategories = listOf(
                        "餐饮" to 1, "交通" to 2, "购物" to 3, "娱乐" to 4,
                        "医疗" to 5, "教育" to 6, "住房" to 7, "通讯" to 8,
                        "人情" to 9, "其他支出" to 10
                    )
                    expenseCategories.forEach { (name, order) ->
                        db.execSQL(
                            "INSERT INTO categories (id, name, type, sortOrder, isPreset, createdAt) VALUES (?, ?, 'EXPENSE', ?, 1, ?)",
                            arrayOf(java.util.UUID.randomUUID().toString(), name, order, System.currentTimeMillis())
                        )
                    }

                    val incomeCategories = listOf(
                        "工资" to 1, "奖金" to 2, "投资" to 3,
                        "兼职" to 4, "红包" to 5, "其他收入" to 6
                    )
                    incomeCategories.forEach { (name, order) ->
                        db.execSQL(
                            "INSERT INTO categories (id, name, type, sortOrder, isPreset, createdAt) VALUES (?, ?, 'INCOME', ?, 1, ?)",
                            arrayOf(java.util.UUID.randomUUID().toString(), name, order, System.currentTimeMillis())
                        )
                    }

                    db.execSQL(
                        "INSERT INTO books (id, name, currency, isDefault, createdAt, updatedAt) VALUES (?, ?, 'CNY', 1, ?, ?)",
                        arrayOf(java.util.UUID.randomUUID().toString(), "默认账本", System.currentTimeMillis(), System.currentTimeMillis())
                    )
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
