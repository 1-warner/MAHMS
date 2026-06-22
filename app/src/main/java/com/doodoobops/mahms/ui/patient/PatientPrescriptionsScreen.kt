package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.domain.model.Demo
import com.doodoobops.mahms.notifications.MedicationReminderWorker
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsEmptyState
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.theme.CareTeal400
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun PatientPrescriptionsScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prescriptions by container.prescriptions.byPatient(Demo.PATIENT_ID).collectAsStateWithLifecycle(emptyList())
    val reminders by container.reminders.byPatient(Demo.PATIENT_ID).collectAsStateWithLifecycle(emptyList())
    val reminderByRx = reminders.associateBy { it.prescriptionId }

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Prescriptions")
        if (prescriptions.isEmpty()) {
            MahmsEmptyState(Icons.Filled.Medication, "No prescriptions", "Prescriptions issued by your doctor appear here.")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(prescriptions, key = { it.prescriptionId }) { rx ->
                    PrescriptionCard(
                        rx,
                        reminder = reminderByRx[rx.prescriptionId],
                        onToggle = { reminder, active ->
                            scope.launch {
                                if (reminder != null) {
                                    container.reminders.update(reminder.copy(isActive = active))
                                    if (active) {
                                        MedicationReminderWorker.schedule(
                                            context, reminder.reminderId,
                                            reminder.medicationName, reminder.dosage, reminder.reminderTime,
                                        )
                                    } else {
                                        MedicationReminderWorker.cancel(context, reminder.reminderId)
                                    }
                                } else if (active) {
                                    // create a reminder for this prescription (default 08:00)
                                    val id = container.reminders.insert(
                                        MedicationReminderEntity(
                                            prescriptionId = rx.prescriptionId,
                                            patientId = Demo.PATIENT_ID,
                                            medicationName = rx.medicationName,
                                            dosage = rx.dosage,
                                            reminderTime = "08:00",
                                            isActive = true,
                                        )
                                    )
                                    MedicationReminderWorker.schedule(context, id, rx.medicationName, rx.dosage, "08:00")
                                }
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(Spacing.x6)) }
            }
        }
    }
}

@Composable
private fun PrescriptionCard(
    rx: PrescriptionEntity,
    reminder: MedicationReminderEntity?,
    onToggle: (MedicationReminderEntity?, Boolean) -> Unit,
) {
    MahmsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rx.medicationName, style = MaterialTheme.typography.titleMedium, color = Neutral900, modifier = Modifier.weight(1f))
            MahmsStatusChip(if (rx.dispensed) "Dispensed" else "Active")
        }
        Spacer(Modifier.height(Spacing.x2))
        DataText("${rx.dosage} · ${rx.frequency}", color = Neutral500)
        DataText("Duration: ${rx.duration}", color = Neutral500)
        Spacer(Modifier.height(Spacing.x1))
        Text("Issued ${DateUtils.prettyDate(rx.issueDate)}", style = MaterialTheme.typography.bodySmall, color = Neutral500)
        Spacer(Modifier.height(Spacing.x3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reminder", style = MaterialTheme.typography.bodyMedium, color = Neutral900, modifier = Modifier.weight(1f))
            Switch(
                checked = reminder?.isActive == true,
                onCheckedChange = { onToggle(reminder, it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = CareTeal400),
            )
        }
    }
}
