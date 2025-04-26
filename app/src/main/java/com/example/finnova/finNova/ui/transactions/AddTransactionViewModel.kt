package com.example.finnova.finNova.ui.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.finnova.finNova.data.PreferenceManager
import com.example.finnova.finNova.data.Transaction
import java.util.UUID

class AddTransactionViewModel(private val preferenceManager: PreferenceManager) : ViewModel() {
    private val _saveResult = MutableLiveData<SaveResult>()
    val saveResult: LiveData<SaveResult> = _saveResult

    fun addTransaction(title: String, amount: Double, category: String, type: Transaction.Type) {
        try {
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                title = title,
                amount = amount,
                category = category,
                type = type,
                date = System.currentTimeMillis()
            )
            preferenceManager.addTransaction(transaction)
            _saveResult.value = SaveResult.Success
        } catch (e: Exception) {
            _saveResult.value = SaveResult.Error(e.message ?: "Failed to save transaction")
        }
    }

    sealed interface SaveResult {
        data object Success : SaveResult
        data class Error(val message: String) : SaveResult
    }
} 