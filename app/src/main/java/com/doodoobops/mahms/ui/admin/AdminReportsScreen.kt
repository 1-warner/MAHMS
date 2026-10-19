package com.doodoobops.mahms.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import com.doodoobops.mahms.utils.PdfExporter
import kotlinx.coroutines.launch

private enum class ReportType(val label: String) {
    APPOINTMENT("Daily Appointment"),
    REVENUE("Revenue"),
    EMERGENCY("Emergency Incident"),
    PRESCRIPTION("Prescription Activity"),
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AdminReportsScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val appts by container.appointments.all().collectAsStateWithLifecycle(emptyList())
    val payments by container.payments.all().collectAsStateWithLifecycle(emptyList())
    val active by container.emergencies.active().collectAsStateWithLifecycle(emptyList())
    val resolved by container.emergencies.resolved().collectAsStateWithLifecycle(emptyList())
    val prescriptions by container.prescriptions.all().collectAsStateWithLifecycle(emptyList())

    var type by remember { mutableStateOf(ReportType.APPOINTMENT) }
    var from by remember { mutableStateOf(DateUtils.lastNDates(30).first()) }
    var to by remember { mutableStateOf(DateUtils.todayStore()) }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<List<String>?>(null) }

    fun inRange(date: String) = date in from..to

    fun generate(): List<String> = when (type) {
        ReportType.APPOINTMENT -> {
            val rows = appts.filter { inRange(it.appointmentDate) }
            listOf("Total appointments: ${rows.size}") +
                rows.groupingBy { it.status }.eachCount().map { "  ${it.key}: ${it.value}" }
        }
        ReportType.REVENUE -> {
            val rows = payments.filter { inRange(it.paymentDate) && it.status == "Completed" }
            listOf(
                "Completed payments: ${rows.size}",
                "Total revenue: ${DateUtils.currency(rows.sumOf { it.amount })}",
            ) + rows.map { "  ${DateUtils.prettyDate(it.paymentDate)} · ${it.paymentMethod} · ${DateUtils.currency(it.amount)}" }
        }
        ReportType.EMERGENCY -> {
            val rows = (active + resolved).filter { inRange(DateUtils.storeFormat(it.timestamp)) }
            listOf("Emergency incidents: ${rows.size}") +
                rows.map { "  ${DateUtils.prettyStamp(it.timestamp)} · ${it.status} · ${it.description}" }
        }
        ReportType.PRESCRIPTION -> {
            val rows = prescriptions.filter { inRange(it.issueDate) }
            listOf(
                "Prescriptions issued: ${rows.size}",
                "Dispensed: ${rows.count { it.dispensed }}",
            ) + rows.map { "  ${DateUtils.prettyDate(it.issueDate)} · ${it.medicationName} ${it.dosage}" }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Neutral25) { pad ->
        Column(modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            MahmsTopBar("Reports")
            Column(Modifier.padding(Spacing.x4), verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                SectionHeader("Report type", Modifier.padding(0.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                    ReportType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t; report = null }, label = { Text(t.label) })
                    }
                }

                SectionHeader("Date range", Modifier.padding(0.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    DateField("From", from, Modifier.weight(1f)) { pickFrom = true }
                    DateField("To", to, Modifier.weight(1f)) { pickTo = true }
                }

                MahmsPrimaryButton("Generate report", onClick = { report = generate() })

                report?.let { lines ->
                    MahmsCard {
                        Text("Mpilo Central Hospital | MAHMS System", style = MaterialTheme.typography.titleMedium, color = Neutral900)
                        Text("${type.label} · ${DateUtils.prettyDate(from)} – ${DateUtils.prettyDate(to)}", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                        Spacer(Modifier.height(Spacing.x3))
                        lines.forEach { DataText(it, color = Neutral900) }
                        Spacer(Modifier.height(Spacing.x3))
                        Text("Confidential", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                    }
                    MahmsSecondaryButton("Export as PDF", onClick = {
                        scope.launch {
                            AppLog.i("Admin", "Exporting the report as a PDF")
                            val dest = PdfExporter.export(
                                context,
                                fileName = "MAHMS_${type.name}_${from}_$to",
                                title = "${type.label} · ${DateUtils.prettyDate(from)} – ${DateUtils.prettyDate(to)}",
                                lines = lines,
                            )
                            snackbar.showSnackbar(if (dest != null) "Saved to $dest" else "Export failed")
                        }
                    })
                }
                Spacer(Modifier.height(Spacing.x6))
            }
        }
    }

    if (pickFrom) DateDialog(onPick = { from = it; pickFrom = false }, onDismiss = { pickFrom = false })
    if (pickTo) DateDialog(onPick = { to = it; pickTo = false }, onDismiss = { pickTo = false })
}

@Composable
private fun DateField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(shape = Radii.md, color = androidx.compose.ui.graphics.Color.White, modifier = modifier, onClick = onClick, border = androidx.compose.foundation.BorderStroke(1.dp, com.doodoobops.mahms.ui.theme.Neutral200)) {
        Column(Modifier.padding(Spacing.x3)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Neutral500)
            DataText(DateUtils.prettyDate(value))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(DateUtils.storeFormat(it)) } }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}
