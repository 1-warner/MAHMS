package com.doodoobops.mahms.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.doodoobops.mahms.utils.AppLog
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Device GPS for the Emergency SOS button, using Android's built-in LocationManager
 * (free, no extra library or API key).
 */
object LocationProvider {

    private const val AREA = "Location"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * Asks the GPS (then the network provider) for a fresh fix, waiting up to [timeoutMs] for each.
     * Falls back to the newest last-known location. Returns null without permission or signal.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context, timeoutMs: Long = 8_000L): Location? {
        if (!hasPermission(context)) {
            AppLog.w(AREA, "Location permission not granted")
            return null
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val enabled = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }

        for (provider in enabled) {
            val fix: Location? = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    try {
                        LocationManagerCompat.getCurrentLocation(
                            manager, provider, signal, ContextCompat.getMainExecutor(context),
                        ) { location -> if (cont.isActive) cont.resume(location) }
                    } catch (e: SecurityException) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
            if (fix != null) {
                AppLog.i(AREA, "Fresh fix from $provider")
                return fix
            }
        }

        val lastKnown = listOf(
            LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        AppLog.i(AREA, if (lastKnown != null) "Using last known location" else "No location available")
        return lastKnown
    }
}
