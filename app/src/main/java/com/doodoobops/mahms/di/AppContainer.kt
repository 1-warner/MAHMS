package com.doodoobops.mahms.di

import android.content.Context
import com.doodoobops.mahms.data.db.MahmsDatabase
import com.doodoobops.mahms.data.repository.AppointmentRepository
import com.doodoobops.mahms.data.repository.DoctorRepository
import com.doodoobops.mahms.data.repository.EmergencyRepository
import com.doodoobops.mahms.data.repository.PatientRepository
import com.doodoobops.mahms.data.repository.PaymentRepository
import com.doodoobops.mahms.data.repository.PrescriptionRepository
import com.doodoobops.mahms.data.repository.ReminderRepository
import com.doodoobops.mahms.data.repository.StaffRepository
import com.doodoobops.mahms.data.repository.UserRepository
import com.doodoobops.mahms.notifications.NotificationHelper
import com.doodoobops.mahms.utils.SessionManager

/** Manual dependency container — owns the database, repositories, and session. */
class AppContainer(context: Context) {
    private val db = MahmsDatabase.get(context)

    val session = SessionManager(context)
    val notifications = NotificationHelper(context)

    val users = UserRepository(db.userDao(), db.patientDao(), db.doctorDao())
    val patients = PatientRepository(db.patientDao())
    val doctors = DoctorRepository(db.doctorDao())
    val appointments = AppointmentRepository(db.appointmentDao())
    val prescriptions = PrescriptionRepository(db.prescriptionDao())
    val emergencies = EmergencyRepository(db.emergencyDao())
    val payments = PaymentRepository(db.paymentDao())
    val reminders = ReminderRepository(db.reminderDao())
    val staff = StaffRepository(db.staffDao())

    val database: MahmsDatabase get() = db
}
