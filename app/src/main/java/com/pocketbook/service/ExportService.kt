package com.pocketbook.service

import android.content.Context
import android.os.Environment
import com.pocketbook.data.entity.Transaction
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
                writer.append("Date,Type,Amount,Category,Account,Note\n")
                transactions.forEach { tx ->
                    val note = tx.note ?: ""
                    writer.append("${formatDate(tx.date)},${tx.type},${tx.amount / 100.0},${tx.categoryId ?: ""},${tx.accountId ?: ""},$note\n")
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

            val sb = StringBuilder()
            sb.append("[\n")
            transactions.forEachIndexed { index, tx ->
                sb.append("  {\n")
                sb.append("    \"id\": \"${tx.id}\",\n")
                sb.append("    \"date\": ${tx.date},\n")
                sb.append("    \"type\": \"${tx.type}\",\n")
                sb.append("    \"amount\": ${tx.amount},\n")
                sb.append("    \"categoryId\": \"${tx.categoryId ?: ""}\",\n")
                sb.append("    \"accountId\": \"${tx.accountId ?: ""}\",\n")
                sb.append("    \"note\": \"${tx.note?.replace("\"", "\\\"") ?: ""}\"\n")
                sb.append("  }")
                if (index < transactions.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("]\n")
            file.writeText(sb.toString())
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
