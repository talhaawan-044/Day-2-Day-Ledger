package com.example.awancoalledger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.awancoalledger.data.LedgerDatabase
import com.example.awancoalledger.data.LedgerRepository
import com.example.awancoalledger.data.ReminderPriority
import com.example.awancoalledger.utils.AlarmSoundManager
import com.example.awancoalledger.utils.NotificationHelper
import com.example.awancoalledger.utils.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        /** MEDIUM priority: auto-snooze every 5 minutes, up to 3 times */
        private const val MEDIUM_SNOOZE_DELAY_MS = 5 * 60 * 1000L   // 5 min
        private const val MEDIUM_MAX_SNOOZE = 3

        /** HIGH priority: auto-snooze every 2 minutes, unlimited until dismissed */
        private const val HIGH_SNOOZE_DELAY_MS = 2 * 60 * 1000L     // 2 min
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("REMINDER_ID", 0)
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Reminder"
        val note = intent.getStringExtra("REMINDER_NOTE")
        val priorityName = intent.getStringExtra("REMINDER_PRIORITY")
        val priority = ReminderPriority.entries.find { it.name == priorityName } ?: ReminderPriority.MEDIUM
        val snoozeCount = intent.getIntExtra("SNOOZE_COUNT", 0)

        Log.d("ReminderReceiver", "Received: id=$id, title=$title, action=${intent.action}, priority=$priority, snoozeCount=$snoozeCount")

        val notificationHelper = NotificationHelper(context)
        val database = LedgerDatabase.getDatabase(context)
        val repository = LedgerRepository(database.ledgerDao())
        val scheduler = ReminderScheduler(context)

        when (intent.action) {
            // ── User pressed "Mark Done" ──
            NotificationHelper.ACTION_DONE -> {
                Log.d("ReminderReceiver", "ACTION_DONE for reminder $id")
                // Stop any alarm sound/vibration
                AlarmSoundManager.stop()
                // Cancel notification
                notificationHelper.cancelNotification(id)
                // Mark as completed in database
                CoroutineScope(Dispatchers.IO).launch {
                    val reminder = database.ledgerDao().getReminderById(id)
                    if (reminder != null) {
                        repository.completeReminder(reminder, scheduler)
                    }
                }
            }

            // ── User pressed "Snooze" ──
            NotificationHelper.ACTION_SNOOZE -> {
                Log.d("ReminderReceiver", "ACTION_SNOOZE for reminder $id, count=$snoozeCount")
                // Stop alarm sound/vibration
                AlarmSoundManager.stop()
                // Cancel current notification
                notificationHelper.cancelNotification(id)

                // Determine delay based on priority
                val delayMs = when (priority) {
                    ReminderPriority.HIGH -> HIGH_SNOOZE_DELAY_MS
                    ReminderPriority.MEDIUM -> MEDIUM_SNOOZE_DELAY_MS
                    else -> MEDIUM_SNOOZE_DELAY_MS
                }

                // Schedule re-fire
                scheduler.scheduleSnooze(
                    reminderId = id,
                    title = title,
                    note = note,
                    priority = priority,
                    snoozeCount = snoozeCount,
                    delayMs = delayMs
                )
            }

            // ── Alarm fired (initial or re-fire from snooze) ──
            else -> {
                Log.d("ReminderReceiver", "Alarm fired for reminder $id, priority=$priority")

                // Show the notification (which triggers full-screen for HIGH)
                notificationHelper.showReminderNotification(id, title, note, priority, snoozeCount)

                // For MEDIUM: schedule auto-snooze if under limit
                if (priority == ReminderPriority.MEDIUM && snoozeCount < MEDIUM_MAX_SNOOZE) {
                    scheduler.scheduleSnooze(
                        reminderId = id,
                        title = title,
                        note = note,
                        priority = priority,
                        snoozeCount = snoozeCount,
                        delayMs = MEDIUM_SNOOZE_DELAY_MS
                    )
                }

                // For HIGH: always auto-snooze (fires indefinitely until user dismisses)
                if (priority == ReminderPriority.HIGH) {
                    scheduler.scheduleSnooze(
                        reminderId = id,
                        title = title,
                        note = note,
                        priority = priority,
                        snoozeCount = snoozeCount,
                        delayMs = HIGH_SNOOZE_DELAY_MS
                    )
                }
            }
        }
    }
}
