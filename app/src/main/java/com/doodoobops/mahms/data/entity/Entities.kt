package com.doodoobops.mahms.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** App login account. Role drives post-login routing. */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val role: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Clinical record this account owns, when the role is Patient. */
    val linkedPatientId: Long? = null,
    /** Clinical record this account owns, when the role is Doctor. */
    val linkedDoctorId: Long? = null,
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val patientId: Long = 0,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val gender: String,
    val phone: String,
    val email: String,
    val address: String,
    val emergencyContact: String,
    val registrationDate: Long = System.currentTimeMillis(),
    /** Free-text clinical notes (vitals appended here for the local MVP). */
    val notes: String = "",
)

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey(autoGenerate = true) val doctorId: Long = 0,
    val firstName: String,
    val lastName: String,
    val specialization: String,
    val department: String,
    val phone: String,
    val email: String,
)

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(entity = PatientEntity::class, parentColumns = ["patientId"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DoctorEntity::class, parentColumns = ["doctorId"], childColumns = ["doctorId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("patientId"), Index("doctorId")],
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val appointmentId: Long = 0,
    val patientId: Long,
    val doctorId: Long,
    val appointmentDate: String,
    val appointmentTime: String,
    /** Pending / Confirmed / Completed / Cancelled */
    val status: String,
    val reason: String,
)

@Entity(
    tableName = "prescriptions",
    foreignKeys = [
        ForeignKey(entity = AppointmentEntity::class, parentColumns = ["appointmentId"], childColumns = ["appointmentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PatientEntity::class, parentColumns = ["patientId"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DoctorEntity::class, parentColumns = ["doctorId"], childColumns = ["doctorId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("appointmentId"), Index("patientId"), Index("doctorId")],
)
data class PrescriptionEntity(
    @PrimaryKey(autoGenerate = true) val prescriptionId: Long = 0,
    val appointmentId: Long,
    val patientId: Long,
    val doctorId: Long,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val issueDate: String,
    val dispensed: Boolean = false,
)

@Entity(
    tableName = "emergencies",
    foreignKeys = [
        ForeignKey(entity = PatientEntity::class, parentColumns = ["patientId"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("patientId")],
)
data class EmergencyEntity(
    @PrimaryKey(autoGenerate = true) val emergencyId: Long = 0,
    val patientId: Long,
    val timestamp: Long,
    val location: String,
    /** Active / In Progress / Resolved */
    val status: String,
    val description: String,
    val resolvedAt: Long? = null,
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(entity = AppointmentEntity::class, parentColumns = ["appointmentId"], childColumns = ["appointmentId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("appointmentId")],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val paymentId: Long = 0,
    val appointmentId: Long,
    val amount: Double,
    val paymentMethod: String,
    val paymentDate: String,
    /** Pending / Completed / Failed */
    val status: String,
)

@Entity(
    tableName = "medication_reminders",
    foreignKeys = [
        ForeignKey(entity = PrescriptionEntity::class, parentColumns = ["prescriptionId"], childColumns = ["prescriptionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PatientEntity::class, parentColumns = ["patientId"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("prescriptionId"), Index("patientId")],
)
data class MedicationReminderEntity(
    @PrimaryKey(autoGenerate = true) val reminderId: Long = 0,
    val prescriptionId: Long,
    val patientId: Long,
    val medicationName: String,
    val dosage: String,
    /** "HH:mm" 24-hour. */
    val reminderTime: String,
    val isActive: Boolean = true,
    /** Adherence tracking: doses marked taken / missed from the reminder sheet. */
    val takenCount: Int = 0,
    val missedCount: Int = 0,
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val staffId: Long = 0,
    val firstName: String,
    val lastName: String,
    val role: String,
    val department: String,
    val phone: String,
    val email: String,
)
