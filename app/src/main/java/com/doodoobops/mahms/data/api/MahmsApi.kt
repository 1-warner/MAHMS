package com.doodoobops.mahms.data.api

import com.doodoobops.mahms.utils.AppLog
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Client for the team's own REST API (see the backend/ folder in this repository).
 *
 * The API is the second online service the app uses, alongside Firebase. Firebase stays the
 * identity provider: this client takes the signed-in user's Firebase ID token and sends it as
 * `Authorization: Bearer <idToken>`, and the server verifies it before doing anything. No
 * password ever reaches the API.
 *
 * Room stays the source of truth the screens read from, so the app keeps working offline.
 * Calls from here are a mirror of what was already saved locally: a failure is logged and
 * swallowed rather than shown to the user or allowed to lose their data.
 *
 * Written with HttpURLConnection and org.json, the same way as [NominatimApi], so the app
 * needs no extra networking library.
 */
class MahmsApi(
    private val baseUrl: String,
    private val auth: FirebaseAuth,
) {
    /** False when no base URL was configured at build time, which disables every call. */
    val isConfigured: Boolean = baseUrl.isNotBlank()

    /** Result of one call. [Failure] carries enough to log, never enough to leak a token. */
    sealed interface Result {
        data class Success(val body: JSONObject?, val array: JSONArray? = null) : Result
        data class Failure(val code: Int, val message: String) : Result
    }

    // ------------------------------------------------------------------ endpoints

    /**
     * GET /health. Open endpoint, so it works even when nobody is signed in.
     * Settings uses it for the "Server status" line.
     */
    suspend fun isServerUp(): Boolean {
        if (!isConfigured) return false
        return when (val r = request("GET", "/health", body = null, authenticated = false)) {
            is Result.Success -> r.body?.optString("status") == "ok"
            is Result.Failure -> false
        }
    }

    /** GET /api/me. The account, its role and the patient or doctor record linked to it. */
    suspend fun me(): JSONObject? = (request("GET", "/api/me") as? Result.Success)?.body

    /**
     * POST /api/patients/me. Makes sure the signed-in account has a patient record on the
     * server, so the appointment and SOS calls below have something to attach to.
     */
    suspend fun ensurePatientRecord(
        clientRef: Long,
        firstName: String,
        lastName: String,
        phone: String,
    ): Boolean {
        val body = JSONObject()
            .put("clientRef", clientRef.toString())
            .put("firstName", firstName)
            .put("lastName", lastName)
            .put("phone", phone)
        return request("POST", "/api/patients/me", body) is Result.Success
    }

    /**
     * POST /api/appointments. [clientRef] is the Room appointmentId, which the server stores
     * as a unique key, so sending the same booking twice updates one row instead of making two.
     */
    suspend fun bookAppointment(
        clientRef: Long,
        doctorRef: Long,
        date: String,
        time: String,
        reason: String,
        status: String,
    ): Result {
        val body = JSONObject()
            .put("clientRef", clientRef.toString())
            .put("doctorId", doctorRef.toString())
            .put("appointmentDate", date)
            .put("appointmentTime", time)
            .put("reason", reason)
            .put("status", status)
        return request("POST", "/api/appointments", body)
    }

    /** POST /api/emergencies. The SOS alert, with the GPS point when the phone had one. */
    suspend fun raiseEmergency(
        clientRef: Long,
        location: String,
        latitude: Double?,
        longitude: Double?,
        description: String,
    ): Result {
        val body = JSONObject()
            .put("clientRef", clientRef.toString())
            .put("location", location)
            .put("description", description)
        if (latitude != null && longitude != null) {
            body.put("latitude", latitude).put("longitude", longitude)
        }
        return request("POST", "/api/emergencies", body)
    }

    /**
     * POST /api/payments. Returns the receipt number the server issued, or null when the call
     * did not get through, in which case the app keeps the number it generated locally.
     */
    suspend fun recordPayment(
        clientRef: Long,
        appointmentRef: Long,
        amount: Double,
        method: String,
        date: String,
        status: String,
    ): String? {
        val body = JSONObject()
            .put("clientRef", clientRef.toString())
            .put("appointmentId", appointmentRef.toString())
            .put("amount", amount)
            .put("paymentMethod", method)
            .put("paymentDate", date)
            .put("status", status)
        val result = request("POST", "/api/payments", body)
        return (result as? Result.Success)?.body?.optStringOrNull("receipt_no")
    }

    /** GET /api/receipts/{paymentRef}. The receipt the server holds for a payment. */
    suspend fun receipt(paymentRef: Long): JSONObject? =
        (request("GET", "/api/receipts/$paymentRef") as? Result.Success)?.body

    // ------------------------------------------------------------------ plumbing

    /** The Firebase ID token for the signed-in user, or null when nobody is signed in. */
    private suspend fun idToken(): String? {
        val user = auth.currentUser ?: return null
        return try {
            user.getIdToken(false).await().token
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(AREA, "Could not get a sign-in token", e)
            null
        }
    }

    /**
     * Performs one request and reads the reply. Never throws: every failure comes back as
     * [Result.Failure] so a repository can carry on with the record it already saved to Room.
     */
    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true,
    ): Result = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.Failure(0, "No API base URL is configured")
        }

        val token = if (authenticated) idToken() else null
        if (authenticated && token == null) {
            return@withContext Result.Failure(0, "Not signed in")
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(baseUrl.trimEnd('/') + path)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }

            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                // The server sends { "error": "..." } for anything it refuses.
                val message = runCatching { JSONObject(text).optString("error") }
                    .getOrNull()
                    .orEmpty()
                    .ifBlank { "HTTP $code" }
                AppLog.w(AREA, "$method $path failed: $code $message")
                return@withContext Result.Failure(code, message)
            }

            AppLog.i(AREA, "$method $path -> $code")
            when {
                text.isBlank() -> Result.Success(null)
                text.trimStart().startsWith("[") -> Result.Success(null, JSONArray(text))
                else -> Result.Success(JSONObject(text))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // Offline, DNS failure, timeout. Expected on a phone, so logged at warning level.
            AppLog.w(AREA, "$method $path could not reach the server: ${e.message}")
            Result.Failure(0, e.message ?: "Network error")
        } catch (e: Exception) {
            AppLog.w(AREA, "$method $path failed unexpectedly", e)
            Result.Failure(0, e.message ?: "Unexpected error")
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val AREA = "MahmsApi"
        const val TIMEOUT_MS = 10_000
        const val USER_AGENT = "MAHMS-Android/1.0"
    }
}

/** org.json turns a JSON null into the string "null", which is never what we want here. */
private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}
