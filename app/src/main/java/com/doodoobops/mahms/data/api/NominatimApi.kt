package com.doodoobops.mahms.data.api

import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.utils.GeoUtils
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Client for the free OpenStreetMap Nominatim reverse-geocoding REST API.
 *
 *   GET https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=..&lon=..
 *
 * Turns the GPS point of an SOS alert into a readable address for the responder. No API key is
 * needed, but Nominatim's usage policy requires an identifying User-Agent and at most one
 * request per second, which is fine here because it is called once per SOS press.
 * Any failure (offline, timeout, bad reply) returns null so the SOS alert is still sent.
 */
object NominatimApi {

    private const val AREA = "Nominatim"
    private const val BASE = "https://nominatim.openstreetmap.org/reverse"
    private const val USER_AGENT = "MAHMS-Android/1.0 (student project, Rosebank College)"

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("$BASE?format=jsonv2&zoom=18&lat=$latitude&lon=$longitude")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "application/json")
                }
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    AppLog.w(AREA, "Reverse geocoding returned HTTP $code")
                    null
                } else {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    GeoUtils.parseDisplayName(body).also {
                        AppLog.i(AREA, if (it != null) "Address found" else "Reply had no address")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w(AREA, "Reverse geocoding failed", e)
                null
            } finally {
                connection?.disconnect()
            }
        }
}
