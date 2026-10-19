package com.doodoobops.mahms.ui.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.ui.common.patientName
import com.doodoobops.mahms.ui.common.rememberPatientMap
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

private val COMMON_MEDS = listOf(
    "Amoxicillin", "Paracetamol", "Ibuprofen", "Atenolol", "Metformin", "Amlodipine",
    "Omeprazole", "Loratadine", "Azithromycin", "Ciprofloxacin", "Salbutamol", "Prednisolone",
    "Cetirizine", "Diclofenac", "Hydrochlorothiazide", "Losartan", "Simvastatin", "Aspirin",
    "Codeine", "Erythromycin",
)
private val FREQUENCIES = listOf("Once daily", "Twice daily", "Three times daily", "As needed")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePrescriptionScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val patients by rememberPatientMap()

    var patientId by remember { mutableStateOf<Long?>(null) }
    var patientExpanded by remember { mutableStateOf(false) }
    var apptId by remember { mutableStateOf<Long?>(null) }
    var apptExpanded by remember { mutableStateOf(false) }
    var medName by remember { mutableStateOf("") }
    var medExpanded by remember { mutableStateOf(false) }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(FREQUENCIES.first()) }
    var freqExpanded by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf("") }

    val appts by (patientId?.let { container.appointments.byPatient(it) } ?: container.appointments.all())
        .collectAsStateWithLifecycle(emptyList())
    val eligibleAppts = appts.filter { it.status == "Confirmed" || it.status == "Completed" }
    val medMatches = COMMON_MEDS.filter { it.contains(medName, ignoreCase = true) }

    val canIssue = patientId != null && apptId != null && medName.isNotBlank() && dosage.isNotBlank() && duration.isNotBlank()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = com.doodoobops.mahms.ui.theme.Neutral25) { pad ->
        Column(modifier.fillMaxSize().padding(pad)) {
            MahmsTopBar("Create prescription")
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                SectionHeader("Patient & appointment", Modifier.padding(0.dp))
                DropdownField(
                    value = patientId?.let { patientName(patients[it]) } ?: "Select patient",
                    label = "Patient", expanded = patientExpanded, onExpand = { patientExpanded = it },
                ) {
                    patients.values.forEach { p ->
                        DropdownMenuItem(text = { Text(patientName(p)) }, onClick = {
                            patientId = p.patientId; apptId = null; patientExpanded = false
                        })
                    }
                }
                DropdownField(
                    value = apptId?.let { id -> eligibleAppts.firstOrNull { it.appointmentId == id }?.let { "${DateUtils.prettyDate(it.appointmentDate)} · ${it.appointmentTime}" } } ?: "Select appointment",
                    label = "Appointment", expanded = apptExpanded, onExpand = { apptExpanded = it },
                ) {
                    eligibleAppts.forEach { a ->
                        DropdownMenuItem(
                            text = { Text("${DateUtils.prettyDate(a.appointmentDate)} · ${a.appointmentTime} · ${a.status}") },
                            onClick = { apptId = a.appointmentId; apptExpanded = false },
                        )
                    }
                }

                SectionHeader("Medication", Modifier.padding(0.dp))
                ExposedDropdownMenuBox(expanded = medExpanded && medMatches.isNotEmpty(), onExpandedChange = { medExpanded = it }) {
                    OutlinedTextField(
                        value = medName,
                        onValueChange = { medName = it; medExpanded = true },
                        label = { Text("Medication name") },
                        shape = Radii.md,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(medExpanded && medMatches.isNotEmpty(), { medExpanded = false }) {
                        medMatches.take(6).forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { medName = m; medExpanded = false })
                        }
                    }
                }
                MahmsTextField("Dosage (e.g. 500 mg)", dosage, { dosage = it })
                DropdownField(value = frequency, label = "Frequency", expanded = freqExpanded, onExpand = { freqExpanded = it }) {
                    FREQUENCIES.forEach { f ->
                        DropdownMenuItem(text = { Text(f) }, onClick = { frequency = f; freqExpanded = false })
                    }
                }
                MahmsTextField("Duration (e.g. 7 days)", duration, { duration = it })

                if (canIssue) {
                    SectionHeader("Preview", Modifier.padding(0.dp))
                    MahmsCard {
                        Text(medName, style = MaterialTheme.typography.titleMedium, color = Neutral900)
                        Spacer(Modifier.height(Spacing.x1))
                        DataText("$dosage · $frequency · $duration", color = Neutral500)
                        Text("For ${patientName(patients[patientId])}", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                    }
                }

                MahmsPrimaryButton("Issue prescription", enabled = canIssue, onClick = {
                    scope.launch {
                        AppLog.i("Prescription", "Doctor ${container.session.currentDoctorId} is issuing a prescription")
                        container.prescriptions.insert(
                            PrescriptionEntity(
                                appointmentId = apptId!!, patientId = patientId!!, doctorId = container.session.currentDoctorId,
                                medicationName = medName.trim(), dosage = dosage.trim(),
                                frequency = frequency, duration = duration.trim(),
                                issueDate = DateUtils.todayStore(),
                            )
                        )
                        // reset
                        medName = ""; dosage = ""; duration = ""; apptId = null
                        snackbar.showSnackbar("Prescription issued")
                    }
                })
                Spacer(Modifier.height(Spacing.x6))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    value: String,
    label: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    items: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpand) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            shape = Radii.md,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded, { onExpand(false) }, content = items)
    }
}
