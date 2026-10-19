package com.doodoobops.mahms.data.repository

import com.doodoobops.mahms.data.api.MahmsApi
import com.doodoobops.mahms.data.cloud.CloudSync
import com.doodoobops.mahms.data.cloud.toCloudMap
import com.doodoobops.mahms.data.db.AppointmentDao
import com.doodoobops.mahms.data.db.DoctorDao
import com.doodoobops.mahms.data.db.EmergencyDao
import com.doodoobops.mahms.data.db.MedicationReminderDao
import com.doodoobops.mahms.data.db.PatientDao
import com.doodoobops.mahms.data.db.PaymentDao
import com.doodoobops.mahms.data.db.PrescriptionDao
import com.doodoobops.mahms.data.db.StaffDao
import com.doodoobops.mahms.data.db.UserDao
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.data.entity.StaffEntity
import com.doodoobops.mahms.data.entity.UserEntity
import com.doodoobops.mahms.domain.model.Demo
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.utils.GeoUtils
import com.doodoobops.mahms.utils.hashPassword
import kotlinx.coroutines.CancellationException

class UserRepository(private val dao: UserDao) {
    suspend fun register(fullName: String, email: String, phone: String, password: String, role: String): Long =
        dao.insert(
            UserEntity(
                fullName = fullName,
                email = email.trim().lowercase(),
                phone = phone,
                passwordHash = hashPassword(password),
                role = role,
            )
        )

    /** Returns the user when email+password match, else null. */
    suspend fun login(email: String, password: String): UserEntity? {
        val user = dao.findByEmail(email.trim().lowercase()) ?: return null
        return if (user.passwordHash == hashPassword(password)) user else null
    }

    suspend fun getById(id: Long) = dao.findById(id)
    suspend fun findByEmail(email: String) = dao.findByEmail(email.trim().lowercase())
    suspend fun count() = dao.count()

    suspend fun updatePassword(id: Long, newPassword: String) =
        dao.updatePassword(id, hashPassword(newPassword))

    /** True when [password] matches the locally cached hash for [id]. */
    suspend fun matchesPassword(id: Long, password: String): Boolean =
        dao.findById(id)?.passwordHash == hashPassword(password)

    /**
     * Cache a Firebase-authenticated user locally so session/role routing works offline.
     * The stored hash is only used for the offline fallback; Firebase holds the real credential.
     * An empty [password] (Google sign-in) keeps any hash already stored.
     */
    suspend fun upsert(fullName: String, email: String, phone: String, password: String, role: String): UserEntity {
        val cleanEmail = email.trim().lowercase()
        val existing = dao.findByEmail(cleanEmail)
        val hash = when {
            password.isNotEmpty() -> hashPassword(password)
            else -> existing?.passwordHash ?: ""
        }
        val entity = (existing ?: UserEntity(fullName = fullName, email = cleanEmail, phone = phone, passwordHash = hash, role = role))
            .copy(fullName = fullName, phone = phone, passwordHash = hash, role = role)
        val id = dao.insert(entity)
        return if (existing != null) entity else entity.copy(userId = id)
    }
}

class PatientRepository(private val dao: PatientDao, private val sync: CloudSync? = null) {
    fun all() = dao.getAll()
    fun observe(id: Long) = dao.observeById(id)
    suspend fun get(id: Long) = dao.getById(id)
    fun search(q: String) = dao.search(q)
    fun count() = dao.count()
    suspend fun update(patient: PatientEntity) {
        dao.update(patient)
        sync?.push("patients", patient.patientId, patient.toCloudMap())
    }
    suspend fun insert(patient: PatientEntity): Long {
        val id = dao.insert(patient)
        sync?.push("patients", id, patient.copy(patientId = id).toCloudMap())
        return id
    }

    /**
     * Finds the patient record that belongs to a signed-in account: first by e-mail, then through
     * the demo-login map, otherwise a new record is created from the account's details.
     * Returns the patientId the session should use.
     */
    suspend fun findOrCreateFor(user: UserEntity): Long {
        val email = user.email.trim().lowercase()
        dao.getByEmail(email)?.let { return it.patientId }
        Demo.PATIENT_BY_EMAIL[email]?.let { id -> if (dao.getById(id) != null) return id }

        val parts = user.fullName.trim().split(Regex("\\s+"), limit = 2)
        // The id is the creation time (not 1, 2, 3...) so two phones that each register a new
        // patient offline can never both claim the same id when their data is synced.
        val id = insert(
            PatientEntity(
                patientId = System.currentTimeMillis(),
                firstName = parts.firstOrNull().orEmpty().ifBlank { "Patient" },
                lastName = parts.getOrNull(1).orEmpty(),
                dateOfBirth = "", gender = "", phone = user.phone, email = email,
                address = "", emergencyContact = "",
            )
        )
        AppLog.i("Session", "Created patient record $id for a new account")
        return id
    }
}

