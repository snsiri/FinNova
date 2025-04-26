package com.example.finnova.finNova.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ExpenseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showExpenseReminder()
    }
} 