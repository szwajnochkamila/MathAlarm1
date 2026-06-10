package com.example.mathalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.mathalarm.data.MathPreferencesManager

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { // uzytkownik rozwiazal zadanie
            stopAlarm() // zatrzymuje alarm (muzyke)
            stopSelf() // zatrzymuje sam siebie
            return START_NOT_STICKY
        }

        startForegroundWithNotification() // powiadomienie
        acquireWakeLock() // blokada (wygaszenie)
        startSound()
        startVibration()
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        createChannel()

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Budzik")
            .setContentText("Kliknij, aby wyłączyć")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPi)
            .setFullScreenIntent(fullScreenPi, true) // !!!
            // wysoki priorytet zeby otworzyl (ekran z AlarmActivity) nawet przy wygaszonym ekranie
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel( // tworzymy kanal powiadomien
                CHANNEL_ID,
                "Budzik",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Dzwoniące budziki"
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true) // ignorujemy tryb nie przeszkadzac
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    // WakeLock zapobiega wejscu CPU w stan glebokiego uspienia
    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MathAlarm::AlarmServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
            // przy dzwonieniu budzika ekran ma pracowac przez max 10 min
        }
    }

    private fun startSound() {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            // 1. Polaczenie z baza ustawien
            val preferencesManager = MathPreferencesManager(applicationContext)
            val settings = preferencesManager.mathSettingsFlow.first()

            // 2. Sprawdzenie, czy jest zapisany wlasny dzwonek, jak nie to szuka systemowego
            var alarmUri: Uri? = null

            if (!settings.customRingtoneUri.isNullOrEmpty()) {
                alarmUri = Uri.parse(settings.customRingtoneUri)
            } else {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            if (alarmUri == null) return@launch

            // 3. Wlaczanie muzyki
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM) // zeby uzyl glosnosci budzika a nie glosnosci multimediow
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmService, alarmUri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        } // zatrzymuje odtwarzacz
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.runCatching { if (isHeld) release() } // zwalnia blokade procesora
        wakeLock = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.example.mathalarm.ACTION_STOP"

        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmService::class.java).setAction(ACTION_STOP)
    }
}