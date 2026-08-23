package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.MahmsEmptyState
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.theme.MpiloBlue500
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.Neutral100
import com.doodoobops.mahms.ui.theme.Neutral800
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun PatientAppointmentsScreen(modifier: Modifier = Modifier, onBook: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val appts by container.appointments.byPatient(container.session.patientId).collectAsStateWithLifecycle(emptyList())
    val doctors by rememberDoctorMap()
    val payments by container.payments.byPatient(container.session.patientId).collectAsStateWithLifecycle(emptyList())
    val paidAppointments = payments.filter { it.status == "Completed" }.map { it.appointmentId }.toSet()
    var tab by remember { mutableStateOf(0) }
    var payingFor by remember { mutableStateOf<com.doodoobops.mahms.data.entity.AppointmentEntity?>(null) }

    val upcoming = appts.filter { it.status in listOf("Pending", "Confirmed") && DateUtils.isFutureDate(it.appointmentDate) }
    val past = appts.filterNot { it in upcoming }
    val shown = if (tab == 0) upcoming else past

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MahmsTopBar("Appointments")
            Row(Modifier.fillMaxWidth().padding(Spacing.x4), horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                SegTab("Upcoming", tab == 0, Modifier.weight(1f)) { tab = 0 }
                SegTab("Past", tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
            if (shown.isEmpty()) {
                MahmsEmptyState(
                    Icons.Filled.CalendarMonth,
                    if (tab == 0) "No upcoming appointments" else "No past appointments",
                    "Book a new appointment to get started.",
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                    verticalArrangement = Arrangement.spacedBy(Spacing.x3),
                ) {
                    items(shown, key = { it.appointmentId }) { a ->
                        PatientAppointmentCard(
                            a, doctors[a.doctorId],
                            onCancel = if (tab == 0) {
                                { scope.launch { container.appointments.update(a.copy(status = "Cancelled")) } }
                            } else null,
                            paid = a.appointmentId in paidAppointments,
                            onPay = { payingFor = a },
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onBook,
            containerColor = MpiloBlue500,
            contentColor = Neutral0,
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.x4),
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Book new") },
        )
    }

    payingFor?.let { appt ->
        PaymentDialog(
            appointment = appt,
            onDismiss = { payingFor = null },
            onPaid = { payingFor = null },
        )
    }
}

@Composable
private fun SegTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        shape = Radii.pill,
        color = if (selected) MpiloBlue500 else Neutral100,
        modifier = modifier.height(40.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Neutral0 else Neutral800, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
    }
}
