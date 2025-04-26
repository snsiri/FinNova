package com.example.finnova.finNova.ui.budget

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.finnova.finNova.data.PreferenceManager
import com.example.finnova.finNova.service.BudgetMonitorService
import com.example.finnova.finNova.util.NotificationHelper

class BudgetViewModel(
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModel() {
    private val _budget = MutableLiveData<Double>(0.0)
    val budget: LiveData<Double> = _budget
    private val notificationHelper = NotificationHelper(context)

    init {
        loadBudget()
    }

    private fun loadBudget() {
        try {
            _budget.value = preferenceManager.getMonthlyBudget()
        } catch (e: Exception) {
            e.printStackTrace()
            _budget.value = 0.0
        }
    }

    fun updateBudget(newBudget: Double) {
        try {
            preferenceManager.saveMonthlyBudget(newBudget)
            _budget.value = newBudget
            
            // Start budget monitoring service
            val serviceIntent = Intent(context, BudgetMonitorService::class.java)
            context.startService(serviceIntent)
            
            // Show immediate notification if needed
            checkBudgetStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkBudgetStatus() {
        val monthlyBudget = preferenceManager.getMonthlyBudget()
        val monthlyExpenses = getMonthlyExpenses()
        notificationHelper.checkBudgetStatus(monthlyExpenses, monthlyBudget)
    }

    fun getMonthlyExpenses(): Double {
        return try {
            preferenceManager.getMonthlyExpenses()
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }

    class Factory(
        private val preferenceManager: PreferenceManager,
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BudgetViewModel(preferenceManager, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 