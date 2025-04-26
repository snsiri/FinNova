package com.example.finnova.finNova.ui.transaction

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finnova.R
import com.example.finnova.finNova.data.PreferenceManager
import com.example.finnova.finNova.data.Transaction
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModel() {
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            try {
                val allTransactions = preferenceManager.getTransactions()
                _transactions.value = allTransactions.sortedByDescending { it.date }
            } catch (e: Exception) {
                e.printStackTrace()
                _transactions.value = emptyList()
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                preferenceManager.addTransaction(transaction)
                loadTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                preferenceManager.updateTransaction(transaction)
                loadTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                preferenceManager.deleteTransaction(transaction)
                loadTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getCategories(): List<String> {
        return try {
            context.resources.getStringArray(R.array.transaction_categories).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
} 