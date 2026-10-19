package com.doodoobops.mahms.ui.nurse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.ui.common.patientName
import com.doodoobops.mahms.ui.common.rememberPatientMap
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsEmptyState
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun NurseQueueScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val patients by rememberPatientMap()
    val doctors by com.doodoobops.mahms.ui.patient.rememberDoctorMap()
    val today = DateUtils.todayStore()
    val queue by container.appointments.byDateAndStatus(today, "Confirmed").collectAsStateWithLifecycle(emptyList())

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Patient queue · today")
        if (queue.isEmpty()) {
            MahmsEmptyState(Icons.Filled.Groups, "Queue is empty", "Confirmed patients for today will appear here.")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(queue, key = { it.appointmentId }) { a ->
                    MahmsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(patientName(patients[a.patientId]), style = MaterialTheme.typography.titleMedium, color = Neutral900)
                                val doc = doctors[a.doctorId]
                                Text(
                                    doc?.let { "Dr ${it.lastName} · ${it.department}" } ?: "",
                                    style = MaterialTheme.typography.bodySmall, color = Neutral500,
                                )
                            }
                            DataText(a.appointmentTime, color = Neutral500)
                        }
                        Spacer(Modifier.height(Spacing.x3))
                        MahmsSecondaryButton("Mark arrived", onClick = {
                            scope.launch {
                                AppLog.i("Nurse", "Appointment ${a.appointmentId} marked arrived")
                                container.appointments.update(a.copy(status = "Completed"))
                            }
                        })
                    }
                }
            }
        }
    }
}
