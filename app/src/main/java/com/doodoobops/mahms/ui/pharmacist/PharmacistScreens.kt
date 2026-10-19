package com.doodoobops.mahms.ui.pharmacist

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.ui.common.patientName
import com.doodoobops.mahms.ui.common.rememberPatientMap
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsEmptyState
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.patient.rememberDoctorMap
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun PharmacistPendingScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val pending by container.prescriptions.pending().collectAsStateWithLifecycle(emptyList())
    val patients by rememberPatientMap()
    val doctors by rememberDoctorMap()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = com.doodoobops.mahms.ui.theme.Neutral25) { pad ->
        Column(modifier.fillMaxSize().padding(pad)) {
            MahmsTopBar("Pending dispensing")
            if (pending.isEmpty()) {
                MahmsEmptyState(Icons.Filled.Medication, "Nothing pending", "All prescriptions have been dispensed.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                    verticalArrangement = Arrangement.spacedBy(Spacing.x3),
                ) {
                    items(pending, key = { it.prescriptionId }) { rx ->
                        RxCard(
                            rx,
                            patient = patientName(patients[rx.patientId]),
                            doctorLast = doctors[rx.doctorId]?.lastName,
                        ) {
                            MahmsPrimaryButton("Dispense", onClick = {
                                scope.launch {
                                    AppLog.i("Pharmacy", "Dispensing prescription ${rx.prescriptionId}")
                                    container.prescriptions.update(rx.copy(dispensed = true))
                                    snackbar.showSnackbar("${rx.medicationName} dispensed")
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PharmacistHistoryScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val dispensed by container.prescriptions.dispensed().collectAsStateWithLifecycle(emptyList())
    val patients by rememberPatientMap()
    val doctors by rememberDoctorMap()

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Dispensing history")
        if (dispensed.isEmpty()) {
            MahmsEmptyState(Icons.Filled.CheckCircle, "No history yet", "Dispensed prescriptions appear here.")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(dispensed.sortedByDescending { it.issueDate }, key = { it.prescriptionId }) { rx ->
                    RxCard(rx, patientName(patients[rx.patientId]), doctors[rx.doctorId]?.lastName) {}
                }
            }
        }
    }
}

@Composable
private fun RxCard(
    rx: PrescriptionEntity,
    patient: String,
    doctorLast: String?,
    action: @Composable () -> Unit,
) {
    MahmsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rx.medicationName, style = MaterialTheme.typography.titleMedium, color = Neutral900, modifier = Modifier.weight(1f))
            MahmsStatusChip(if (rx.dispensed) "Dispensed" else "Pending")
        }
        Spacer(Modifier.height(Spacing.x1))
        DataText("${rx.dosage} · ${rx.frequency} · ${rx.duration}", color = Neutral500)
        Spacer(Modifier.height(Spacing.x2))
        Text("Patient: $patient", style = MaterialTheme.typography.bodyMedium, color = Neutral900)
        Text("Prescribed by Dr ${doctorLast ?: "—"} · issued ${DateUtils.prettyDate(rx.issueDate)}", style = MaterialTheme.typography.bodySmall, color = Neutral500)
        Spacer(Modifier.height(Spacing.x3))
        action()
    }
}
