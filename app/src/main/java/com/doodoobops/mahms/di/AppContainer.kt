package com.doodoobops.mahms.di

import android.content.Context
import com.doodoobops.mahms.data.cloud.CloudSync
import com.doodoobops.mahms.data.cloud.FirebaseAuthRepository
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
import com.doodoobops.mahms.data.entity.UserEntity
import com.doodoobops.mahms.domain.model.Role
import com.doodoobops.mahms.notifications.NotificationHelper
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Manual dependency container — owns the database, repositories, and session. */
class AppContainer(context: Context) {
    private val db = MahmsDatabase.get(context)

    val session = SessionManager(context)
    val notifications = NotificationHelper(context)

    // Online services (Firebase): authentication + Cloud Firestore
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val cloud = CloudSync(db, firestore, firebaseAuth)

    val users = UserRepository(db.userDao())
    val authRepo = FirebaseAuthRepository(firebaseAuth, firestore, users, cloud)

    val patients = PatientRepository(db.patientDao(), cloud)
    val doctors = DoctorRepository(db.doctorDao())
    val appointments = AppointmentRepository(db.appointmentDao(), cloud)
    val prescriptions = PrescriptionRepository(db.prescriptionDao(), cloud)
    val emergencies = EmergencyRepository(db.emergencyDao(), cloud)
    val payments = PaymentRepository(db.paymentDao(), cloud)
    val reminders = ReminderRepository(db.reminderDao())
    val staff = StaffRepository(db.staffDao(), cloud)

    val database: MahmsDatabase get() = db

    /**
     * Starts the session for a signed-in account and links it to that account's OWN patient or
     * doctor record, so every screen shows the signed-in person's data (not a fixed demo record).
     */
    suspend fun startLinkedSession(user: UserEntity) {
        var patientId = -1L
        var doctorId = -1L
        when (Role.fromLabel(user.role)) {
            Role.PATIENT -> patientId = patients.findOrCreateFor(user)
            Role.DOCTOR -> doctorId = doctors.findOrCreateFor(user)
            else -> Unit
        }
        session.startSession(user.userId, user.role, user.fullName, patientId, doctorId)
        AppLog.i("Session", "Started ${user.role} session (patient=$patientId, doctor=$doctorId)")
    }
}
