package com.tinnomore.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tinnomore.MainActivity
import com.tinnomore.R
import com.tinnomore.data.NotchSettingsStore
import com.tinnomore.util.GlobalNotchEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Servicio en foreground que mantiene activo el notch sobre el audio GLOBAL
 * del dispositivo (todas las apps), no solo el de TinNoMore.
 *
 * Por qué un foreground service y no algo más liviano:
 *  - El [android.media.audiofx.DynamicsProcessing] enganchado a audioSession=0
 *    vive mientras exista el proceso que lo creó. Si Android mata el proceso
 *    en background (lo cual hace agresivamente sin foreground service), el
 *    efecto se libera y el notch global desaparece sin que el usuario se
 *    entere.
 *  - Requiere notificación persistente (Android obliga a esto desde API 26)
 *    para que quede claro que hay un proceso corriendo en background.
 *
 * Se inicia/detiene desde GlobalNotchViewModel según el toggle en Ajustes /
 * pantalla de terapia, y también se relanza automáticamente si estaba
 * activado y el sistema reinicia el proceso (ver START_STICKY).
 */
class GlobalNotchService : Service() {

    companion object {
        const val CHANNEL_ID = "global_notch_channel"
        const val NOTIFICATION_ID = 4821

        const val EXTRA_FREQ_HZ = "freq_hz"
        const val EXTRA_DEPTH_DB = "depth_db"

        fun start(context: Context, freqHz: Int, depthDb: Float) {
            val intent = Intent(context, GlobalNotchService::class.java).apply {
                putExtra(EXTRA_FREQ_HZ, freqHz)
                putExtra(EXTRA_DEPTH_DB, depthDb)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GlobalNotchService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob())
    private var settingsJob: Job? = null
    private lateinit var settingsStore: NotchSettingsStore

    override fun onCreate() {
        super.onCreate()
        settingsStore = NotchSettingsStore(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val freq = intent?.getIntExtra(EXTRA_FREQ_HZ, 4000) ?: 4000
        val depth = intent?.getFloatExtra(EXTRA_DEPTH_DB, -24f) ?: -24f

        startForeground(NOTIFICATION_ID, buildNotification(freq))

        if (GlobalNotchEffect.isSupported) {
            GlobalNotchEffect.start(freq, depth)
        }

        // Escucha cambios posteriores de frecuencia/profundidad/activación
        // (ej. el usuario cambia la frecuencia del notch mientras el modo
        // global sigue activo) y reconfigura el efecto en caliente.
        settingsJob?.cancel()
        settingsJob = scope.launch {
            combine(
                settingsStore.enabled,
                settingsStore.freqHz,
                settingsStore.depthDb
            ) { enabled, hz, db -> Triple(enabled, hz, db) }
                .distinctUntilChanged()
                .collect { (enabled, hz, db) ->
                    if (!enabled) {
                        stopSelf()
                        return@collect
                    }
                    GlobalNotchEffect.update(hz, db)
                    updateNotification(hz)
                }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        scope.cancel()
        GlobalNotchEffect.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Terapia de notch (global)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indica que el filtro notch está aplicándose a todo el audio del dispositivo"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(freqHz: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Notch activo en todo el audio")
            .setContentText("Filtrando ${freqHz} Hz en cualquier app que reproduzca sonido")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(freqHz: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(freqHz))
    }
}
