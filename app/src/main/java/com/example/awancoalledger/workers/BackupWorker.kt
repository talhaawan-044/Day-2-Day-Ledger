package com.example.awancoalledger.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.awancoalledger.data.LedgerDatabase
import com.example.awancoalledger.data.LedgerRepository
import com.example.awancoalledger.utils.DataExchangeUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("BackupWorker", "Starting auto-backup snapshot...")
            
            // 1. Get Repository
            val database = LedgerDatabase.getDatabase(applicationContext)
            val repository = LedgerRepository(database.ledgerDao())
            
            // 2. Collect Data
            val backupData = repository.getBackupData()
            val json = DataExchangeUtils.serializeBackup(backupData)
            
            // 3. Prepare Directory
            val backupDir = File(applicationContext.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // 4. Save File
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "snapshot_$timestamp.json"
            val file = File(backupDir, fileName)
            file.writeText(json)
            
            Log.d("BackupWorker", "Snapshot saved: $fileName (${file.length()} bytes)")
            
            // 5. Cleanup old backups (Keep last 5)
            DataExchangeUtils.cleanupBackups(applicationContext)
            
            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Error during auto-backup", e)
            Result.retry()
        }
    }
}
