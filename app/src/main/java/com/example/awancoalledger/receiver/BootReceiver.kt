package com.example.awancoalledger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.awancoalledger.data.LedgerDatabase
import com.example.awancoalledger.data.LedgerRepository
import com.example.awancoalledger.utils.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val database = LedgerDatabase.getDatabase(context)
            val repository = LedgerRepository(database.ledgerDao())
            val scheduler = ReminderScheduler(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val reminders = repository.getAllReminders().first()
                reminders.filter { !it.isCompleted }.forEach {
                    scheduler.schedule(it)
                }
            }
        }
    }
}
