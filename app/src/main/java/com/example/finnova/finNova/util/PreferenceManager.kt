package com.example.finnova.finNova.util

import android.content.Context
import android.content.SharedPreferences
import com.example.finnova.finNova.model.Transaction
import com.example.finnova.finNova.model.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Calendar
import java.util.Date
import androidx.core.content.edit

class PreferenceManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "FinNovaPrefs"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_TRANSACTIONS = "transactions"
    }

    fun saveMonthlyBudget(budget: Double) {
        sharedPreferences.edit { putFloat(KEY_MONTHLY_BUDGET, budget.toFloat()) }
    }

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }

    fun saveCurrency(currency: String) {
        sharedPreferences.edit { putString(KEY_CURRENCY, currency) }
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "$") ?: "$"
    }

    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit { putString(KEY_TRANSACTIONS, json) }
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addTransaction(transaction: Transaction) {
        val currentTransactions = getTransactions().toMutableList()
        currentTransactions.add(transaction)
        saveTransactions(currentTransactions)
    }

    fun deleteTransaction(transactionId: String) {
        val currentTransactions = getTransactions().toMutableList()
        currentTransactions.removeIf { it.id == transactionId }
        saveTransactions(currentTransactions)
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        val currentTransactions = getTransactions().toMutableList()
        val index = currentTransactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            currentTransactions[index] = updatedTransaction
            saveTransactions(currentTransactions)
        }
    }

    fun getMonthlyExpenses(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        return getTransactions()
            .filter {
                it.type == TransactionType.EXPENSE &&
                Calendar.getInstance().apply { time = it.date }.get(Calendar.MONTH) == currentMonth
            }
            .sumOf { it.amount }
    }

    // Backup and Restore methods
    fun getBackupFiles(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir.listFiles { file -> 
            file.name.startsWith("FinNova_Backup_") && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun createBackup(backupData: String): Boolean {
        try {
            val dateFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val fileName = "FinNova_Backup_${dateFormat.format(Date())}.json"
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val backupFile = File(backupDir, fileName)
            FileWriter(backupFile).use { writer ->
                writer.write(backupData)
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun restoreFromBackup(backupFile: File): Boolean {
        try {
            val backupData = backupFile.readText()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data = gson.fromJson<Map<String, Any>>(backupData, type)

            data["transactions"]?.let {
                val transactions = gson.fromJson<List<Transaction>>(
                    gson.toJson(it),
                    object : TypeToken<List<Transaction>>() {}.type
                )
                saveTransactions(transactions)
            }

            data["budget"]?.let {
                saveMonthlyBudget((it as Number).toDouble())
            }

            data["currency"]?.let {
                saveCurrency(it as String)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
} 