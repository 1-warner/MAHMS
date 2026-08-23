package com.doodoobops.mahms.data.repository

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
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.data.entity.StaffEntity
import com.doodoobops.mahms.data.entity.UserEntity
import com.doodoobops.mahms.utils.hashPassword

sealed interface RegisterResult {
    data class Success(val userId: Long) : RegisterResult
    data object EmailTaken : RegisterResult
    data class Failed(val reason: String) : RegisterResult
}

class UserRepository(
    private val dao: UserDao,
    private val patientDao: PatientDao,
    private val doctorDao: DoctorDao,
) {
    /**
     * Creates an account and, for clinical roles, the matching clinical record so the
     * session reads that person's real data instead of a shared demo record.
     */
    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String,
    ): RegisterResult {
        val normalised = email.trim().lowercase()
        if (dao.findByEmail(normalised) != null) return RegisterResult.EmailTaken

        val first = fullName.trim().substringBefore(' ').ifBlank { fullName.trim() }
        val last = fullName.trim().substringAfter(' ', "").ifBlank { "-" }

        val patientId = if (role == "Patient") {
            patientDao.insert(
                PatientEntity(
                    firstName = first, lastName = last, dateOfBirth = "", gender = "",
                    phone = phone, email = normalised, address = "", emergencyContact = "",
                )
            )
        } else null

        val doctorId = if (role == "Doctor") {
            doctorDao.insert(
                DoctorEntity(
                    firstName = first, lastName = last,
                    specialization = "General Practitioner", department = "Outpatient",
                    phone = phone, email = normalised,
                )
            )
        } else null

        return try {
            val id = dao.insert(
                UserEntity(
                    fullName = fullName.trim(), email = normalised, phone = phone,
                    passwordHash = hashPassword(password), role = role,
                    linkedPatientId = patientId, linkedDoctorId = doctorId,
                )
            )
            if (id > 0) RegisterResult.Success(id) else RegisterResult.Failed("Could not create account")
        } catch (e: Exception) {
            RegisterResult.Failed("Could not create account")
        }
    }

    /** Returns the user when email+password match, else null. */
    suspend fun login(email: String, password: String): UserEntity? {
        val user = dao.findByEmail(email.trim().lowercase()) ?: return null
        return if (user.passwordHash == hashPassword(password)) user else null
    }

    /** Self-service reset: proves identity with the registered email + phone. */
    suspend fun verifyIdentity(email: String, phone: String): UserEntity? =
        dao.findByEmailAndPhone(email.trim().lowercase(), phone.trim())

    suspend fun setPassword(userId: Long, newPassword: String) =
        dao.updatePassword(userId, hashPassword(newPassword))

    fun all() = dao.getAll()
    suspend fun getById(id: Long) = dao.findById(id)
    suspend fun count() = dao.count()
}

class PatientRepository(private val dao: PatientDao) {
    fun all() = dao.getAll()
    fun observe(id: Long) = dao.observeById(id)
    suspend fun get(id: Long) = dao.getById(id)
    fun search(q: String) = dao.search(q)
    fun count() = dao.count()
    suspend fun update(patient: PatientEntity) = dao.update(patient)
    suspend fun insert(patient: PatientEntity) = dao.insert(patient)
}

class DoctorRepository(private val dao: DoctorDao) {
    fun all() = dao.getAll()
    fun byDepartment(dept: String) = dao.getByDepartment(dept)
    fun departments() = dao.departments()
    suspend fun get(id: Long) = dao.getById(id)
}

class AppointmentRepository(private val dao: AppointmentDao) {
    fun all() = dao.getAll()
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun byDoctor(id: Long) = dao.getByDoctor(id)
    fun byDoctorAndDate(id: Long, date: String) = dao.getByDoctorAndDate(id, date)
    fun byDateAndStatus(date: String, status: String) = dao.getByDateAndStatus(date, status)
    fun byPatientAndStatuses(id: Long, statuses: List<String>) = dao.getByPatientAndStatuses(id, statuses)
    fun countForDate(date: String) = dao.countForDate(date)
    fun countPerDay() = dao.countPerDay()
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun insert(a: AppointmentEntity) = dao.insert(a)
    suspend fun update(a: AppointmentEntity) = dao.update(a)
}

class PrescriptionRepository(private val dao: PrescriptionDao) {
    fun all() = dao.getAll()
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun byDoctor(id: Long) = dao.getByDoctor(id)
    fun pending() = dao.getPending()
    fun dispensed() = dao.getDispensed()
    fun countForDoctorOnDate(id: Long, date: String) = dao.countForDoctorOnDate(id, date)
    suspend fun insert(p: PrescriptionEntity) = dao.insert(p)
    suspend fun update(p: PrescriptionEntity) = dao.update(p)
}

class EmergencyRepository(private val dao: EmergencyDao) {
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun active() = dao.getActive()
    fun resolved() = dao.getResolved()
    fun countActive() = dao.countActive()
    suspend fun insert(e: EmergencyEntity) = dao.insert(e)
    suspend fun update(e: EmergencyEntity) = dao.update(e)
}

class PaymentRepository(private val dao: PaymentDao) {
    fun all() = dao.getAll()
    fun revenueForDate(date: String) = dao.revenueForDate(date)
    fun byPatient(patientId: Long) = dao.getByPatient(patientId)
    suspend fun forAppointment(appointmentId: Long) = dao.getByAppointment(appointmentId)
    suspend fun insert(p: PaymentEntity) = dao.insert(p)
    suspend fun update(p: PaymentEntity) = dao.update(p)
}

class ReminderRepository(private val dao: MedicationReminderDao) {
    fun byPatient(id: Long) = dao.getByPatient(id)
    fun countActive(id: Long) = dao.countActive(id)
    suspend fun getByPrescription(id: Long) = dao.getByPrescription(id)
    suspend fun insert(r: MedicationReminderEntity) = dao.insert(r)
    suspend fun update(r: MedicationReminderEntity) = dao.update(r)
    suspend fun allActive() = dao.getAllActive()
}

class StaffRepository(private val dao: StaffDao) {
    fun all() = dao.getAll()
    suspend fun insert(s: StaffEntity) = dao.insert(s)
    suspend fun delete(s: StaffEntity) = dao.delete(s)
}
