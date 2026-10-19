package com.doodoobops.mahms.utils

import android.util.Log

/**
 * One place for app logging. Every line carries the area it came from, e.g. "[Auth] Login ok".
 * Filter in Logcat with:  tag:MAHMS
 *
 * Never log passwords, tokens or full medical details. Log ids and outcomes only.
 */
object AppLog {
    private const val TAG = "MAHMS"

    fun d(area: String, message: String) {
        Log.d(TAG, "[$area] $message")
    }

    fun i(area: String, message: String) {
        Log.i(TAG, "[$area] $message")
    }

    fun w(area: String, message: String, error: Throwable? = null) {
        if (error != null) Log.w(TAG, "[$area] $message", error) else Log.w(TAG, "[$area] $message")
    }

    fun e(area: String, message: String, error: Throwable? = null) {
        if (error != null) Log.e(TAG, "[$area] $message", error) else Log.e(TAG, "[$area] $message")
    }
}
