package com.doodoobops.mahms.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.data.entity.StaffEntity
import com.doodoobops.mahms.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("UPDATE users SET passwordHash = :hash WHERE userId = :id")
    suspend fun updatePassword(id: Long, hash: String)
}

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity): Long

    @Update
    suspend fun update(patient: PatientEntity)

    @Delete
    suspend fun delete(patient: PatientEntity)

    @Query("SELECT * FROM patients ORDER BY firstName, lastName")
    fun getAll(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE patientId = :id LIMIT 1")
    fun observeById(id: Long): Flow<PatientEntity?>

    @Query("SELECT * FROM patients WHERE patientId = :id LIMIT 1")
    suspend fun getById(id: Long): PatientEntity?

    @Query("SELECT * FROM patients WHERE firstName LIKE '%' || :q || '%' OR lastName LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%' ORDER BY firstName")
    fun search(q: String): Flow<List<PatientEntity>>

    @Query("SELECT COUNT(*) FROM patients")
    fun count(): Flow<Int>

    @Query("SELECT * FROM patients WHERE lower(email) = lower(:email) LIMIT 1")
    suspend fun getByEmail(email: String): PatientEntity?

    /** Insert-or-update WITHOUT delete, so child rows are never removed by a cascade. */
    @Upsert
    suspend fun upsert(patient: PatientEntity)
}

@Dao
interface DoctorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doctor: DoctorEntity): Long

    @Query("SELECT * FROM doctors ORDER BY lastName")
    fun getAll(): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE department = :dept ORDER BY lastName")
    fun getByDepartment(dept: String): Flow<List<DoctorEntity>>

    @Query("SELECT DISTINCT department FROM doctors ORDER BY department")
    fun departments(): Flow<List<String>>

    @Query("SELECT * FROM doctors WHERE doctorId = :id LIMIT 1")
    suspend fun getById(id: Long): DoctorEntity?

    @Query("SELECT * FROM doctors WHERE lower(email) = lower(:email) LIMIT 1")
    suspend fun getByEmail(email: String): DoctorEntity?

    @Upsert
    suspend fun upsert(doctor: DoctorEntity)
}

@Dao
interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: AppointmentEntity): Long

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments ORDER BY appointmentDate, appointmentTime")
    fun getAll(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY appointmentDate DESC, appointmentTime DESC")
    fun getByPatient(patientId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId ORDER BY appointmentDate, appointmentTime")
    fun getByDoctor(doctorId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId AND appointmentDate = :date ORDER BY appointmentTime")
    fun getByDoctorAndDate(doctorId: Long, date: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE appointmentDate = :date AND status = :status ORDER BY appointmentTime")
    fun getByDateAndStatus(date: String, status: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId AND status IN (:statuses) ORDER BY appointmentDate, appointmentTime")
    fun getByPatientAndStatuses(patientId: Long, statuses: List<String>): Flow<List<AppointmentEntity>>

    @Query("SELECT COUNT(*) FROM appointments WHERE appointmentDate = :date")
    fun countForDate(date: String): Flow<Int>

    @Query("SELECT appointmentDate as date, COUNT(*) as total FROM appointments GROUP BY appointmentDate ORDER BY appointmentDate")
    fun countPerDay(): Flow<List<DayCount>>

    @Query("SELECT * FROM appointments WHERE appointmentId = :id LIMIT 1")
    suspend fun getById(id: Long): AppointmentEntity?

    @Upsert
    suspend fun upsert(appointment: AppointmentEntity)
}

data class DayCount(val date: String, val total: Int)

@Dao
interface PrescriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prescription: PrescriptionEntity): Long

    @Update
    suspend fun update(prescription: PrescriptionEntity)

    @Query("SELECT * FROM prescriptions ORDER BY issueDate DESC")
    fun getAll(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE patientId = :patientId ORDER BY issueDate DESC")
    fun getByPatient(patientId: Long): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE doctorId = :doctorId ORDER BY issueDate DESC")
    fun getByDoctor(doctorId: Long): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE dispensed = 0 ORDER BY issueDate DESC")
    fun getPending(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE dispensed = 1 ORDER BY issueDate DESC")
    fun getDispensed(): Flow<List<PrescriptionEntity>>

    @Query("SELECT COUNT(*) FROM prescriptions WHERE doctorId = :doctorId AND issueDate = :date")
    fun countForDoctorOnDate(doctorId: Long, date: String): Flow<Int>

    @Upsert
    suspend fun upsert(prescription: PrescriptionEntity)
}

@Dao
interface EmergencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emergency: EmergencyEntity): Long

    @Update
    suspend fun update(emergency: EmergencyEntity)

    @Query("SELECT * FROM emergencies WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getByPatient(patientId: Long): Flow<List<EmergencyEntity>>

    @Query("SELECT * FROM emergencies WHERE status IN ('Active','In Progress') ORDER BY timestamp DESC")
    fun getActive(): Flow<List<EmergencyEntity>>

    @Query("SELECT * FROM emergencies WHERE status = 'Resolved' ORDER BY timestamp DESC")
    fun getResolved(): Flow<List<EmergencyEntity>>

    @Query("SELECT COUNT(*) FROM emergencies WHERE status IN ('Active','In Progress')")
    fun countActive(): Flow<Int>

    @Upsert
    suspend fun upsert(emergency: EmergencyEntity)
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAll(): Flow<List<PaymentEntity>>

    @Query("SELECT IFNULL(SUM(amount),0) FROM payments WHERE status = 'Completed' AND paymentDate = :date")
    fun revenueForDate(date: String): Flow<Double>

    /** All payments for one patient (payments link to appointments, appointments to patients). */
    @Query("SELECT p.* FROM payments p INNER JOIN appointments a ON a.appointmentId = p.appointmentId WHERE a.patientId = :patientId ORDER BY p.paymentId DESC")
    fun getByPatient(patientId: Long): Flow<List<PaymentEntity>>

    @Upsert
    suspend fun upsert(payment: PaymentEntity)
}

@Dao
interface MedicationReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: MedicationReminderEntity): Long

    @Update
    suspend fun update(reminder: MedicationReminderEntity)

    @Query("SELECT * FROM medication_reminders WHERE patientId = :patientId ORDER BY reminderTime")
    fun getByPatient(patientId: Long): Flow<List<MedicationReminderEntity>>

    @Query("SELECT * FROM medication_reminders WHERE prescriptionId = :prescriptionId LIMIT 1")
    suspend fun getByPrescription(prescriptionId: Long): MedicationReminderEntity?

    @Query("SELECT COUNT(*) FROM medication_reminders WHERE patientId = :patientId AND isActive = 1")
    fun countActive(patientId: Long): Flow<Int>

    @Query("SELECT * FROM medication_reminders WHERE isActive = 1")
    suspend fun getAllActive(): List<MedicationReminderEntity>

    @Upsert
    suspend fun upsert(reminder: MedicationReminderEntity)
}

@Dao
interface StaffDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(staff: StaffEntity): Long

    @Delete
    suspend fun delete(staff: StaffEntity)

    @Query("SELECT * FROM staff ORDER BY lastName")
    fun getAll(): Flow<List<StaffEntity>>

    @Upsert
    suspend fun upsert(staff: StaffEntity)
}
