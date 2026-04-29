package com.pocketbook.service

import android.content.Context
import android.os.Environment
import com.pocketbook.data.entity.Transaction
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportService @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend fun exportToCSV(bookId: String, context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val transactions = transactionRepository.getTransactionsByBook(bookId).firstOrNull() ?: emptyList()

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "jiyi_export_${formatDateForFile(System.currentTimeMillis())}.csv"
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                writer.append("Date,Type,Amount,Category,Account,Note
")
                transactions.forEach { tx ->
                    writer.append("${formatDate(tx.date)},${tx.type},${tx.amount / 100.0},${tx.categoryId},${tx.accountId},${tx.note}
")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToJSON(bookId: String, context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val transactions = transactionRepository.getTransactionsByBook(bookId).firstOrNull() ?: emptyList()

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "jiyi_export_${formatDateForFile(System.currentTimeMillis())}.json"
            val file = File(downloadsDir, fileName)

            val jsonBuilder = StringBuilder()
            jsonBuilder.append("[
")
            transactions.forEachIndexed { index, tx ->
                jsonBuilder.append("  {
")
                jsonBuilder.append("    "id": "${tx.id}",
")
                jsonBuilder.append("    "date": ${tx.date},
")
                jsonBuilder.append("    "type": "${tx.type}",
")
                jsonBuilder.append("    "amount": ${tx.amount},
")
                jsonBuilder.append("    "categoryId": "${tx.categoryId}",
")
                jsonBuilder.append("    "accountId": "${tx.accountId}",
")
                jsonBuilder.append("    "note": "${tx.note?.replace(""", "\"")}"
")
                jsonBuilder.append("  }")
                if (index < transactions.size - 1) jsonBuilder.append(",")
                jsonBuilder.append("
")
            }
            jsonBuilder.append("]
")

            file.writeText(jsonBuilder.toString())
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDateForFile(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
