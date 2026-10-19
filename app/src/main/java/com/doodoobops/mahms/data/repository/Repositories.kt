package com.doodoobops.mahms.data.repository

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
import com.doodoobops.mahms.utils.hashPassword

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

class AppointmentRepository(private val dao: AppointmentDao, private val sync: CloudSync? = null) {
    fun all() = dao.getAll()
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun byDoctor(id: Long) = dao.getByDoctor(id)
    fun byDoctorAndDate(id: Long, date: String) = dao.getByDoctorAndDate(id, date)
    fun byDateAndStatus(date: String, status: String) = dao.getByDateAndStatus(date, status)
    fun byPatientAndStatuses(id: Long, statuses: List<String>) = dao.getByPatientAndStatuses(id, statuses)
    fun countForDate(date: String) = dao.countForDate(date)
    fun countPerDay() = dao.countPerDay()
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun insert(a: AppointmentEntity): Long {
        val id = dao.insert(a)
        sync?.push("appointments", id, a.copy(appointmentId = id).toCloudMap())
        return id
    }
    suspend fun update(a: AppointmentEntity) {
        dao.update(a)
        sync?.push("appointments", a.appointmentId, a.toCloudMap())
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

class EmergencyRepository(private val dao: EmergencyDao, private val sync: CloudSync? = null) {
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun active() = dao.getActive()
    fun resolved() = dao.getResolved()
    fun countActive() = dao.countActive()
    suspend fun insert(e: EmergencyEntity): Long {
        val id = dao.insert(e)
        sync?.push("emergencies", id, e.copy(emergencyId = id).toCloudMap())
        return id
    }
    suspend fun update(e: EmergencyEntity) {
        dao.update(e)
        sync?.push("emergencies", e.emergencyId, e.toCloudMap())
    }
}

class PaymentRepository(private val dao: PaymentDao, private val sync: CloudSync? = null) {
    fun all() = dao.getAll()
    fun byPatient(patientId: Long) = dao.getByPatient(patientId)
    fun revenueForDate(date: String) = dao.revenueForDate(date)

    /** Saves (or replaces, when the id exists) a payment and copies it to the online database. */
    suspend fun insert(p: PaymentEntity): Long {
        val id = dao.insert(p)
        sync?.push("payments", id, p.copy(paymentId = id).toCloudMap())
        AppLog.i("Payments", "Payment $id saved for appointment ${p.appointmentId} (${p.status})")
        return id
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
