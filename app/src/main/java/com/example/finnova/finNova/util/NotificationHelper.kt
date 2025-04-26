package com.example.finnova.finNova.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.finnova.R
import com.example.finnova.finNova.ui.dashboard.DashboardActivity
import java.text.NumberFormat
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_BUDGET = "budget_notifications"
        private const val CHANNEL_ID_REMINDER = "expense_reminders"
        private const val NOTIFICATION_ID_BUDGET = 1
        private const val NOTIFICATION_ID_REMINDER = 2
        const val CHANNEL_ID = "expense_reminders"
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_NAME = "Expense Reminders"
        private const val CHANNEL_DESCRIPTION = "Daily reminders to record your expenses"
        
        // Budget warning thresholds
        private const val CRITICAL_THRESHOLD = 100
        private const val HIGH_WARNING_THRESHOLD = 90
        private const val MEDIUM_WARNING_THRESHOLD = 80
        private const val LOW_WARNING_THRESHOLD = 70
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                CHANNEL_ID_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for budget status"
                enableVibration(true)
                enableLights(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Expense Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to record expenses"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(budgetChannel)
            notificationManager?.createNotificationChannel(reminderChannel)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun checkBudgetStatus(currentExpenses: Double, monthlyBudget: Double) {
        if (!hasNotificationPermission() || monthlyBudget <= 0) {
            return
        }

        val percentageUsed = (currentExpenses / monthlyBudget * 100).toInt()
        val remaining = monthlyBudget - currentExpenses

        when {
            percentageUsed >= CRITICAL_THRESHOLD -> {
                showBudgetWarning(
                    "Budget Exceeded!",
                    "You've exceeded your budget by ${formatCurrency(currentExpenses - monthlyBudget)}",
                    NotificationCompat.PRIORITY_MAX,
                    true
                )
            }
            percentageUsed >= HIGH_WARNING_THRESHOLD -> {
                showBudgetWarning(
                    "Critical Budget Warning",
                    "You've used $percentageUsed% of your budget! Only ${formatCurrency(remaining)} remaining",
                    NotificationCompat.PRIORITY_HIGH,
                    true
                )
            }
            percentageUsed >= MEDIUM_WARNING_THRESHOLD -> {
                showBudgetWarning(
                    "Budget Warning",
                    "You've used $percentageUsed% of your monthly budget. ${formatCurrency(remaining)} remaining",
                    NotificationCompat.PRIORITY_HIGH,
                    true
                )
            }
            percentageUsed >= LOW_WARNING_THRESHOLD -> {
                showBudgetWarning(
                    "Budget Notice",
                    "You've used $percentageUsed% of your monthly budget. ${formatCurrency(remaining)} remaining",
                    NotificationCompat.PRIORITY_DEFAULT,
                    false
                )
            }
        }
    }

    private fun showBudgetWarning(
        title: String,
        message: String,
        priority: Int,
        useVibration: Boolean
    ) {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (useVibration) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
        }

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BUDGET,
                notificationBuilder.build()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showExpenseReminder() {
        if (!hasNotificationPermission()) {
            return
        }
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Record Your Expenses")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
        } catch (e: Exception) {
            e.printStackTrace()
            "$0.00"
        }
    }
} 