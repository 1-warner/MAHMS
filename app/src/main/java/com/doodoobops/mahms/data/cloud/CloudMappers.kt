package com.doodoobops.mahms.data.cloud

import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.data.entity.StaffEntity

/**
 * Room entity -> Firestore document mappers.
 *
 * UserEntity is intentionally NOT mapped: passwords live only in Firebase Authentication
 * (hashed server-side), and profile/role data is written to `users/{uid}` by
 * FirebaseAuthRepository. No password hash ever leaves the device.
 */

fun PatientEntity.toCloudMap(): Map<String, Any?> = mapOf(
    "patientId" to patientId,
    "firstName" to firstName,
    "lastName" to lastName,
    "dateOfBirth" to dateOfBirth,
    "gender" to gender,
    "phone" to phone,
    "email" to email,
    "address" to address,
    "emergencyContact" to emergencyContact,
    "registrationDate" to registrationDate,
    "notes" to notes,
)

fun DoctorEntity.toCloudMap(): Map<String, Any?> = mapOf(
    "doctorId" to doctorId,
    "firstName" to firstName,
    "lastName" to lastName,
    "specialization" to specialization,
    "department" to department,
    "phone" to phone,
    "email" to email,
)

fun AppointmentEntity.toCloudMap(): Map<String, Any?> = mapOf(
    "appointmentId" to appointmentId,
    "patientId" to patientId,
    "doctorId" to doctorId,
    "appointmentDate" to appointmentDate,
    "appointmentTime" to appointmentTime,
    "status" to status,
    "reason" to reason,
)

fun PrescriptionEntity.toCloudMap(): Map<String, Any?> = mapOf(
    "prescriptionId" to prescriptionId,
    "appointmentId" to appointmentId,
    "patientId" to patientId,
    "doctorId" to doctorId,
    "medicationName" to medicationName,
    "dosage" to dosage,
    "frequency" to frequency,
    "duration" to duration,
    "issueDate" to issueDate,
    "dispensed" to dispensed,
)

fun EmergencyEntity.toCloudMap(): Map<String, Any?> = mapOf(
    "emergencyId" to emergencyId,
    "patientId" to patientId,
    "timestamp" to timestamp,
    "location" to location,
    "status" to status,
    "description" to description,
    "resolvedAt" to resolvedAt,
)

fun PaymentEntity.toCloudMap(): Map<String, Any?> = mapOf(
    "paymentId" to paymentId,
    "appointmentId" to appointmentId,
    "amount" to amount,
    "paymentMethod" to paymentMethod,
    "paymentDate" to paymentDate,
    "status" to status,
)

fun MedicationReminderEntity.toCloudMap(): Map<String, Any?> = mapOf(
    "reminderId" to reminderId,
    "prescriptionId" to prescriptionId,
    "patientId" to patientId,
    "medicationName" to medicationName,
    "dosage" to dosage,
    "reminderTime" to reminderTime,
    "isActive" to isActive,
)

fun StaffEntity.toCloudMap(): Map<String, Any?> = mapOf(
    "staffId" to staffId,
    "firstName" to firstName,
    "lastName" to lastName,
    "role" to role,
    "department" to department,
    "phone" to phone,
    "email" to email,
)

// ---------------------------------------------------------------------------------------------
// Firestore document -> Room entity (used when pulling the online data down to the phone).
// Each function returns null when the document has no usable id, so bad documents are skipped.
// Firestore hands numbers back as Long or Double, so every read goes through Number.
// ---------------------------------------------------------------------------------------------

private fun Map<String, Any?>.str(key: String): String = (this[key] as? String).orEmpty()
private fun Map<String, Any?>.lng(key: String): Long? = (this[key] as? Number)?.toLong()
private fun Map<String, Any?>.dbl(key: String): Double = (this[key] as? Number)?.toDouble() ?: 0.0
private fun Map<String, Any?>.flag(key: String): Boolean = (this[key] as? Boolean) ?: false

fun Map<String, Any?>.toPatient(): PatientEntity? {
    val id = lng("patientId") ?: return null
    return PatientEntity(
        patientId = id, firstName = str("firstName"), lastName = str("lastName"),
        dateOfBirth = str("dateOfBirth"), gender = str("gender"), phone = str("phone"),
        email = str("email"), address = str("address"), emergencyContact = str("emergencyContact"),
        registrationDate = lng("registrationDate") ?: 0L, notes = str("notes"),
    )
}

fun Map<String, Any?>.toDoctor(): DoctorEntity? {
    val id = lng("doctorId") ?: return null
    return DoctorEntity(
        doctorId = id, firstName = str("firstName"), lastName = str("lastName"),
        specialization = str("specialization"), department = str("department"),
        phone = str("phone"), email = str("email"),
    )
}

fun Map<String, Any?>.toAppointment(): AppointmentEntity? {
    val id = lng("appointmentId") ?: return null
    return AppointmentEntity(
        appointmentId = id, patientId = lng("patientId") ?: return null,
        doctorId = lng("doctorId") ?: return null, appointmentDate = str("appointmentDate"),
        appointmentTime = str("appointmentTime"), status = str("status"), reason = str("reason"),
    )
}

fun Map<String, Any?>.toPrescription(): PrescriptionEntity? {
    val id = lng("prescriptionId") ?: return null
    return PrescriptionEntity(
        prescriptionId = id, appointmentId = lng("appointmentId") ?: return null,
        patientId = lng("patientId") ?: return null, doctorId = lng("doctorId") ?: return null,
        medicationName = str("medicationName"), dosage = str("dosage"), frequency = str("frequency"),
        duration = str("duration"), issueDate = str("issueDate"), dispensed = flag("dispensed"),
    )
}

fun Map<String, Any?>.toEmergency(): EmergencyEntity? {
    val id = lng("emergencyId") ?: return null
    return EmergencyEntity(
        emergencyId = id, patientId = lng("patientId") ?: return null,
        timestamp = lng("timestamp") ?: 0L, location = str("location"), status = str("status"),
        description = str("description"), resolvedAt = lng("resolvedAt"),
    )
}

fun Map<String, Any?>.toPayment(): PaymentEntity? {
    val id = lng("paymentId") ?: return null
    return PaymentEntity(
        paymentId = id, appointmentId = lng("appointmentId") ?: return null, amount = dbl("amount"),
        paymentMethod = str("paymentMethod"), paymentDate = str("paymentDate"), status = str("status"),
    )
}

fun Map<String, Any?>.toReminder(): MedicationReminderEntity? {
    val id = lng("reminderId") ?: return null
    return MedicationReminderEntity(
        reminderId = id, prescriptionId = lng("prescriptionId") ?: return null,
        patientId = lng("patientId") ?: return null, medicationName = str("medicationName"),
        dosage = str("dosage"), reminderTime = str("reminderTime"), isActive = flag("isActive"),
    )
}

fun Map<String, Any?>.toStaff(): StaffEntity? {
    val id = lng("staffId") ?: return null
    return StaffEntity(
        staffId = id, firstName = str("firstName"), lastName = str("lastName"), role = str("role"),
        department = str("department"), phone = str("phone"), email = str("email"),
    )
}
