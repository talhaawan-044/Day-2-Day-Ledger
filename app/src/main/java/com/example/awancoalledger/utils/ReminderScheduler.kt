package com.example.awancoalledger.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.awancoalledger.data.Reminder
import com.example.awancoalledger.data.ReminderPriority
import com.example.awancoalledger.receiver.ReminderReceiver

class ReminderScheduler(private val context: Context) {

    private val alarmService = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(reminder: Reminder) {
        val am = alarmService ?: return
        val rTime = reminder.remindTime ?: return
        
        if (rTime < System.currentTimeMillis() || reminder.isCompleted) {
            Log.d("ReminderScheduler", "Skipping schedule for reminder: ${reminder.title}. Time: $rTime, Now: ${System.currentTimeMillis()}, Completed: ${reminder.isCompleted}")
            return
        }

        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("REMINDER_TITLE", reminder.title)
                putExtra("REMINDER_NOTE", reminder.note)
                putExtra("REMINDER_ID", reminder.id)
                putExtra("REMINDER_PRIORITY", reminder.priority.name)
                putExtra("SNOOZE_COUNT", 0)
                Log.d("ReminderScheduler", "Preparing intent for reminder: ${reminder.id}, Title: ${reminder.title}, Priority: ${reminder.priority}")
                action = "com.example.awancoalledger.ACTION_REMINDER_${reminder.id}"
                setPackage(context.packageName)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, flags)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, rTime, pendingIntent)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, rTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, rTime, pendingIntent)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, rTime, pendingIntent)
            }
            Log.d("ReminderScheduler", "Scheduled: ${reminder.title} at $rTime")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule", e)
        }
    }

    /**
     * Schedule a snooze re-fire for a reminder.
     *
     * @param reminderId     The reminder's database ID
     * @param title          Reminder title
     * @param note           Reminder note
     * @param priority       Priority level
     * @param snoozeCount    How many times this reminder has already been snoozed
     * @param delayMs        Delay in milliseconds from now
     */
    fun scheduleSnooze(
        reminderId: Int,
        title: String,
        note: String?,
        priority: ReminderPriority,
        snoozeCount: Int,
        delayMs: Long
    ) {
        val am = alarmService ?: return
        val triggerTime = System.currentTimeMillis() + delayMs

        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("REMINDER_TITLE", title)
                putExtra("REMINDER_NOTE", note)
                putExtra("REMINDER_ID", reminderId)
                putExtra("REMINDER_PRIORITY", priority.name)
                putExtra("SNOOZE_COUNT", snoozeCount + 1)
                action = "com.example.awancoalledger.ACTION_REMINDER_${reminderId}"
                setPackage(context.packageName)
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            // Use a distinct request code for snooze to avoid collision with the original alarm
            val pendingIntent = PendingIntent.getBroadcast(context, reminderId + 5000, intent, flags)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }

            Log.d("ReminderScheduler", "Snooze scheduled: ID=$reminderId, snoozeCount=${snoozeCount + 1}, delay=${delayMs}ms")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule snooze", e)
        }
    }

    fun cancel(reminder: Reminder) {
        val am = alarmService ?: return
        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.awancoalledger.ACTION_REMINDER_${reminder.id}"
                setPackage(context.packageName)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, flags)
            am.cancel(pendingIntent)

            // Also cancel any pending snooze alarm
            val snoozePendingIntent = PendingIntent.getBroadcast(context, reminder.id + 5000, intent, flags)
            am.cancel(snoozePendingIntent)

            Log.d("ReminderScheduler", "Cancelled: ${reminder.id}")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel", e)
        }
    }
}
