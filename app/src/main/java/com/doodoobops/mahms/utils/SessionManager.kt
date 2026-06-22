package com.doodoobops.mahms.utils

import android.content.Context

/**
 * Lightweight session state via SharedPreferences:
 *  - is_registered drives first-time (Register) vs returning (Login) routing
 *  - the logged-in user's id/role/name for the active session
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("mahms_session", Context.MODE_PRIVATE)

    var isRegistered: Boolean
        get() = prefs.getBoolean(KEY_REGISTERED, false)
        set(value) = prefs.edit().putBoolean(KEY_REGISTERED, value).apply()

    var currentUserId: Long
        get() = prefs.getLong(KEY_USER_ID, -1L)
        private set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var currentRole: String?
        get() = prefs.getString(KEY_ROLE, null)
        private set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var currentName: String?
        get() = prefs.getString(KEY_NAME, null)
        private set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    val isLoggedIn: Boolean get() = currentUserId >= 0

    fun startSession(userId: Long, role: String, name: String) {
        currentUserId = userId
        currentRole = role
        currentName = name
    }

    fun endSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_ROLE)
            .remove(KEY_NAME)
            .apply()
    }

    companion object {
        private const val KEY_REGISTERED = "is_registered"
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_ROLE = "current_role"
        private const val KEY_NAME = "current_name"
    }
}
