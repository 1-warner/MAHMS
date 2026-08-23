package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils

/** Doctors keyed by id, for resolving names without showing raw IDs. */
@Composable
fun rememberDoctorMap(): State<Map<Long, DoctorEntity>> {
    val container = LocalAppContainer.current
    return container.doctors.all()
        .let { flow ->
            androidx.compose.runtime.produceState(initialValue = emptyMap<Long, DoctorEntity>(), flow) {
                flow.collect { list -> value = list.associateBy { it.doctorId } }
            }
        }
}

fun doctorName(d: DoctorEntity?): String =
    d?.let { "Dr ${it.firstName.first()}. ${it.lastName} · ${it.specialization}" } ?: "Doctor"

@Composable
fun PatientAppointmentCard(
    appt: AppointmentEntity,
    doctor: DoctorEntity?,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    paid: Boolean? = null,
    onPay: (() -> Unit)? = null,
) {
    MahmsCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                doctorName(doctor),
                style = MaterialTheme.typography.titleMedium,
                color = Neutral900,
                modifier = Modifier.weight(1f),
            )
            MahmsStatusChip(appt.status)
        }
        Spacer(Modifier.height(Spacing.x2))
        DataText("${DateUtils.prettyDate(appt.appointmentDate)} · ${appt.appointmentTime}", color = Neutral500)
        if (appt.reason.isNotBlank()) {
            Spacer(Modifier.height(Spacing.x1))
            Text(appt.reason, style = MaterialTheme.typography.bodyMedium, color = Neutral500)
        }
        if (paid != null && appt.status != "Cancelled") {
            Spacer(Modifier.height(Spacing.x3))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Consultation fee",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral500,
                    modifier = Modifier.weight(1f),
                )
                MahmsStatusChip(if (paid) "Completed" else "Pending")
            }
            if (!paid && onPay != null) {
                Spacer(Modifier.height(Spacing.x2))
                MahmsPrimaryButton("Pay now", onClick = onPay)
            }
        }
        if (onCancel != null && (appt.status == "Pending" || appt.status == "Confirmed")) {
            Spacer(Modifier.height(Spacing.x3))
            MahmsSecondaryButton("Cancel appointment", onClick = onCancel)
        }
    }
}
