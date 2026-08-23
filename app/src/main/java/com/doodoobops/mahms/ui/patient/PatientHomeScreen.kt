package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.MpiloBlue500
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils

@Composable
fun PatientHomeScreen(
    modifier: Modifier = Modifier,
    onBook: () -> Unit,
    onGoToTab: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val patient by container.patients.observe(container.session.patientId).collectAsStateWithLifecycle(null)
    val appts by container.appointments.byPatient(container.session.patientId).collectAsStateWithLifecycle(emptyList())
    val activeReminders by container.reminders.countActive(container.session.patientId).collectAsStateWithLifecycle(0)
    val doctors by rememberDoctorMap()

    val upcoming = appts.filter { it.status in listOf("Pending", "Confirmed") && DateUtils.isFutureDate(it.appointmentDate) }
    val recent = appts.take(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.x4),
    ) {
        Spacer(Modifier.height(Spacing.x4))
        Text(
            "Good ${DateUtils.greeting()},",
            style = MaterialTheme.typography.bodyMedium,
            color = Neutral500,
        )
        Text(
            patient?.firstName ?: "there",
            style = MaterialTheme.typography.headlineMedium,
            color = Neutral900,
        )

        Spacer(Modifier.height(Spacing.x4))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
            StatCard("Upcoming", upcoming.size.toString(), "appointments", Modifier.weight(1f))
            StatCard("Active", activeReminders.toString(), "reminders", Modifier.weight(1f))
        }

        Spacer(Modifier.height(Spacing.x4))
        Surface(
            shape = Radii.md,
            color = MpiloBlue500,
            modifier = Modifier.fillMaxWidth().clickable { onBook() },
        ) {
            Row(Modifier.padding(Spacing.x5)) {
                Column(Modifier.weight(1f)) {
                    Text("Book an appointment", color = Neutral0, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(Spacing.x1))
                    Text("Find a doctor and reserve a time slot", color = Neutral0.copy(alpha = 0.85f), fontSize = 13.sp)
                }
                androidx.compose.material3.Icon(Icons.Filled.Add, null, tint = Neutral0)
            }
        }

        SectionHeader("Recent appointments", Modifier.padding(horizontal = 0.dp, vertical = Spacing.x4))
        if (recent.isEmpty()) {
            Text("No appointments yet.", color = Neutral500, style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                recent.forEach { a ->
                    PatientAppointmentCard(a, doctors[a.doctorId])
                }
            }
        }
        Spacer(Modifier.height(Spacing.x8))
    }
}

@Composable
private fun StatCard(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    MahmsCard(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Neutral500)
        Spacer(Modifier.height(Spacing.x1))
        Text(value, style = MaterialTheme.typography.displaySmall, color = Neutral900)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = Neutral500)
    }
}
