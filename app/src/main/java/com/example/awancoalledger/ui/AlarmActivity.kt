package com.example.awancoalledger.ui

import androidx.compose.material3.MaterialTheme
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.LedgerDatabase
import com.example.awancoalledger.data.LedgerRepository
import com.example.awancoalledger.data.ReminderPriority
import com.example.awancoalledger.ui.theme.AwanCoalLedgerTheme
import com.example.awancoalledger.ui.theme.ErrorRed
import com.example.awancoalledger.utils.AlarmSoundManager
import com.example.awancoalledger.utils.NotificationHelper
import com.example.awancoalledger.utils.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lock screen & turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Acquire a wake lock to prevent the device from sleeping
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AwanCoalLedger:AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max

        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Reminder"
        val note = intent.getStringExtra("REMINDER_NOTE") ?: ""
        val id = intent.getIntExtra("REMINDER_ID", 0)
        val priorityName = intent.getStringExtra("REMINDER_PRIORITY")
        val priority = ReminderPriority.entries.find { it.name == priorityName } ?: ReminderPriority.HIGH
        val snoozeCount = intent.getIntExtra("SNOOZE_COUNT", 0)

        // Start alarm sound & vibration
        AlarmSoundManager.start(this, priority)

        setContent {
            AwanCoalLedgerTheme(darkTheme = true) {
                AlarmScreen(
                    title = title,
                    note = note,
                    reminderId = id,
                    priority = priority,
                    snoozeCount = snoozeCount,
                    onDismiss = {
                        dismissAlarm(id)
                    },
                    onSnooze = {
                        snoozeAlarm(id, title, note, priority, snoozeCount)
                    }
                )
            }
        }
    }

    private fun dismissAlarm(id: Int) {
        AlarmSoundManager.stop()
        val notificationHelper = NotificationHelper(this)
        notificationHelper.cancelNotification(id)

        val database = LedgerDatabase.getDatabase(this)
        val repository = LedgerRepository(database.ledgerDao())
        val scheduler = ReminderScheduler(this)

        CoroutineScope(Dispatchers.IO).launch {
            val reminder = database.ledgerDao().getReminderById(id)
            if (reminder != null) {
                repository.completeReminder(reminder, scheduler)
            }
        }
        finish()
    }

    private fun snoozeAlarm(id: Int, title: String, note: String, priority: ReminderPriority, snoozeCount: Int) {
        AlarmSoundManager.stop()
        val notificationHelper = NotificationHelper(this)
        notificationHelper.cancelNotification(id)

        val scheduler = ReminderScheduler(this)
        val delayMs = if (priority == ReminderPriority.HIGH) 2 * 60 * 1000L else 5 * 60 * 1000L

        scheduler.scheduleSnooze(
            reminderId = id,
            title = title,
            note = note,
            priority = priority,
            snoozeCount = snoozeCount,
            delayMs = delayMs
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    // Prevent back button from dismissing the alarm screen
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing — user must tap Dismiss or Snooze
    }
}

@Composable
fun AlarmScreen(
    title: String,
    note: String,
    reminderId: Int,
    priority: ReminderPriority,
    snoozeCount: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    // Pulsing animation for the alarm icon ring
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Subtle background glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Icon shake animation
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    val currentTime = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    val accentColor = when (priority) {
        ReminderPriority.HIGH -> ErrorRed
        ReminderPriority.MEDIUM -> Color(0xFFFF9500)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = glowAlpha),
                        Color(0xFF0A0A0A),
                        Color.Black
                    ),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─── Top: Time ───
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Text(
                    text = currentTime,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
            }

            // ─── Center: Alarm Icon + Title ───
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing rings behind the icon
                Box(contentAlignment = Alignment.Center) {
                    // Outer pulse ring
                    Surface(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(pulseScale * 1.2f)
                            .alpha(pulseAlpha * 0.5f),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f)
                    ) {}

                    // Inner pulse ring
                    Surface(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.25f)
                    ) {}

                    // Main icon circle
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f),
                        tonalElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .size(64.dp)
                                    .offset(x = shakeOffset.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Priority badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = when (priority) {
                            ReminderPriority.HIGH -> "🔴  URGENT"
                            ReminderPriority.MEDIUM -> "🟠  IMPORTANT"
                            else -> "REMINDER"
                        },
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // Title
                Text(
                    text = title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Note
                if (note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = note,
                        fontSize = 17.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Snooze count indicator
                if (snoozeCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Snoozed $snoozeCount time${if (snoozeCount > 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            // ─── Bottom: Action Buttons ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DISMISS button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        Icons.Outlined.AlarmOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "DISMISS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }

                // SNOOZE button
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Color.White.copy(alpha = 0.25f)
                    )
                ) {
                    Icon(
                        Icons.Outlined.Snooze,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (priority == ReminderPriority.HIGH) "SNOOZE  ·  2 MIN" else "SNOOZE  ·  5 MIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
