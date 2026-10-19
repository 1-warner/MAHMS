package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.common.patientName
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.LabeledValue
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.utils.Billing
import com.doodoobops.mahms.utils.DateUtils
import com.doodoobops.mahms.utils.PdfExporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Payments" tab of the patient Appointments screen (requirements FR15 and FR16):
 *  - Outstanding: visits that are confirmed or completed and not yet paid, each with a Pay now button.
 *  - Receipts: every completed payment, with a receipt the patient can view and save as a PDF.
 * The payment gateway is simulated (no real money moves); a real gateway such as Paynow would
 * replace the delay in [PayDialog].
 */
@Composable
fun PatientPaymentsContent(
    appointments: List<AppointmentEntity>,
    doctors: Map<Long, DoctorEntity>,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val patientId = container.session.currentPatientId
    val payments by container.payments.byPatient(patientId).collectAsStateWithLifecycle(emptyList())
    val patient by container.patients.observe(patientId).collectAsStateWithLifecycle(null)

    val paidIds = payments.filter { it.status == "Completed" }.map { it.appointmentId }.toSet()
    val outstanding = appointments.filter { it.status in listOf("Completed", "Confirmed") && it.appointmentId !in paidIds }
    val receipts = payments.filter { it.status == "Completed" }
    val apptById = appointments.associateBy { it.appointmentId }

    var paying by remember { mutableStateOf<AppointmentEntity?>(null) }
    var viewing by remember { mutableStateOf<PaymentEntity?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(Spacing.x4),
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        item { SectionHeader("Outstanding") }
        if (outstanding.isEmpty()) {
            item { Text("Nothing to pay right now.", style = MaterialTheme.typography.bodyMedium, color = Neutral500) }
        }
        items(outstanding, key = { "appt-${it.appointmentId}" }) { a ->
            val doctor = doctors[a.doctorId]
            MahmsCard {
                Text(doctorName(doctor), style = MaterialTheme.typography.titleMedium, color = Neutral900)
                Spacer(Modifier.height(Spacing.x1))
                DataText("${DateUtils.prettyDate(a.appointmentDate)} · ${a.appointmentTime}", color = Neutral500)
                Spacer(Modifier.height(Spacing.x1))
                DataText("Fee ${DateUtils.currency(Billing.consultationFee(doctor?.specialization))}")
                Spacer(Modifier.height(Spacing.x3))
                MahmsPrimaryButton("Pay now", onClick = { paying = a })
            }
        }

        item { SectionHeader("Receipts") }
        if (receipts.isEmpty()) {
            item { Text("Receipts appear here after you pay.", style = MaterialTheme.typography.bodyMedium, color = Neutral500) }
        }
        items(receipts, key = { "pay-${it.paymentId}" }) { p ->
            MahmsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DataText(Billing.receiptNumber(p.paymentId, p.paymentDate), modifier = Modifier.weight(1f))
                    MahmsStatusChip("Completed")
                }
                Spacer(Modifier.height(Spacing.x1))
                DataText("${DateUtils.currency(p.amount)} · ${p.paymentMethod} · ${DateUtils.prettyDate(p.paymentDate)}", color = Neutral500)
                Spacer(Modifier.height(Spacing.x3))
                MahmsSecondaryButton("View receipt", onClick = { viewing = p })
            }
        }
    }

    paying?.let { a ->
        val doctor = doctors[a.doctorId]
        val fee = Billing.consultationFee(doctor?.specialization)
        PayDialog(
            amount = fee,
            label = "${doctorName(doctor)} · ${DateUtils.prettyDate(a.appointmentDate)}",
            onDismiss = { paying = null },
            onConfirm = { method ->
                // An earlier Pending or Failed payment for this visit is completed, not duplicated.
                val earlier = payments.firstOrNull { it.appointmentId == a.appointmentId && it.status != "Completed" }
                val saved = (earlier ?: PaymentEntity(
                    // Time-based id so two phones can never create the same payment id.
                    paymentId = System.currentTimeMillis(),
                    appointmentId = a.appointmentId, amount = fee, paymentMethod = method,
                    paymentDate = DateUtils.todayStore(), status = "Completed",
                )).copy(amount = fee, paymentMethod = method, paymentDate = DateUtils.todayStore(), status = "Completed")
                val id = container.payments.insert(saved)
                paying = null
                viewing = saved.copy(paymentId = id)
            },
        )
    }

    viewing?.let { p ->
        val appt = apptById[p.appointmentId]
        ReceiptDialog(
            payment = p,
            appointment = appt,
            doctor = appt?.let { doctors[it.doctorId] },
            patientLabel = patientName(patient),
            onDismiss = { viewing = null },
        )
    }
}

@Composable
private fun PayDialog(
    amount: Double,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf(Billing.PAYMENT_METHODS.first()) }
    var processing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!processing) onDismiss() },
        title = { Text("Pay ${DateUtils.currency(amount)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = Neutral500)
                Billing.PAYMENT_METHODS.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !processing) { method = m }.padding(vertical = Spacing.x1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = method == m, onClick = null)
                        Spacer(Modifier.width(Spacing.x2))
                        Text(m, style = MaterialTheme.typography.bodyLarge, color = Neutral900)
                    }
                }
                Text(
                    "Payments are simulated in this prototype. No money moves.",
                    style = MaterialTheme.typography.bodySmall, color = Neutral500,
                )
            }
        },
        confirmButton = {
            MahmsPrimaryButton("Pay now", loading = processing, onClick = {
                processing = true
                scope.launch {
                    delay(900) // stands in for the payment gateway round trip
                    onConfirm(method)
                }
            })
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !processing) { Text("Cancel") } },
    )
}

@Composable
private fun ReceiptDialog(
    payment: PaymentEntity,
    appointment: AppointmentEntity?,
    doctor: DoctorEntity?,
    patientLabel: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var saveMessage by remember { mutableStateOf<String?>(null) }
    val number = Billing.receiptNumber(payment.paymentId, payment.paymentDate)
    val visit = appointment?.let { "${DateUtils.prettyDate(it.appointmentDate)} · ${it.appointmentTime}" } ?: "—"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receipt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                LabeledValue("Receipt number", number, mono = true)
                LabeledValue("Patient", patientLabel)
                LabeledValue("Doctor", doctorName(doctor))
                LabeledValue("Visit", visit, mono = true)
                LabeledValue("Paid on", DateUtils.prettyDate(payment.paymentDate), mono = true)
                LabeledValue("Method", payment.paymentMethod)
                LabeledValue("Amount", DateUtils.currency(payment.amount), mono = true)
                saveMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neutral500) }
            }
        },
        confirmButton = {
            MahmsSecondaryButton("Save as PDF", onClick = {
                val lines = listOf(
                    "Receipt number: $number",
                    "Patient: $patientLabel",
                    "Doctor: ${doctorName(doctor)}",
                    "Visit: $visit",
                    "Paid on: ${DateUtils.prettyDate(payment.paymentDate)}",
                    "Method: ${payment.paymentMethod}",
                    "Amount: ${DateUtils.currency(payment.amount)}",
                    "Status: Paid",
                )
                val where = PdfExporter.export(context, "receipt_$number", "Payment receipt $number", lines)
                AppLog.i("Payments", if (where != null) "Receipt $number saved as PDF" else "Receipt PDF could not be saved")
                saveMessage = if (where != null) "Saved to $where" else "Could not save the PDF on this phone."
            })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
