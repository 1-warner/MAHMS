package com.doodoobops.mahms.data.seed

import com.doodoobops.mahms.data.db.MahmsDatabase
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.data.entity.StaffEntity
import com.doodoobops.mahms.data.entity.UserEntity
import com.doodoobops.mahms.utils.DateUtils
import com.doodoobops.mahms.utils.hashPassword
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Seeds realistic mock data on first launch (idempotent: only when empty). */
object DatabaseSeeder {

    private val store = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun offset(days: Int): String = store.format(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }.time
    )

    suspend fun seedIfEmpty(db: MahmsDatabase) {
        if (db.patientDao().getById(1) != null) return
        seed(db)
    }

    private suspend fun seed(db: MahmsDatabase) {
        // ---- Demo login accounts (one per role) ----
        val accounts = listOf(
            Triple("Thabo Moyo", "patient@mpilo.co.zw", "Patient"),
            Triple("Dr Sibusiso Moyo", "doctor@mpilo.co.zw", "Doctor"),
            Triple("Sr Lindiwe Ndlovu", "nurse@mpilo.co.zw", "Nurse"),
            Triple("Admin Office", "admin@mpilo.co.zw", "Hospital Administrator"),
            Triple("Pharmacy 03", "pharmacist@mpilo.co.zw", "Pharmacist"),
            Triple("Casualty Desk", "responder@mpilo.co.zw", "Emergency Responder"),
        )
        accounts.forEachIndexed { i, (name, email, role) ->
            db.userDao().insert(
                UserEntity(
                    fullName = name, email = email, phone = "+26377234567$i",
                    passwordHash = hashPassword("Password1"), role = role,
                )
            )
        }

        // ---- Doctors (3) ----
        val docs = listOf(
            DoctorEntity(firstName = "Sibusiso", lastName = "Moyo", specialization = "Cardiologist", department = "Cardiology", phone = "+263772000001", email = "smoyo@mpilo.co.zw"),
            DoctorEntity(firstName = "Tariro", lastName = "Sibanda", specialization = "General Practitioner", department = "Outpatient", phone = "+263772000002", email = "tsibanda@mpilo.co.zw"),
            DoctorEntity(firstName = "Anesu", lastName = "Ndlovu", specialization = "Obstetrician", department = "Maternity", phone = "+263772000003", email = "andlovu@mpilo.co.zw"),
        )
        docs.forEach { db.doctorDao().insert(it) }

        // ---- Patients (5) ----
        val patients = listOf(
            PatientEntity(firstName = "Thabo", lastName = "Moyo", dateOfBirth = "1987-03-12", gender = "Male", phone = "+263772345678", email = "thabo@mail.co.zw", address = "14 Fife St, Bulawayo", emergencyContact = "+263772999111"),
            PatientEntity(firstName = "Naledi", lastName = "Ncube", dateOfBirth = "1995-07-22", gender = "Female", phone = "+263773111222", email = "naledi@mail.co.zw", address = "8 Josiah Tongogara, Bulawayo", emergencyContact = "+263772999222"),
            PatientEntity(firstName = "Tendai", lastName = "Dube", dateOfBirth = "1979-11-02", gender = "Male", phone = "+263774222333", email = "tendai@mail.co.zw", address = "22 Leopold Takawira, Bulawayo", emergencyContact = "+263772999333"),
            PatientEntity(firstName = "Sipho", lastName = "Sibanda", dateOfBirth = "2001-01-30", gender = "Male", phone = "+263775333444", email = "sipho@mail.co.zw", address = "5 Robert Mugabe Way, Bulawayo", emergencyContact = "+263772999444"),
            PatientEntity(firstName = "Rutendo", lastName = "Khumalo", dateOfBirth = "1990-09-15", gender = "Female", phone = "+263776444555", email = "rutendo@mail.co.zw", address = "31 George Silundika, Bulawayo", emergencyContact = "+263772999555"),
        )
        patients.forEach { db.patientDao().insert(it) }

        // ---- Appointments (8, mixed statuses & dates) ----
        val today = DateUtils.todayStore()
        val appts = listOf(
            AppointmentEntity(patientId = 1, doctorId = 1, appointmentDate = offset(2), appointmentTime = "09:00", status = "Confirmed", reason = "Cardiology follow-up and BP review"),
            AppointmentEntity(patientId = 1, doctorId = 2, appointmentDate = offset(5), appointmentTime = "11:30", status = "Pending", reason = "Persistent cough for two weeks"),
            AppointmentEntity(patientId = 2, doctorId = 3, appointmentDate = today, appointmentTime = "10:00", status = "Confirmed", reason = "Antenatal review, 28 weeks"),
            AppointmentEntity(patientId = 3, doctorId = 1, appointmentDate = today, appointmentTime = "08:30", status = "Confirmed", reason = "Chest tightness on exertion"),
            AppointmentEntity(patientId = 4, doctorId = 2, appointmentDate = offset(-3), appointmentTime = "14:30", status = "Completed", reason = "Sprained ankle, follow-up"),
            AppointmentEntity(patientId = 5, doctorId = 2, appointmentDate = offset(-10), appointmentTime = "09:15", status = "Completed", reason = "Hay fever consultation"),
            AppointmentEntity(patientId = 2, doctorId = 1, appointmentDate = offset(-1), appointmentTime = "12:00", status = "Cancelled", reason = "Palpitations — rescheduled"),
            AppointmentEntity(patientId = 1, doctorId = 1, appointmentDate = offset(-20), appointmentTime = "09:00", status = "Completed", reason = "Initial cardiology assessment"),
        )
        appts.forEach { db.appointmentDao().insert(it) }

        // ---- Prescriptions (4) ----
        val rx = listOf(
            PrescriptionEntity(appointmentId = 8, patientId = 1, doctorId = 1, medicationName = "Atenolol", dosage = "50 mg", frequency = "Once daily", duration = "Ongoing", issueDate = offset(-20), dispensed = true),
            PrescriptionEntity(appointmentId = 8, patientId = 1, doctorId = 1, medicationName = "Amoxicillin", dosage = "500 mg", frequency = "Twice daily", duration = "7 days", issueDate = offset(-2), dispensed = false),
            PrescriptionEntity(appointmentId = 5, patientId = 4, doctorId = 2, medicationName = "Ibuprofen", dosage = "400 mg", frequency = "Three times daily", duration = "5 days", issueDate = offset(-3), dispensed = false),
            PrescriptionEntity(appointmentId = 6, patientId = 5, doctorId = 2, medicationName = "Loratadine", dosage = "10 mg", frequency = "Once daily", duration = "14 days", issueDate = offset(-10), dispensed = true),
        )
        rx.forEach { db.prescriptionDao().insert(it) }

        // ---- Medication reminders ----
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 1, patientId = 1, medicationName = "Atenolol", dosage = "50 mg", reminderTime = "08:00", isActive = true))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 2, patientId = 1, medicationName = "Amoxicillin", dosage = "500 mg", reminderTime = "20:00", isActive = true))

        // ---- Emergencies (1 active, 1 resolved) ----
        db.emergencyDao().insert(
            EmergencyEntity(patientId = 1, timestamp = System.currentTimeMillis() - 15 * 60_000, location = "GPS not available (mock) · Ward B-4", status = "Active", description = "Severe chest pain and shortness of breath")
        )
        db.emergencyDao().insert(
            EmergencyEntity(patientId = 3, timestamp = System.currentTimeMillis() - 2 * 86_400_000L, location = "GPS not available (mock) · Outpatient", status = "Resolved", description = "Fainting episode in waiting area", resolvedAt = System.currentTimeMillis() - 2 * 86_400_000L + 30 * 60_000)
        )

        // ---- Payments (3) ----
        db.paymentDao().insert(PaymentEntity(appointmentId = 5, amount = 25.0, paymentMethod = "EcoCash", paymentDate = today, status = "Completed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 6, amount = 20.0, paymentMethod = "Cash", paymentDate = today, status = "Completed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 1, amount = 40.0, paymentMethod = "Medical Aid", paymentDate = today, status = "Pending"))

        // ---- Staff ----
        listOf(
            StaffEntity(firstName = "Lindiwe", lastName = "Ndlovu", role = "Nurse", department = "Maternity", phone = "+263772100001", email = "lndlovu@mpilo.co.zw"),
            StaffEntity(firstName = "Brian", lastName = "Chikomo", role = "Pharmacist", department = "Pharmacy", phone = "+263772100002", email = "bchikomo@mpilo.co.zw"),
            StaffEntity(firstName = "Faith", lastName = "Mpofu", role = "Emergency Responder", department = "Casualty", phone = "+263772100003", email = "fmpofu@mpilo.co.zw"),
        ).forEach { db.staffDao().insert(it) }
    }
}