class DoctorRepository(private val dao: DoctorDao) {
    suspend fun insert(d: DoctorEntity): Long = dao.insert(d)

    /** Doctor record for a signed-in Doctor account: by e-mail, then the demo map, else created. */
    suspend fun findOrCreateFor(user: UserEntity): Long {
        val email = user.email.trim().lowercase()
        dao.getByEmail(email)?.let { return it.doctorId }
        Demo.DOCTOR_BY_EMAIL[email]?.let { id -> if (dao.getById(id) != null) return id }

        val parts = user.fullName.removePrefix("Dr ").trim().split(Regex("\\s+"), limit = 2)
        val id = dao.insert(
            DoctorEntity(
                doctorId = System.currentTimeMillis(),
                firstName = parts.firstOrNull().orEmpty().ifBlank { "Doctor" },
                lastName = parts.getOrNull(1).orEmpty(),
                specialization = "General Practitioner", department = "Outpatient",
                phone = user.phone, email = email,
            )
        )
        AppLog.i("Session", "Created doctor record $id for a new account")
        return id
    }

    fun all() = dao.getAll()
    fun byDepartment(dept: String) = dao.getByDepartment(dept)
    fun departments() = dao.departments()
    suspend fun get(id: Long) = dao.getById(id)
}

class AppointmentRepository(
    private val dao: AppointmentDao,
    private val sync: CloudSync? = null,
    private val api: MahmsApi? = null,
) {
    fun all() = dao.getAll()
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun byDoctor(id: Long) = dao.getByDoctor(id)
    fun byDoctorAndDate(id: Long, date: String) = dao.getByDoctorAndDate(id, date)
    fun byDateAndStatus(date: String, status: String) = dao.getByDateAndStatus(date, status)
    fun byPatientAndStatuses(id: Long, statuses: List<String>) = dao.getByPatientAndStatuses(id, statuses)
    fun countForDate(date: String) = dao.countForDate(date)
    fun countPerDay() = dao.countPerDay()
    suspend fun get(id: Long) = dao.getById(id)
    /**
     * Saves the booking to Room first, so it is never lost, then mirrors it to Cloud Firestore
     * and to the team's REST API. Both mirrors are best effort: offline, the row stays in Room
     * and is picked up by the next Sync now.
     */
    suspend fun insert(a: AppointmentEntity): Long {
        val id = dao.insert(a)
        val saved = a.copy(appointmentId = id)
        sync?.push("appointments", id, saved.toCloudMap())
        mirrorToApi(saved)
        return id
    }
    suspend fun update(a: AppointmentEntity) {
        dao.update(a)
        sync?.push("appointments", a.appointmentId, a.toCloudMap())
        mirrorToApi(a)
    }

    private suspend fun mirrorToApi(a: AppointmentEntity) {
        val client = api ?: return
        if (!client.isConfigured) return
        try {
            client.bookAppointment(
                clientRef = a.appointmentId,
                doctorRef = a.doctorId,
                date = a.appointmentDate,
                time = a.appointmentTime,
                reason = a.reason,
                status = a.status,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w("Appointments", "Could not mirror appointment ${a.appointmentId} to the API", e)
        }
    }
}

class PrescriptionRepository(private val dao: PrescriptionDao, private val sync: CloudSync? = null) {
    fun all() = dao.getAll()
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun byDoctor(id: Long) = dao.getByDoctor(id)
    fun pending() = dao.getPending()
    fun dispensed() = dao.getDispensed()
    fun countForDoctorOnDate(id: Long, date: String) = dao.countForDoctorOnDate(id, date)
    suspend fun insert(p: PrescriptionEntity): Long {
        val id = dao.insert(p)
        sync?.push("prescriptions", id, p.copy(prescriptionId = id).toCloudMap())
        return id
    }
    suspend fun update(p: PrescriptionEntity) {
        dao.update(p)
        sync?.push("prescriptions", p.prescriptionId, p.toCloudMap())
    }
}

class EmergencyRepository(
    private val dao: EmergencyDao,
    private val sync: CloudSync? = null,
    private val api: MahmsApi? = null,
) {
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun active() = dao.getActive()
    fun resolved() = dao.getResolved()
    fun countActive() = dao.countActive()
    /**
     * Saves the SOS alert locally, then sends it to Firestore and to the REST API. The alert
     * is kept even with no GPS fix and no connection, because losing it would be worse than
     * losing the position.
     */
    suspend fun insert(e: EmergencyEntity): Long {
        val id = dao.insert(e)
        val saved = e.copy(emergencyId = id)
        sync?.push("emergencies", id, saved.toCloudMap())

        val client = api
        if (client != null && client.isConfigured) {
            try {
                val point = GeoUtils.parseCoordinates(saved.location)
                client.raiseEmergency(
                    clientRef = id,
                    location = saved.location,
                    latitude = point?.first,
                    longitude = point?.second,
                    description = saved.description,
                )
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                AppLog.w("Emergency", "Could not send alert $id to the API", ex)
            }
        }
        return id
    }
    suspend fun update(e: EmergencyEntity) {
        dao.update(e)
        sync?.push("emergencies", e.emergencyId, e.toCloudMap())
    }
}

class PaymentRepository(
    private val dao: PaymentDao,
    private val sync: CloudSync? = null,
    private val api: MahmsApi? = null,
) {
    fun all() = dao.getAll()
    fun byPatient(patientId: Long) = dao.getByPatient(patientId)
    fun revenueForDate(date: String) = dao.revenueForDate(date)

    /**
     * Saves (or replaces, when the id exists) a payment and copies it to the online database.
     *
     * The payment is also recorded through the REST API, which issues the receipt number from
     * a database sequence. When that succeeds the number is stored on the local row and the
     * receipt screens show it. When it does not, receiptNo stays null and the screens fall
     * back to [com.doodoobops.mahms.utils.Billing.receiptNumber], so an offline payment still
     * produces a receipt.
     */
    suspend fun insert(p: PaymentEntity): Long {
        val id = dao.insert(p)
        var saved = p.copy(paymentId = id)

        val client = api
        if (client != null && client.isConfigured) {
            try {
                val serverReceipt = client.recordPayment(
                    clientRef = id,
                    appointmentRef = saved.appointmentId,
                    amount = saved.amount,
                    method = saved.paymentMethod,
                    date = saved.paymentDate,
                    status = saved.status,
                )
                if (serverReceipt != null && serverReceipt != saved.receiptNo) {
                    saved = saved.copy(receiptNo = serverReceipt)
                    dao.upsert(saved)
                    AppLog.i("Payments", "Receipt $serverReceipt issued by the API for payment $id")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("Payments", "Could not record payment $id with the API", e)
            }
        }

        sync?.push("payments", id, saved.toCloudMap())
        AppLog.i("Payments", "Payment $id saved for appointment ${saved.appointmentId} (${saved.status})")
        return id
    }

    /** The receipt the server holds for a payment, or null when the API is not reachable. */
    suspend fun fetchServerReceipt(paymentId: Long): String? {
        val client = api ?: return null
        if (!client.isConfigured) return null
        return try {
            client.receipt(paymentId)?.optString("receiptNo")?.ifBlank { null }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w("Payments", "Could not fetch the receipt for payment $paymentId", e)
            null
        }
    }
}

class ReminderRepository(private val dao: MedicationReminderDao) {
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun countActive(id: Long) = dao.countActive(id)
    suspend fun getByPrescription(id: Long) = dao.getByPrescription(id)
    suspend fun insert(r: MedicationReminderEntity) = dao.insert(r)
    suspend fun update(r: MedicationReminderEntity) = dao.update(r)
    suspend fun allActive() = dao.getAllActive()
}

class StaffRepository(private val dao: StaffDao, private val sync: CloudSync? = null) {
    fun all() = dao.getAll()
    suspend fun insert(s: StaffEntity): Long {
        val id = dao.insert(s)
        sync?.push("staff", id, s.copy(staffId = id).toCloudMap())
        return id
    }
    suspend fun delete(s: StaffEntity) {
        dao.delete(s)
        sync?.delete("staff", s.staffId)
    }
}
