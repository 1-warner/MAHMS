package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.theme.CareTeal50
import com.doodoobops.mahms.ui.theme.CareTeal600
import com.doodoobops.mahms.ui.theme.Emergency700
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Bottom-sheet medication reminder from the design kit. "Taken" and "missed"
 * feed the adherence figure shown on the patient profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedReminderSheet(
    reminder: MedicationReminderEntity,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.x6, vertical = Spacing.x2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = CareTeal50, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Medication, contentDescription = null,
                        tint = CareTeal600, modifier = Modifier.size(30.dp),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.x3))
            Text(
                "MEDICATION REMINDER · ${reminder.reminderTime}",
                style = MaterialTheme.typography.labelMedium,
                color = Neutral500,
            )
            Spacer(Modifier.height(Spacing.x2))
            Text(
                "Time to take ${reminder.medicationName}",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Neutral900,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.x2))
            Text(
                "${reminder.dosage}, by mouth, with food.",
                style = MaterialTheme.typography.bodyLarge,
                color = Neutral500,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.x6))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                MahmsSecondaryButton(
                    "Snooze 10 min",
                    modifier = Modifier.weight(1f),
                    onClick = { onResult("Reminder snoozed for 10 minutes") },
                )
                MahmsPrimaryButton(
                    "Mark as taken",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            container.reminders.update(
                                reminder.copy(takenCount = reminder.takenCount + 1),
                            )
                            onResult("${reminder.medicationName} marked as taken")
                        }
                    },
                )
            }
            Spacer(Modifier.height(Spacing.x3))
            TextButton(onClick = {
                scope.launch {
                    container.reminders.update(
                        reminder.copy(missedCount = reminder.missedCount + 1),
                    )
                    onResult("Dose recorded as missed")
                }
            }) {
                Text("I missed this dose", color = Emergency700, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(Spacing.x4))
        }
    }
}
