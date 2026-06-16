package com.example.awancoalledger.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.awancoalledger.MainActivity
import com.example.awancoalledger.receiver.ReminderReceiver
import com.example.awancoalledger.ui.AlarmActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_SILENT = "reminder_channel_silent"
        const val CHANNEL_LOW = "reminder_channel_low"
        const val CHANNEL_MED = "reminder_channel_med"
        const val CHANNEL_HIGH = "reminder_channel_high"

        const val ACTION_SNOOZE = "com.example.awancoalledger.ACTION_SNOOZE"
        const val ACTION_DONE = "com.example.awancoalledger.ACTION_DONE"
    }

    fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Silent channel — NONE priority
            val silentChannel =
                    NotificationChannel(
                                    CHANNEL_SILENT,
                                    "Silent Reminders",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                description = "Quiet reminders with no sound or vibration"
                                setSound(null, null)
                                enableVibration(false)
                            }

            // Low channel — standard notification
            val lowChannel =
                    NotificationChannel(
                                    CHANNEL_LOW,
                                    "Low Priority Reminders",
                                    NotificationManager.IMPORTANCE_DEFAULT
                            )
                            .apply { description = "Standard reminders with sound" }

            // Medium channel — heads-up persistent
            val medChannel =
                    NotificationChannel(
                                    CHANNEL_MED,
                                    "Important Reminders",
                                    NotificationManager.IMPORTANCE_HIGH
                            )
                            .apply {
                                description = "Persistent reminders that re-alert automatically"
                                enableLights(true)
                                lightColor = Color.parseColor("#FF9500")
                                enableVibration(true)
                                vibrationPattern = longArrayOf(0, 500, 300, 500)
                                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                            }

            // High channel — alarm-level urgency
            val highChannel =
                    NotificationChannel(
                                    CHANNEL_HIGH,
                                    "Urgent Alarm Reminders",
                                    NotificationManager.IMPORTANCE_HIGH
                            )
                            .apply {
                                description = "Full-screen alarm reminders that cannot be missed"
                                enableLights(true)
                                lightColor = Color.RED
                                enableVibration(true)
                                vibrationPattern = longArrayOf(0, 800, 200, 800, 200, 800)
                                setBypassDnd(true)
                                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                                // Use alarm sound on this channel
                                val alarmUri =
                                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                if (alarmUri != null) {
                                    setSound(
                                            alarmUri,
                                            AudioAttributes.Builder()
                                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                                    .setContentType(
                                                            AudioAttributes
                                                                    .CONTENT_TYPE_SONIFICATION
                                                    )
                                                    .build()
                                    )
                                }
                            }

            notificationManager.createNotificationChannel(silentChannel)
            notificationManager.createNotificationChannel(lowChannel)
            notificationManager.createNotificationChannel(medChannel)
            notificationManager.createNotificationChannel(highChannel)
        }
    }

    fun showReminderNotification(
            id: Int,
            title: String,
            note: String?,
            priority: com.example.awancoalledger.data.ReminderPriority =
                    com.example.awancoalledger.data.ReminderPriority.MEDIUM,
            snoozeCount: Int = 0
    ) {
        // Tap action — open the app to reminders screen
        val tapIntent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("target_screen", "reminders")
                    putExtra("reminder_id", id)
                }
        val tapPendingIntent =
                PendingIntent.getActivity(
                        context,
                        id,
                        tapIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        // "Mark Done" action
        val doneIntent =
                Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_DONE
                    putExtra("REMINDER_ID", id)
                    putExtra("REMINDER_TITLE", title)
                    putExtra("REMINDER_PRIORITY", priority.name)
                }
        val donePendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        id + 2000,
                        doneIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        // "Snooze" action (for MEDIUM and HIGH)
        val snoozeIntent =
                Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_SNOOZE
                    putExtra("REMINDER_ID", id)
                    putExtra("REMINDER_TITLE", title)
                    putExtra("REMINDER_NOTE", note)
                    putExtra("REMINDER_PRIORITY", priority.name)
                    putExtra("SNOOZE_COUNT", snoozeCount)
                }
        val snoozePendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        id + 3000,
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        // Pick channel and notification priority
        val channelId =
                when (priority) {
                    com.example.awancoalledger.data.ReminderPriority.NONE -> CHANNEL_SILENT
                    com.example.awancoalledger.data.ReminderPriority.LOW -> CHANNEL_LOW
                    com.example.awancoalledger.data.ReminderPriority.MEDIUM -> CHANNEL_MED
                    com.example.awancoalledger.data.ReminderPriority.HIGH -> CHANNEL_HIGH
                }

        val notifPriority =
                when (priority) {
                    com.example.awancoalledger.data.ReminderPriority.NONE ->
                            NotificationCompat.PRIORITY_MIN
                    com.example.awancoalledger.data.ReminderPriority.LOW ->
                            NotificationCompat.PRIORITY_DEFAULT
                    com.example.awancoalledger.data.ReminderPriority.MEDIUM ->
                            NotificationCompat.PRIORITY_HIGH
                    com.example.awancoalledger.data.ReminderPriority.HIGH ->
                            NotificationCompat.PRIORITY_MAX
                }

        val contentText =
                when (priority) {
                    com.example.awancoalledger.data.ReminderPriority.HIGH ->
                            "🔴 URGENT: ${note ?: "You have a critical task!"}"
                    com.example.awancoalledger.data.ReminderPriority.MEDIUM -> {
                        val remaining = 3 - snoozeCount
                        if (remaining > 0)
                                "🟠 ${note ?: "Important reminder"} • Will re-alert ${remaining}x"
                        else note ?: "Important reminder"
                    }
                    com.example.awancoalledger.data.ReminderPriority.LOW -> note
                                    ?: "You have a task to complete."
                    com.example.awancoalledger.data.ReminderPriority.NONE -> note ?: "Reminder"
                }

        val builder =
                NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.ic_popup_reminder)
                        .setContentTitle(title)
                        .setContentText(contentText)
                        .setPriority(notifPriority)
                        .setAutoCancel(
                                priority == com.example.awancoalledger.data.ReminderPriority.LOW ||
                                        priority ==
                                                com.example.awancoalledger.data.ReminderPriority
                                                        .NONE
                        )
                        .setContentIntent(tapPendingIntent)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Per-priority behavior
        when (priority) {
            com.example.awancoalledger.data.ReminderPriority.NONE -> {
                // Completely silent
                builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
                builder.setSilent(true)
                builder.addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Dismiss",
                        donePendingIntent
                )
            }
            com.example.awancoalledger.data.ReminderPriority.LOW -> {
                // Standard notification with sound
                builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
                builder.addAction(android.R.drawable.ic_menu_view, "Mark Done", donePendingIntent)
            }
            com.example.awancoalledger.data.ReminderPriority.MEDIUM -> {
                // Persistent — can't be swiped away
                builder.setOngoing(true)
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                builder.addAction(
                        android.R.drawable.ic_menu_recent_history,
                        "Snooze (5 min)",
                        snoozePendingIntent
                )
                builder.addAction(android.R.drawable.ic_menu_view, "Done ✓", donePendingIntent)
                builder.setVibrate(longArrayOf(0, 500, 300, 500))
            }
            com.example.awancoalledger.data.ReminderPriority.HIGH -> {
                // Full-screen alarm
                builder.setOngoing(true)
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                builder.addAction(
                        android.R.drawable.ic_menu_recent_history,
                        "Snooze (5 min)",
                        snoozePendingIntent
                )
                builder.addAction(android.R.drawable.ic_menu_view, "Done ✓", donePendingIntent)
                builder.setVibrate(longArrayOf(0, 800, 200, 800, 200, 800))

                // Full-screen intent — launches AlarmActivity over lock screen
                val alarmIntent =
                        Intent(context, AlarmActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("REMINDER_TITLE", title)
                            putExtra("REMINDER_NOTE", note)
                            putExtra("REMINDER_ID", id)
                            putExtra("REMINDER_PRIORITY", priority.name)
                            putExtra("SNOOZE_COUNT", snoozeCount)
                        }
                val alarmPendingIntent =
                        PendingIntent.getActivity(
                                context,
                                id + 1000,
                                alarmIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                builder.setFullScreenIntent(alarmPendingIntent, true)
            }
        }

        notificationManager.notify(id, builder.build())
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
}
