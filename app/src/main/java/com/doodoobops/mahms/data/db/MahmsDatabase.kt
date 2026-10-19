package com.doodoobops.mahms.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.data.entity.StaffEntity
import com.doodoobops.mahms.data.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class,
        DoctorEntity::class,
        AppointmentEntity::class,
        PrescriptionEntity::class,
        EmergencyEntity::class,
        PaymentEntity::class,
        MedicationReminderEntity::class,
        StaffEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MahmsDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun patientDao(): PatientDao
    abstract fun doctorDao(): DoctorDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun emergencyDao(): EmergencyDao
    abstract fun paymentDao(): PaymentDao
    abstract fun reminderDao(): MedicationReminderDao
    abstract fun staffDao(): StaffDao

    companion object {
        @Volatile
        private var instance: MahmsDatabase? = null

        /**
         * Version 2 adds payments.receiptNo, the receipt number issued by the MAHMS API.
         * A real migration rather than a destructive one, so a phone that already has
         * payments in it keeps them when the app is updated.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payments ADD COLUMN receiptNo TEXT")
            }
        }

        fun get(context: Context): MahmsDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): MahmsDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MahmsDatabase::class.java,
                "mahms.db",
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys=ON;")
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
    }
}
