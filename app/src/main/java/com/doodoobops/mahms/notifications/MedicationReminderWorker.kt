package com.doodoobops.mahms.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.doodoobops.mahms.utils.AppLog
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Posts a medication reminder notification. Enqueued as a daily periodic worker
 * whose first run lands on the reminder's HH:mm.
 */
class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val name = inputData.getString(KEY_NAME) ?: return Result.success()
        val dosage = inputData.getString(KEY_DOSAGE) ?: ""
        val id = inputData.getInt(KEY_ID, name.hashCode())
        AppLog.i("Reminder", "Reminder $id fired")
        NotificationHelper(applicationContext).showReminder(id, name, dosage)
        return Result.success()
    }

    companion object {
        const val KEY_NAME = "med_name"
        const val KEY_DOSAGE = "med_dosage"
        const val KEY_ID = "med_id"

        /** Schedule (or replace) a daily reminder for one medication at [hhmm] = "HH:mm". */
        fun schedule(context: Context, reminderId: Long, name: String, dosage: String, hhmm: String) {
            val (hour, minute) = parse(hhmm)
            val request = PeriodicWorkRequestBuilder<MedicationReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        KEY_NAME to name,
                        KEY_DOSAGE to dosage,
                        KEY_ID to reminderId.toInt(),
                    )
                )
                .build()
            AppLog.i("Reminder", "Scheduled daily reminder $reminderId at $hhmm")
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "reminder_$reminderId",
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context, reminderId: Long) {
            AppLog.i("Reminder", "Cancelled reminder $reminderId")
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$reminderId")
        }

        private fun parse(hhmm: String): Pair<Int, Int> {
            val parts = hhmm.split(":")
            return (parts.getOrNull(0)?.toIntOrNull() ?: 8) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }

        private fun initialDelayMillis(hour: Int, minute: Int): Long {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return next.timeInMillis - now.timeInMillis
        }
    }
}
