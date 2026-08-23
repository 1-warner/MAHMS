package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.notifications.AppointmentReminderWorker
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

private val SLOTS = buildList {
    var minutes = 9 * 60
    while (minutes <= 16 * 60 + 30) {
        add("%02d:%02d".format(minutes / 60, minutes % 60))
        minutes += 30
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookAppointmentScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val departments by container.doctors.departments().collectAsStateWithLifecycle(emptyList())

    var department by remember { mutableStateOf<String?>(null) }
    var deptExpanded by remember { mutableStateOf(false) }
    var doctorExpanded by remember { mutableStateOf(false) }
    var selectedDoctorId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf<String?>(null) }
    var slot by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val doctors by (if (department != null) container.doctors.byDepartment(department!!) else container.doctors.all())
        .collectAsStateWithLifecycle(emptyList())
    val selectedDoctor = doctors.firstOrNull { it.doctorId == selectedDoctorId }

    val canConfirm = selectedDoctorId != null && date != null && slot != null && reason.isNotBlank()

    Column(Modifier.fillMaxSize()) {
        MahmsTopBar("Book appointment", showBack = true, onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.x4),
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            SectionHeader("Step 1 · Department & doctor", Modifier.padding(0.dp))
            ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                OutlinedTextField(
                    value = department ?: "Select department",
                    onValueChange = {}, readOnly = true,
                    label = { Text("Department") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    shape = Radii.md,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(deptExpanded, { deptExpanded = false }) {
                    departments.forEach { d ->
                        DropdownMenuItem(text = { Text(d) }, onClick = {
                            department = d; selectedDoctorId = null; deptExpanded = false
                        })
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = doctorExpanded, onExpandedChange = { doctorExpanded = it }) {
                OutlinedTextField(
                    value = selectedDoctor?.let { "Dr ${it.firstName} ${it.lastName}" } ?: "Select doctor",
                    onValueChange = {}, readOnly = true,
                    label = { Text("Doctor") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    shape = Radii.md,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(doctorExpanded, { doctorExpanded = false }) {
                    doctors.forEach { doc ->
                        DropdownMenuItem(
                            text = { Text("Dr ${doc.firstName} ${doc.lastName} · ${doc.specialization}") },
                            onClick = { selectedDoctorId = doc.doctorId; doctorExpanded = false },
                        )
                    }
                }
            }

            SectionHeader("Step 2 · Date & time", Modifier.padding(0.dp))
            Surface(
                shape = Radii.md, color = Neutral25,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePicker = true },
            ) {
                Box(Modifier.padding(Spacing.x4)) {
                    Text(date?.let { DateUtils.prettyDate(it) } ?: "Choose a date", color = Neutral500)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.x2), verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                SLOTS.forEach { s ->
                    FilterChip(selected = slot == s, onClick = { slot = s }, label = { Text(s) })
                }
            }

            SectionHeader("Step 3 · Reason for visit", Modifier.padding(0.dp))
            MahmsTextField(
                "Reason", reason, { if (it.length <= 500) reason = it },
                singleLine = false, keyboardType = KeyboardType.Text,
            )

            if (canConfirm) {
                SectionHeader("Step 4 · Confirm", Modifier.padding(0.dp))
                MahmsCard {
                    Text(
                        "Dr ${selectedDoctor!!.firstName} ${selectedDoctor.lastName} · ${selectedDoctor.specialization}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(Spacing.x2))
                    DataText("${DateUtils.prettyDate(date!!)} · $slot", color = Neutral500)
                    Spacer(Modifier.height(Spacing.x1))
                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = Neutral500)
                }
            }

            MahmsPrimaryButton(
                "Confirm booking",
                enabled = canConfirm,
                loading = saving,
                onClick = {
                    saving = true
                    scope.launch {
                        val newId = container.appointments.insert(
                            AppointmentEntity(
                                patientId = container.session.patientId,
                                doctorId = selectedDoctorId!!,
                                appointmentDate = date!!,
                                appointmentTime = slot!!,
                                status = "Pending",
                                reason = reason.trim(),
                            )
                        )
                        // Spec: schedule the reminder notification when the booking is made.
                        AppointmentReminderWorker.schedule(
                            context = context,
                            appointmentId = newId,
                            doctorName = "Dr ${selectedDoctor!!.firstName} ${selectedDoctor.lastName}",
                            date = date!!,
                            time = slot!!,
                        )
                        saving = false
                        onDone()
                    }
                },
            )
            Spacer(Modifier.height(Spacing.x6))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= System.currentTimeMillis() - 86_400_000L
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = DateUtils.storeFormat(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}
