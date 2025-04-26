package com.example.finnova.finNova.util

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ExpenseReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showExpenseReminder()
        return Result.success()
    }
} 