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
import com.doodoobops.mahms.utils.AppLog
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
        AppLog.i("Seed", "First launch: filling the database with sample data")
        // ---- Demo login accounts (one per role) ----
        val accounts = listOf(
            Triple("Thabo Moyo", "patient@mpilo.co.zw", "Patient"),
            Triple("Dr Sibusiso Moyo", "doctor@mpilo.co.zw", "Doctor"),
            Triple("Sr Lindiwe Ndlovu", "nurse@mpilo.co.zw", "Nurse"),
            Triple("Admin Office", "admin@mpilo.co.zw", "Hospital Administrator"),
            Triple("Pharmacy 03", "pharmacist@mpilo.co.zw", "Pharmacist"),
            Triple("Casualty Desk", "responder@mpilo.co.zw", "Emergency Responder"),
            Triple("Naledi Ncube", "naledi@mail.co.zw", "Patient"),
            Triple("Dr Tariro Sibanda", "tsibanda@mpilo.co.zw", "Doctor"),
            Triple("Sr Grace Mutasa", "gmutasa@mpilo.co.zw", "Nurse"),
            Triple("Pharmacy 07", "pharmacist2@mpilo.co.zw", "Pharmacist"),
        )
        accounts.forEachIndexed { i, (name, email, role) ->
            db.userDao().insert(
                UserEntity(
                    fullName = name, email = email, phone = "+26377234567$i",
                    passwordHash = hashPassword("Password1"), role = role,
                )
            )
        }

        // ---- Doctors (10) ----
        val docs = listOf(
            DoctorEntity(firstName = "Sibusiso", lastName = "Moyo", specialization = "Cardiologist", department = "Cardiology", phone = "+263772000001", email = "smoyo@mpilo.co.zw"),
            DoctorEntity(firstName = "Tariro", lastName = "Sibanda", specialization = "General Practitioner", department = "Outpatient", phone = "+263772000002", email = "tsibanda@mpilo.co.zw"),
            DoctorEntity(firstName = "Anesu", lastName = "Ndlovu", specialization = "Obstetrician", department = "Maternity", phone = "+263772000003", email = "andlovu@mpilo.co.zw"),
            DoctorEntity(firstName = "Farai", lastName = "Chuma", specialization = "Paediatrician", department = "Paediatrics", phone = "+263772000004", email = "fchuma@mpilo.co.zw"),
            DoctorEntity(firstName = "Nomsa", lastName = "Khanye", specialization = "Orthopaedic Surgeon", department = "Orthopaedics", phone = "+263772000005", email = "nkhanye@mpilo.co.zw"),
            DoctorEntity(firstName = "Tapiwa", lastName = "Mhlanga", specialization = "Dermatologist", department = "Dermatology", phone = "+263772000006", email = "tmhlanga@mpilo.co.zw"),
            DoctorEntity(firstName = "Lerato", lastName = "Mathe", specialization = "Neurologist", department = "Neurology", phone = "+263772000007", email = "lmathe@mpilo.co.zw"),
            DoctorEntity(firstName = "Kudzai", lastName = "Marufu", specialization = "General Practitioner", department = "Outpatient", phone = "+263772000008", email = "kmarufu@mpilo.co.zw"),
            DoctorEntity(firstName = "Zodwa", lastName = "Tshuma", specialization = "Psychiatrist", department = "Mental Health", phone = "+263772000009", email = "ztshuma@mpilo.co.zw"),
            DoctorEntity(firstName = "Blessing", lastName = "Gumbo", specialization = "Radiologist", department = "Radiology", phone = "+263772000010", email = "bgumbo@mpilo.co.zw"),
        )
        docs.forEach { db.doctorDao().insert(it) }

        // ---- Patients (10) ----
        val patients = listOf(
            PatientEntity(firstName = "Thabo", lastName = "Moyo", dateOfBirth = "1987-03-12", gender = "Male", phone = "+263772345678", email = "thabo@mail.co.zw", address = "14 Fife St, Bulawayo", emergencyContact = "+263772999111"),
            PatientEntity(firstName = "Naledi", lastName = "Ncube", dateOfBirth = "1995-07-22", gender = "Female", phone = "+263773111222", email = "naledi@mail.co.zw", address = "8 Josiah Tongogara, Bulawayo", emergencyContact = "+263772999222"),
            PatientEntity(firstName = "Tendai", lastName = "Dube", dateOfBirth = "1979-11-02", gender = "Male", phone = "+263774222333", email = "tendai@mail.co.zw", address = "22 Leopold Takawira, Bulawayo", emergencyContact = "+263772999333"),
            PatientEntity(firstName = "Sipho", lastName = "Sibanda", dateOfBirth = "2001-01-30", gender = "Male", phone = "+263775333444", email = "sipho@mail.co.zw", address = "5 Robert Mugabe Way, Bulawayo", emergencyContact = "+263772999444"),
            PatientEntity(firstName = "Rutendo", lastName = "Khumalo", dateOfBirth = "1990-09-15", gender = "Female", phone = "+263776444555", email = "rutendo@mail.co.zw", address = "31 George Silundika, Bulawayo", emergencyContact = "+263772999555"),
            PatientEntity(firstName = "Themba", lastName = "Nkomo", dateOfBirth = "1983-05-19", gender = "Male", phone = "+263777555666", email = "themba@mail.co.zw", address = "17 Jason Moyo St, Bulawayo", emergencyContact = "+263772999666"),
            PatientEntity(firstName = "Chipo", lastName = "Mutasa", dateOfBirth = "1998-12-08", gender = "Female", phone = "+263778666777", email = "chipo@mail.co.zw", address = "3 Main St, Bulawayo", emergencyContact = "+263772999777"),
            PatientEntity(firstName = "Mandla", lastName = "Ncube", dateOfBirth = "1965-02-27", gender = "Male", phone = "+263779777888", email = "mandla@mail.co.zw", address = "40 Lobengula St, Bulawayo", emergencyContact = "+263772999888"),
            PatientEntity(firstName = "Precious", lastName = "Ndebele", dateOfBirth = "2010-06-11", gender = "Female", phone = "+263771888999", email = "precious@mail.co.zw", address = "9 Fort St, Bulawayo", emergencyContact = "+263772999999"),
            PatientEntity(firstName = "Joseph", lastName = "Mpofu", dateOfBirth = "1972-08-04", gender = "Male", phone = "+263772999000", email = "joseph@mail.co.zw", address = "26 Selborne Ave, Bulawayo", emergencyContact = "+263772999000"),
        )
        patients.forEach { db.patientDao().insert(it) }

        // ---- Appointments (14, mixed statuses & dates) ----
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
            AppointmentEntity(patientId = 6, doctorId = 5, appointmentDate = offset(-7), appointmentTime = "10:30", status = "Completed", reason = "Knee pain after football"),
            AppointmentEntity(patientId = 7, doctorId = 6, appointmentDate = offset(-5), appointmentTime = "13:00", status = "Completed", reason = "Persistent skin rash"),
            AppointmentEntity(patientId = 8, doctorId = 8, appointmentDate = offset(1), appointmentTime = "15:00", status = "Confirmed", reason = "Blood pressure check"),
            AppointmentEntity(patientId = 9, doctorId = 4, appointmentDate = offset(3), appointmentTime = "09:45", status = "Pending", reason = "Childhood vaccination"),
            AppointmentEntity(patientId = 10, doctorId = 7, appointmentDate = offset(-12), appointmentTime = "11:00", status = "Completed", reason = "Recurring migraines"),
            AppointmentEntity(patientId = 4, doctorId = 9, appointmentDate = offset(7), appointmentTime = "14:00", status = "Pending", reason = "Stress and sleep problems"),
        )
        appts.forEach { db.appointmentDao().insert(it) }

        // ---- Prescriptions (12) ----
        val rx = listOf(
            PrescriptionEntity(appointmentId = 8, patientId = 1, doctorId = 1, medicationName = "Atenolol", dosage = "50 mg", frequency = "Once daily", duration = "Ongoing", issueDate = offset(-20), dispensed = true),
            PrescriptionEntity(appointmentId = 8, patientId = 1, doctorId = 1, medicationName = "Amoxicillin", dosage = "500 mg", frequency = "Twice daily", duration = "7 days", issueDate = offset(-2), dispensed = false),
            PrescriptionEntity(appointmentId = 5, patientId = 4, doctorId = 2, medicationName = "Ibuprofen", dosage = "400 mg", frequency = "Three times daily", duration = "5 days", issueDate = offset(-3), dispensed = false),
            PrescriptionEntity(appointmentId = 6, patientId = 5, doctorId = 2, medicationName = "Loratadine", dosage = "10 mg", frequency = "Once daily", duration = "14 days", issueDate = offset(-10), dispensed = true),
            PrescriptionEntity(appointmentId = 9, patientId = 6, doctorId = 5, medicationName = "Diclofenac", dosage = "50 mg", frequency = "Twice daily", duration = "7 days", issueDate = offset(-7), dispensed = true),
            PrescriptionEntity(appointmentId = 10, patientId = 7, doctorId = 6, medicationName = "Hydrocortisone cream", dosage = "1%", frequency = "Twice daily", duration = "10 days", issueDate = offset(-5), dispensed = false),
            PrescriptionEntity(appointmentId = 13, patientId = 10, doctorId = 7, medicationName = "Sumatriptan", dosage = "50 mg", frequency = "As needed", duration = "30 days", issueDate = offset(-12), dispensed = true),
            PrescriptionEntity(appointmentId = 13, patientId = 10, doctorId = 7, medicationName = "Paracetamol", dosage = "500 mg", frequency = "Three times daily", duration = "5 days", issueDate = offset(-12), dispensed = true),
            PrescriptionEntity(appointmentId = 5, patientId = 4, doctorId = 2, medicationName = "Paracetamol", dosage = "500 mg", frequency = "Three times daily", duration = "3 days", issueDate = offset(-3), dispensed = false),
            PrescriptionEntity(appointmentId = 6, patientId = 5, doctorId = 2, medicationName = "Cetirizine", dosage = "10 mg", frequency = "Once daily", duration = "14 days", issueDate = offset(-10), dispensed = false),
            PrescriptionEntity(appointmentId = 8, patientId = 1, doctorId = 1, medicationName = "Aspirin", dosage = "75 mg", frequency = "Once daily", duration = "Ongoing", issueDate = offset(-20), dispensed = true),
            PrescriptionEntity(appointmentId = 8, patientId = 1, doctorId = 1, medicationName = "Atorvastatin", dosage = "20 mg", frequency = "Once daily at night", duration = "Ongoing", issueDate = offset(-20), dispensed = false),
        )
        rx.forEach { db.prescriptionDao().insert(it) }

        // ---- Medication reminders (10) ----
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 1, patientId = 1, medicationName = "Atenolol", dosage = "50 mg", reminderTime = "08:00", isActive = true))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 2, patientId = 1, medicationName = "Amoxicillin", dosage = "500 mg", reminderTime = "20:00", isActive = true))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 3, patientId = 4, medicationName = "Ibuprofen", dosage = "400 mg", reminderTime = "07:30", isActive = true))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 4, patientId = 5, medicationName = "Loratadine", dosage = "10 mg", reminderTime = "09:00", isActive = false))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 5, patientId = 6, medicationName = "Diclofenac", dosage = "50 mg", reminderTime = "08:30", isActive = true))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 6, patientId = 7, medicationName = "Hydrocortisone cream", dosage = "1%", reminderTime = "19:00", isActive = true))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 7, patientId = 10, medicationName = "Sumatriptan", dosage = "50 mg", reminderTime = "12:00", isActive = false))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 9, patientId = 4, medicationName = "Paracetamol", dosage = "500 mg", reminderTime = "13:00", isActive = true))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 11, patientId = 1, medicationName = "Aspirin", dosage = "75 mg", reminderTime = "08:15", isActive = true))
        db.reminderDao().insert(MedicationReminderEntity(prescriptionId = 12, patientId = 1, medicationName = "Atorvastatin", dosage = "20 mg", reminderTime = "21:00", isActive = true))

        // ---- Emergencies (10: 3 active, 7 resolved) ----
        db.emergencyDao().insert(
            EmergencyEntity(patientId = 1, timestamp = System.currentTimeMillis() - 15 * 60_000, location = "Ward B-4, Mpilo Central Hospital, Bulawayo (-20.167000, 28.567000)", status = "Active", description = "Severe chest pain and shortness of breath")
        )
        db.emergencyDao().insert(
            EmergencyEntity(patientId = 3, timestamp = System.currentTimeMillis() - 2 * 86_400_000L, location = "Outpatient entrance, Mpilo Central Hospital, Bulawayo (-20.166400, 28.566500)", status = "Resolved", description = "Fainting episode in waiting area", resolvedAt = System.currentTimeMillis() - 2 * 86_400_000L + 30 * 60_000)
        )
        val dayMs = 86_400_000L
        val now = System.currentTimeMillis()
        listOf(
            Triple(2L, "Asthma attack, difficulty breathing", "Active"),
            Triple(9L, "Child with high fever and convulsions", "Active"),
            Triple(6L, "Deep cut to the leg, heavy bleeding", "Resolved"),
            Triple(7L, "Allergic reaction, swollen face", "Resolved"),
            Triple(8L, "Suspected stroke, slurred speech", "Resolved"),
            Triple(10L, "Severe migraine with vomiting", "Resolved"),
            Triple(4L, "Road accident, suspected fracture", "Resolved"),
            Triple(5L, "Fainting and dizziness at home", "Resolved"),
        ).forEachIndexed { i, (pid, desc, st) ->
            val ts = now - (i + 1) * dayMs / 2
            db.emergencyDao().insert(
                EmergencyEntity(
                    patientId = pid, timestamp = ts, location = "Bulawayo, Zimbabwe (-20.150000, 28.580000)",
                    status = st, description = desc,
                    resolvedAt = if (st == "Resolved") ts + 25 * 60_000 else null,
                )
            )
        }

        // ---- Payments (10) ----
        db.paymentDao().insert(PaymentEntity(appointmentId = 5, amount = 25.0, paymentMethod = "EcoCash", paymentDate = today, status = "Completed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 6, amount = 20.0, paymentMethod = "Cash", paymentDate = today, status = "Completed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 1, amount = 40.0, paymentMethod = "Medical Aid", paymentDate = today, status = "Pending"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 8, amount = 45.0, paymentMethod = "Medical Aid", paymentDate = offset(-20), status = "Completed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 9, amount = 35.0, paymentMethod = "EcoCash", paymentDate = offset(-7), status = "Completed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 10, amount = 30.0, paymentMethod = "Cash", paymentDate = offset(-5), status = "Completed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 13, amount = 50.0, paymentMethod = "Card", paymentDate = offset(-12), status = "Completed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 11, amount = 20.0, paymentMethod = "EcoCash", paymentDate = today, status = "Pending"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 7, amount = 20.0, paymentMethod = "Cash", paymentDate = offset(-1), status = "Failed"))
        db.paymentDao().insert(PaymentEntity(appointmentId = 3, amount = 40.0, paymentMethod = "Medical Aid", paymentDate = today, status = "Completed"))

        // ---- Staff (10) ----
        listOf(
            StaffEntity(firstName = "Lindiwe", lastName = "Ndlovu", role = "Nurse", department = "Maternity", phone = "+263772100001", email = "lndlovu@mpilo.co.zw"),
            StaffEntity(firstName = "Brian", lastName = "Chikomo", role = "Pharmacist", department = "Pharmacy", phone = "+263772100002", email = "bchikomo@mpilo.co.zw"),
            StaffEntity(firstName = "Faith", lastName = "Mpofu", role = "Emergency Responder", department = "Casualty", phone = "+263772100003", email = "fmpofu@mpilo.co.zw"),
            StaffEntity(firstName = "Grace", lastName = "Mutasa", role = "Nurse", department = "Outpatient", phone = "+263772100004", email = "gmutasa@mpilo.co.zw"),
            StaffEntity(firstName = "Collen", lastName = "Sithole", role = "Nurse", department = "Paediatrics", phone = "+263772100005", email = "csithole@mpilo.co.zw"),
            StaffEntity(firstName = "Rudo", lastName = "Zhou", role = "Pharmacist", department = "Pharmacy", phone = "+263772100006", email = "rzhou@mpilo.co.zw"),
            StaffEntity(firstName = "Musa", lastName = "Dlamini", role = "Emergency Responder", department = "Casualty", phone = "+263772100007", email = "mdlamini@mpilo.co.zw"),
            StaffEntity(firstName = "Tendekai", lastName = "Moyo", role = "Hospital Administrator", department = "Administration", phone = "+263772100008", email = "tmoyo@mpilo.co.zw"),
            StaffEntity(firstName = "Sandra", lastName = "Banda", role = "Receptionist", department = "Front Desk", phone = "+263772100009", email = "sbanda@mpilo.co.zw"),
            StaffEntity(firstName = "Elias", lastName = "Ncube", role = "Lab Technician", department = "Laboratory", phone = "+263772100010", email = "encube@mpilo.co.zw"),
        ).forEach { db.staffDao().insert(it) }
    }
}
