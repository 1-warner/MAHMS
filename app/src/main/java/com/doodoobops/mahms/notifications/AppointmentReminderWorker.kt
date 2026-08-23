package com.doodoobops.mahms.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.doodoobops.mahms.utils.DateUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Reminds the patient the day before a booked appointment. */
class AppointmentReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val doctor = inputData.getString(KEY_DOCTOR) ?: return Result.success()
        val whenText = inputData.getString(KEY_WHEN) ?: ""
        val id = inputData.getInt(KEY_ID, doctor.hashCode())
        NotificationHelper(applicationContext).showAppointmentReminder(id, doctor, whenText)
        return Result.success()
    }

    companion object {
        const val KEY_DOCTOR = "appt_doctor"
        const val KEY_WHEN = "appt_when"
        const val KEY_ID = "appt_id"

        /**
         * Schedules a one-off reminder 24 hours before the appointment. Appointments
         * booked for tomorrow or sooner fire a short confirmation instead.
         */
        fun schedule(
            context: Context,
            appointmentId: Long,
            doctorName: String,
            date: String,
            time: String,
        ) {
            val delay = delayMillis(date, time)
            val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        KEY_DOCTOR to doctorName,
                        KEY_WHEN to "${DateUtils.prettyDate(date)} at $time",
                        KEY_ID to appointmentId.toInt(),
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "appointment_$appointmentId",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context, appointmentId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("appointment_$appointmentId")
        }

        private fun delayMillis(date: String, time: String): Long {
            val day = DateUtils.parseStore(date) ?: return TimeUnit.MINUTES.toMillis(1)
            val hour = time.substringBefore(':').toIntOrNull() ?: 9
            val minute = time.substringAfter(':', "0").toIntOrNull() ?: 0
            val target = Calendar.getInstance().apply {
                timeInMillis = day
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                add(Calendar.DAY_OF_YEAR, -1) // remind a day ahead
            }.timeInMillis
            val delta = target - System.currentTimeMillis()
            return if (delta > 0) delta else TimeUnit.MINUTES.toMillis(1)
        }
    }
}
