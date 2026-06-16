package com.example.awancoalledger.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.awancoalledger.data.ReminderPriority

/**
 * Singleton manager for alarm sounds and vibration.
 * Plays at STREAM_ALARM volume so HIGH-priority reminders cut through
 * silent/vibrate mode just like a real alarm clock.
 */
object AlarmSoundManager {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isPlaying = false

    /**
     * Start alarm sound + vibration appropriate for the given priority.
     * - HIGH: Loud looping alarm at alarm-stream volume + aggressive continuous vibration
     * - MEDIUM: Single alarm tone at notification volume + repeating vibration pattern
     * - LOW: Short vibration buzz only
     * - NONE: Nothing
     */
    fun start(context: Context, priority: ReminderPriority) {
        stop() // Clean up any previous playback

        vibrator = getVibrator(context)

        when (priority) {
            ReminderPriority.HIGH -> {
                startSound(context, isLooping = true, streamType = AudioManager.STREAM_ALARM)
                startVibration(priority)
            }
            ReminderPriority.MEDIUM -> {
                startSound(context, isLooping = false, streamType = AudioManager.STREAM_NOTIFICATION)
                startVibration(priority)
            }
            ReminderPriority.LOW -> {
                startVibration(priority)
            }
            ReminderPriority.NONE -> { /* Completely silent */ }
        }

        isPlaying = true
    }

    /** Stop all sound and vibration immediately. */
    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Error stopping media player", e)
        }
        mediaPlayer = null

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Error cancelling vibrator", e)
        }
        vibrator = null
        isPlaying = false
    }

    fun isActive(): Boolean = isPlaying

    // ────────────────────────────────────────────
    // Internal helpers
    // ────────────────────────────────────────────

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun startSound(context: Context, isLooping: Boolean, streamType: Int) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            if (streamType == AudioManager.STREAM_ALARM)
                                AudioAttributes.USAGE_ALARM
                            else
                                AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                this.isLooping = isLooping
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Failed to start alarm sound", e)
        }
    }

    private fun startVibration(priority: ReminderPriority) {
        val vib = vibrator ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (priority) {
                    ReminderPriority.HIGH -> {
                        // Aggressive pattern: 0ms wait, 800ms on, 200ms off — repeats
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 800, 200, 800, 200, 800),
                            intArrayOf(0, 255, 0, 255, 0, 255),
                            0 // repeat from index 0
                        )
                    }
                    ReminderPriority.MEDIUM -> {
                        // Noticeable pattern: buzz-pause-buzz, repeats
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 500, 300, 500, 1000),
                            intArrayOf(0, 200, 0, 200, 0),
                            0 // repeat
                        )
                    }
                    ReminderPriority.LOW -> {
                        // Single short buzz, no repeat
                        VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    ReminderPriority.NONE -> return
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (priority) {
                    ReminderPriority.HIGH -> vib.vibrate(longArrayOf(0, 800, 200, 800, 200, 800), 0)
                    ReminderPriority.MEDIUM -> vib.vibrate(longArrayOf(0, 500, 300, 500, 1000), 0)
                    ReminderPriority.LOW -> vib.vibrate(300)
                    ReminderPriority.NONE -> {}
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Failed to start vibration", e)
        }
    }
}
