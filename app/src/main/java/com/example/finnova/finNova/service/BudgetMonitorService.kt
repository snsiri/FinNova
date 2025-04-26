package com.example.finnova.finNova.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.finnova.R
import com.example.finnova.finNova.data.PreferenceManager
import com.example.finnova.finNova.ui.dashboard.DashboardActivity
import com.example.finnova.finNova.util.NotificationHelper
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class BudgetMonitorService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var notificationHelper: NotificationHelper

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "budget_monitor_service"
    }

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors your budget status"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Budget Monitor Active")
            .setContentText("Monitoring your budget status")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                try {
                    checkBudgetStatus()
                    // Check every 6 hours
                    delay(TimeUnit.HOURS.toMillis(6))
                } catch (e: Exception) {
                    Log.e("BudgetMonitorService", "Error monitoring budget: ${e.message}")
                }
            }
        }
    }

    private fun checkBudgetStatus() {
        try {
            val monthlyBudget = preferenceManager.getMonthlyBudget()
            val monthlyExpenses = preferenceManager.getMonthlyExpenses()

            if (monthlyBudget <= 0) return

            notificationHelper.checkBudgetStatus(monthlyExpenses, monthlyBudget)
        } catch (e: Exception) {
            Log.e("BudgetMonitorService", "Error checking budget status: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
} 