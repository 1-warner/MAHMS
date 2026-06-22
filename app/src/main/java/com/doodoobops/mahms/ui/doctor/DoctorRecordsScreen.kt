package com.doodoobops.mahms.ui.doctor

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.common.patientName
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.InitialsAvatar
import com.doodoobops.mahms.ui.components.LabeledValue
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils

@Composable
fun DoctorRecordsScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PatientEntity?>(null) }
    val results by container.patients.search(query).collectAsStateWithLifecycle(emptyList())

    val chosen = selected
    if (chosen != null) {
        PatientDetail(chosen, onBack = { selected = null })
        return
    }

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Patient records")
        Column(Modifier.padding(Spacing.x4)) {
            MahmsTextField("Search by name or phone", query, { query = it }, trailingIcon = {
                androidx.compose.material3.Icon(Icons.Filled.Search, null)
            })
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.x4),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            items(results, key = { it.patientId }) { p ->
                Surface(
                    shape = com.doodoobops.mahms.ui.theme.Radii.md,
                    color = com.doodoobops.mahms.ui.theme.Neutral0,
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.doodoobops.mahms.ui.theme.Neutral100),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selected = p },
                ) {
                    Row(Modifier.padding(Spacing.x3), verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar("${p.firstName.first()}${p.lastName.first()}", size = 40)
                        Spacer(Modifier.weight(0.05f))
                        Column(Modifier.weight(1f).padding(start = Spacing.x3)) {
                            Text(patientName(p), style = MaterialTheme.typography.titleMedium, color = Neutral900)
                            DataText(p.phone, color = Neutral500)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientDetail(p: PatientEntity, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val appts by container.appointments.byPatient(p.patientId).collectAsStateWithLifecycle(emptyList())
    val rx by container.prescriptions.byPatient(p.patientId).collectAsStateWithLifecycle(emptyList())

    Column(Modifier.fillMaxSize()) {
        MahmsTopBar(patientName(p), showBack = true, onBack = onBack)
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            item {
                MahmsCard {
                    LabeledValue("Date of birth", p.dateOfBirth, mono = true)
                    Spacer(Modifier.height(Spacing.x2))
                    LabeledValue("Gender", p.gender)
                    Spacer(Modifier.height(Spacing.x2))
                    LabeledValue("Phone", p.phone, mono = true)
                    Spacer(Modifier.height(Spacing.x2))
                    LabeledValue("Address", p.address)
                    if (p.notes.isNotBlank()) {
                        Spacer(Modifier.height(Spacing.x2))
                        LabeledValue("Clinical notes", p.notes)
                    }
                }
            }
            item { SectionHeader("Appointment history", Modifier.padding(0.dp)) }
            items(appts, key = { "a${it.appointmentId}" }) { a ->
                MahmsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DataText("${DateUtils.prettyDate(a.appointmentDate)} · ${a.appointmentTime}", color = Neutral500, modifier = Modifier.weight(1f))
                        MahmsStatusChip(a.status)
                    }
                    Text(a.reason, style = MaterialTheme.typography.bodyMedium, color = Neutral900)
                }
            }
            item { SectionHeader("Prescription history", Modifier.padding(0.dp)) }
            items(rx, key = { "r${it.prescriptionId}" }) { r ->
                MahmsCard {
                    Text(r.medicationName, style = MaterialTheme.typography.titleMedium, color = Neutral900)
                    DataText("${r.dosage} · ${r.frequency} · ${r.duration}", color = Neutral500)
                    Text("Issued ${DateUtils.prettyDate(r.issueDate)}", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                }
            }
            item { Spacer(Modifier.height(Spacing.x6)) }
        }
    }
}
