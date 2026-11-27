package com.aibox.launcher.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import java.util.Locale

/**
 * GPS без Google Play Services: системный LocationManager.
 * Данные приходят только после разрешения ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION,
 * которое запрашивается из лаунчера при включении режима «по GPS».
 */
class GpsLocationSource(context: Context) {

    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var callback: ((Location) -> Unit)? = null
    private var registered = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            callback?.invoke(location)
        }

        override fun onProviderDisabled(provider: String) = Unit
        override fun onProviderEnabled(provider: String) = Unit

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    fun hasPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            appContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun currentLocation(): Location? {
        if (!hasPermission()) return null
        return providers()
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    /** Подписываемся на обновления не чаще раза в минуту и не точнее 50 метров. */
    fun startUpdates(onLocation: (Location) -> Unit): Boolean {
        if (!hasPermission()) return false
        callback = onLocation
        var started = false
        providers().forEach { provider ->
            val ok = runCatching {
                manager.requestLocationUpdates(
                    provider,
                    60_000L,
                    50f,
                    listener,
                    Looper.getMainLooper()
                )
            }.isSuccess
            if (ok) started = true
        }
        registered = started
        if (!started) callback = null
        return started
    }

    fun stopUpdates() {
        if (registered) {
            runCatching { manager.removeUpdates(listener) }
            registered = false
        }
        callback = null
    }

    /** Человекочитаемое имя места для виджетов и строки состояния погоды. */
    fun describe(location: Location): String {
        val geocoder = Geocoder(appContext, Locale("ru"))
        val place = runCatching {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        }.getOrNull()?.firstOrNull()

        return place?.locality
            ?: place?.subAdminArea
            ?: place?.adminArea
            ?: String.format(Locale.getDefault(), "%.3f, %.3f", location.latitude, location.longitude)
    }

    private fun providers(): List<String> = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER
    ).filter { provider ->
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
    }
}
