package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

/** Standard outpatient consultation fee for this build. */
const val CONSULTATION_FEE = 25.0

private val METHODS = listOf("EcoCash", "Cash", "Medical Aid", "Bank Card")

/** Settles the consultation fee for one appointment. */
@Composable
fun PaymentDialog(
    appointment: AppointmentEntity,
    onDismiss: () -> Unit,
    onPaid: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf(METHODS.first()) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay consultation fee") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                DataText(DateUtils.currency(CONSULTATION_FEE))
                Text(
                    "${DateUtils.prettyDate(appointment.appointmentDate)} · ${appointment.appointmentTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral500,
                )
                Text("Payment method", style = MaterialTheme.typography.labelMedium, color = Neutral500)
                METHODS.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = method == m, onClick = { method = m })
                            .padding(vertical = Spacing.x1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = method == m, onClick = { method = m })
                        Text(m, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            MahmsPrimaryButton("Pay now", loading = busy, onClick = {
                busy = true
                scope.launch {
                    container.payments.insert(
                        PaymentEntity(
                            appointmentId = appointment.appointmentId,
                            amount = CONSULTATION_FEE,
                            paymentMethod = method,
                            paymentDate = DateUtils.todayStore(),
                            status = "Completed",
                        )
                    )
                    busy = false
                    onPaid("Payment of ${DateUtils.currency(CONSULTATION_FEE)} received")
                }
            })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
