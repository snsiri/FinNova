package com.example.finnova.finNova.ui.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.finnova.finNova.data.PreferenceManager

class TransactionsViewModelFactory(
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionsViewModel(preferenceManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 