package com.doodoobops.mahms.data.cloud

import com.doodoobops.mahms.data.db.MahmsDatabase
import com.doodoobops.mahms.utils.AppLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps the local Room data and Cloud Firestore in step, in BOTH directions.
 *
 * - Room stays the source of truth the UI reads from (fast, works offline).
 * - Push: every write goes to Room first, then is pushed here fire-and-forget. Firestore queues
 *   writes offline and retries, so a missing connection never blocks or crashes the UI.
 * - Pull: [pullAll] downloads every collection and upserts it into Room, so changes made on
 *   another phone or in the Firebase console reach this phone. [syncAll] pulls, then pushes.
 * - Nothing is pushed or pulled unless a Firebase user is signed in (matches the security rules).
 *
 * Collections: patients, doctors, appointments, prescriptions, emergencies, payments,
 * medication_reminders, staff.  (User profiles live in `users/{uid}`, written by
 * FirebaseAuthRepository.)
 */
class CloudSync(
    private val db: MahmsDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var syncRunning = false

    fun push(collection: String, id: Long, data: Map<String, Any?>) {
        if (auth.currentUser == null) return
        firestore.collection(collection).document(id.toString()).set(data)
            .addOnFailureListener { AppLog.w(TAG, "Push failed: $collection/$id", it) }
    }

    fun delete(collection: String, id: Long) {
        if (auth.currentUser == null) return
        firestore.collection(collection).document(id.toString()).delete()
            .addOnFailureListener { AppLog.w(TAG, "Delete failed: $collection/$id", it) }
    }

    /**
     * Pull then push, in the background. Called after each sign-in and from Settings > Sync now.
     * [includeStaffData] is false for patients: the security rules only let hospital staff write
     * the doctors and staff collections, so a patient's phone does not try to.
     */
    fun syncAll(includeStaffData: Boolean) {
        if (auth.currentUser == null) return
        if (syncRunning) {
            AppLog.d(TAG, "Sync already running, skipped")
            return
        }
        syncRunning = true
        scope.launch {
            try {
                val pulled = pullAll()
                pushAll(includeStaffData)
                AppLog.i(TAG, "Sync finished, $pulled records pulled")
            } finally {
                syncRunning = false
            }
        }
    }

    /** Uploads every local table. */
    suspend fun pushAll(includeStaffData: Boolean = true) {
        if (auth.currentUser == null) return
        try {
            db.patientDao().getAll().first().forEach { push("patients", it.patientId, it.toCloudMap()) }
            db.appointmentDao().getAll().first().forEach { push("appointments", it.appointmentId, it.toCloudMap()) }
            db.prescriptionDao().getAll().first().forEach { push("prescriptions", it.prescriptionId, it.toCloudMap()) }
            db.emergencyDao().getActive().first().forEach { push("emergencies", it.emergencyId, it.toCloudMap()) }
            db.emergencyDao().getResolved().first().forEach { push("emergencies", it.emergencyId, it.toCloudMap()) }
            db.paymentDao().getAll().first().forEach { push("payments", it.paymentId, it.toCloudMap()) }
            db.reminderDao().getAllActive().forEach { push("medication_reminders", it.reminderId, it.toCloudMap()) }
            if (includeStaffData) {
                db.doctorDao().getAll().first().forEach { push("doctors", it.doctorId, it.toCloudMap()) }
                db.staffDao().getAll().first().forEach { push("staff", it.staffId, it.toCloudMap()) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "Full upload failed", e)
        }
    }

    /**
     * Downloads every collection and upserts it into Room (the online copy wins for the same id).
     * Parents are loaded before children so foreign keys hold. A bad document is skipped and
     * logged, never fatal. Returns how many records were stored.
     */
    suspend fun pullAll(): Int {
        if (auth.currentUser == null) return 0
        var stored = 0

        suspend fun <T> pull(name: String, convert: (Map<String, Any?>) -> T?, save: suspend (T) -> Unit) {
            val snapshot = try {
                withTimeoutOrNull(15_000) { firestore.collection(name).get().await() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w(TAG, "Could not download $name", e)
                null
            }
            if (snapshot == null) {
                AppLog.w(TAG, "Download of $name timed out or failed")
                return
            }
            for (doc in snapshot.documents) {
                val data: Map<String, Any?> = doc.data ?: continue
                try {
                    val entity = convert(data) ?: continue
                    save(entity)
                    stored++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.w(TAG, "Skipped $name/${doc.id}", e)
                }
            }
        }

        pull("patients", { it.toPatient() }) { db.patientDao().upsert(it) }
        pull("doctors", { it.toDoctor() }) { db.doctorDao().upsert(it) }
        pull("appointments", { it.toAppointment() }) { db.appointmentDao().upsert(it) }
        pull("prescriptions", { it.toPrescription() }) { db.prescriptionDao().upsert(it) }
        pull("emergencies", { it.toEmergency() }) { db.emergencyDao().upsert(it) }
        pull("payments", { it.toPayment() }) { db.paymentDao().upsert(it) }
        pull("medication_reminders", { it.toReminder() }) { db.reminderDao().upsert(it) }
        pull("staff", { it.toStaff() }) { db.staffDao().upsert(it) }
        return stored
    }

    private companion object {
        const val TAG = "CloudSync"
    }
}
