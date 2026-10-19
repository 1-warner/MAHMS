package com.doodoobops.mahms.ui.doctor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DoctorScheduleScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val patients by rememberPatientMap()

    val week = remember { currentWeek() }
    var selected by remember { mutableStateOf(DateUtils.todayStore().takeIf { it in week } ?: week.first()) }
    val appts by container.appointments.byDoctorAndDate(container.session.currentDoctorId, selected).collectAsStateWithLifecycle(emptyList())
    val dow = remember { SimpleDateFormat("EEE", Locale.US) }

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Schedule")
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(Spacing.x4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            week.forEach { d ->
                val label = DateUtils.parseStore(d)?.let { dow.format(it) } ?: d
                FilterChip(
                    selected = selected == d,
                    onClick = { selected = d },
                    label = { Text("$label ${d.takeLast(2)}") },
                )
            }
        }
        if (appts.isEmpty()) {
            MahmsEmptyState(Icons.Filled.EventBusy, "Nothing booked", "No appointments for ${DateUtils.prettyDate(selected)}.")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(appts, key = { it.appointmentId }) { a ->
                    MahmsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(patientName(patients[a.patientId]), style = MaterialTheme.typography.titleMedium, color = Neutral900, modifier = Modifier.weight(1f))
                            MahmsStatusChip(a.status)
                        }
                        Spacer(Modifier.height(Spacing.x1))
                        DataText(a.appointmentTime, color = Neutral500)
                        Text(a.reason, style = MaterialTheme.typography.bodyMedium, color = Neutral500, maxLines = 2)
                        if (a.status != "Completed" && a.status != "Cancelled") {
                            Spacer(Modifier.height(Spacing.x3))
                            MahmsSecondaryButton("Mark attended", onClick = {
                                scope.launch {
                                    AppLog.i("Doctor", "Appointment ${a.appointmentId} marked attended")
                                    container.appointments.update(a.copy(status = "Completed"))
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

private fun currentWeek(): List<String> {
    val store = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    return (0 until 7).map { store.format(cal.time).also { cal.add(Calendar.DAY_OF_YEAR, 1) } }
}
