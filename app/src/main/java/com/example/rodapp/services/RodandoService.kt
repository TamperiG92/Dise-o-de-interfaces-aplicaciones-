package com.example.rodapp.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.rodapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RodandoService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var prevLocation: Location? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var notifManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notifManager = getSystemService(NotificationManager::class.java)
        crearCanal()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification(getString(R.string.notif_rodando_titulo), "0.0 km"))
        RodandoEstado.activa.value = true
        RodandoEstado.distanciaMetros.value = 0f
        RodandoEstado.tiempoSegundos.value = 0L
        RodandoEstado.velocidadKmh.value = 0f
        prevLocation = null
        startTimer()
        startLocationUpdates()
        return START_STICKY
    }

    private fun startTimer() {
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                RodandoEstado.tiempoSegundos.value++
                if (RodandoEstado.tiempoSegundos.value % 5 == 0L) {
                    val km = "%.1f".format(RodandoEstado.distanciaMetros.value / 1000f)
                    notifManager.notify(NOTIF_ID, buildNotification(getString(R.string.notif_rodando_titulo), "$km km"))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    prevLocation?.let { prev ->
                        RodandoEstado.distanciaMetros.value += prev.distanceTo(loc)
                    }
                    RodandoEstado.velocidadKmh.value = loc.speed * 3.6f
                    prevLocation = loc
                }
            }
        }

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        scope.cancel()
        if (::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
        RodandoEstado.activa.value = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_rodando_canal),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_rodando_canal_desc)
                setShowBadge(false)
            }
            notifManager.createNotificationChannel(canal)
        }
    }

    private fun buildNotification(title: String, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        const val NOTIF_ID = 2001
        const val CHANNEL_ID = "RODANDO_CHANNEL"
    }
}
