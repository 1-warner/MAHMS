package com.doodoobops.mahms.utils

import android.content.Context

/**
 * Lightweight session state via SharedPreferences:
 *  - is_registered drives first-time (Register) vs returning (Login) routing
 *  - the logged-in user's id/role/name for the active session
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

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

    /** Patient record (patients.patientId) that belongs to the signed-in Patient, or -1. */
    var currentPatientId: Long
        get() = prefs.getLong(KEY_PATIENT_ID, -1L)
        private set(value) = prefs.edit().putLong(KEY_PATIENT_ID, value).apply()

    /** Doctor record (doctors.doctorId) that belongs to the signed-in Doctor, or -1. */
    var currentDoctorId: Long
        get() = prefs.getLong(KEY_DOCTOR_ID, -1L)
        private set(value) = prefs.edit().putLong(KEY_DOCTOR_ID, value).apply()

    /** Settings toggle: when false, medication reminder notifications are suppressed. */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    val isLoggedIn: Boolean get() = currentUserId >= 0

    fun startSession(userId: Long, role: String, name: String, patientId: Long = -1L, doctorId: Long = -1L) {
        currentUserId = userId
        currentRole = role
        currentName = name
        currentPatientId = patientId
        currentDoctorId = doctorId
    }

    fun endSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_ROLE)
            .remove(KEY_NAME)
            .remove(KEY_PATIENT_ID)
            .remove(KEY_DOCTOR_ID)
            .apply()
    }

    companion object {
        const val PREFS = "mahms_session"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_REGISTERED = "is_registered"
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_ROLE = "current_role"
        private const val KEY_NAME = "current_name"
        private const val KEY_PATIENT_ID = "current_patient_id"
        private const val KEY_DOCTOR_ID = "current_doctor_id"
    }
}
