package com.doodoobops.mahms.ui.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.common.patientName
import com.doodoobops.mahms.ui.common.rememberPatientMap
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsEmptyState
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils

@Composable
fun DoctorHomeScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val today = DateUtils.todayStore()
    val todayAppts by container.appointments.byDoctorAndDate(container.session.doctorId, today).collectAsStateWithLifecycle(emptyList())
    val rxToday by container.prescriptions.countForDoctorOnDate(container.session.doctorId, today).collectAsStateWithLifecycle(0)
    val patients by rememberPatientMap()

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Doctor dashboard")
        Column(Modifier.padding(horizontal = Spacing.x4)) {
            Spacer(Modifier.height(Spacing.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                Stat("Today", todayAppts.size.toString(), "appointments", Modifier.weight(1f))
                Stat("Issued", rxToday.toString(), "prescriptions today", Modifier.weight(1f))
            }
        }
        SectionHeader("Today's appointments")
        if (todayAppts.isEmpty()) {
            MahmsEmptyState(Icons.Filled.EventAvailable, "No appointments today", "Your schedule is clear.")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.x4, vertical = Spacing.x2),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(todayAppts.sortedBy { it.appointmentTime }, key = { it.appointmentId }) { a ->
                    MahmsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(patientName(patients[a.patientId]), style = MaterialTheme.typography.titleMedium, color = Neutral900, modifier = Modifier.weight(1f))
                            MahmsStatusChip(a.status)
                        }
                        Spacer(Modifier.height(Spacing.x1))
                        DataText(a.appointmentTime, color = Neutral500)
                        Text(a.reason, style = MaterialTheme.typography.bodyMedium, color = Neutral500)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    MahmsCard(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Neutral500)
        Text(value, style = MaterialTheme.typography.displaySmall, color = Neutral900)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = Neutral500)
    }
}
