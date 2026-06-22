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
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.data.entity.StaffEntity
import com.doodoobops.mahms.data.entity.UserEntity
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
