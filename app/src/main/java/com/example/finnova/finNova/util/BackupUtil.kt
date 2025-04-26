package com.example.finnova.finNova.util

import android.content.Context
import android.net.Uri
import com.example.finnova.finNova.data.Transaction
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupUtil(private val context: Context) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun createBackup(uri: Uri, transactions: List<Transaction>, monthlyBudget: Double, currency: String): Boolean {
        return try {
            val backupData = mapOf(
                "transactions" to transactions,
                "budget" to monthlyBudget,
                "currency" to currency,
                "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            
            val jsonData = gson.toJson(backupData)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonData.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreFromBackup(uri: Uri): Map<String, Any>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val backupData = gson.fromJson(reader, Map::class.java)
                    backupData as Map<String, Any>
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 