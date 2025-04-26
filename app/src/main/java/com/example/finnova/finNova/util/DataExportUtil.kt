package com.example.finnova.finNova.util

import android.content.Context
import android.net.Uri
import com.example.finnova.finNova.data.PreferenceManager
import com.example.finnova.finNova.data.Transaction
import com.google.gson.GsonBuilder
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class DataExportUtil(private val context: Context) {
    private val preferenceManager = PreferenceManager(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    fun exportToJson(uri: Uri): Boolean {
        return try {
            val transactions = preferenceManager.getTransactions()
            val jsonData = gson.toJson(transactions)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonData)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportToText(uri: Uri): Boolean {
        return try {
            val transactions = preferenceManager.getTransactions()
            val stringBuilder = StringBuilder()
            
            // Add header
            stringBuilder.append("Transaction Report\n")
            stringBuilder.append("Generated: ${dateFormat.format(Date())}\n\n")
            stringBuilder.append("ID | Date | Type | Title | Amount | Category\n")
            stringBuilder.append("-".repeat(60))
            stringBuilder.append("\n")

            // Add transactions
            transactions.forEach { transaction ->
                stringBuilder.append(
                    "${transaction.id} | " +
                    "${dateFormat.format(Date(transaction.date))} | " +
                    "${transaction.type} | " +
                    "${transaction.title} | " +
                    "${formatAmount(transaction)} | " +
                    "${transaction.category}\n"
                )
            }

            // Add summary
            stringBuilder.append("\nSummary:\n")
            stringBuilder.append("-".repeat(20))
            stringBuilder.append("\n")
            
            val totalIncome = transactions
                .filter { it.type == Transaction.Type.INCOME }
                .sumOf { it.amount }
            
            val totalExpense = transactions
                .filter { it.type == Transaction.Type.EXPENSE }
                .sumOf { it.amount }

            stringBuilder.append("Total Income: ${formatAmount(totalIncome)}\n")
            stringBuilder.append("Total Expense: ${formatAmount(totalExpense)}\n")
            stringBuilder.append("Net Balance: ${formatAmount(totalIncome - totalExpense)}\n")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(stringBuilder.toString())
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun formatAmount(transaction: Transaction): String {
        val prefix = if (transaction.type == Transaction.Type.INCOME) "+" else "-"
        return "$prefix$${String.format(Locale.getDefault(), "%.2f", transaction.amount)}"
    }

    private fun formatAmount(amount: Double): String {
        return "$${String.format(Locale.getDefault(), "%.2f", amount)}"
    }
} 