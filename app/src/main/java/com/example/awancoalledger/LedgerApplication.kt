package com.example.awancoalledger

import android.app.Application
import com.example.awancoalledger.data.LedgerDatabase
import com.example.awancoalledger.data.LedgerRepository
import com.example.awancoalledger.data.SettingsRepository
import com.example.awancoalledger.utils.NotificationHelper
import androidx.work.*
import com.example.awancoalledger.workers.BackupWorker
import java.util.concurrent.TimeUnit

class LedgerApplication : Application() {
    val database by lazy { LedgerDatabase.getDatabase(this) }
    val repository by lazy { LedgerRepository(database.ledgerDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper(this).initNotificationChannels()
        scheduleAutoBackup()
    }

    private fun scheduleAutoBackup() {
        val backupWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(6, TimeUnit.HOURS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AutoBackupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            backupWorkRequest
        )
    }
}
