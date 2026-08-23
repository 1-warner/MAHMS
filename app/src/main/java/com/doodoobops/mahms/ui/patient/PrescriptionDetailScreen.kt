package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.R
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.LabeledValue
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.MpiloBlue100
import com.doodoobops.mahms.ui.theme.MpiloBlue50
import com.doodoobops.mahms.ui.theme.MpiloBlue600
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils

/**
 * Formal e-prescription: hospital letterhead, patient and prescriber blocks,
 * the medication line, and a pharmacy code the dispensary can scan.
 */
@Composable
fun PrescriptionDetailScreen(
    rx: PrescriptionEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val patient by container.patients.observe(rx.patientId).collectAsStateWithLifecycle(null)
    val doctors by rememberDoctorMap()
    val doctor = doctors[rx.doctorId]

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("E-prescription", showBack = true, onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.x4),
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            // Letterhead
            MahmsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.mpilo_logo),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                    Column(Modifier.weight(1f).padding(start = Spacing.x3)) {
                        Text(
                            "Mpilo Central Hospital",
                            fontWeight = FontWeight.Bold,
                            color = Neutral900,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Bulawayo · Pharmacy 03",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral500,
                        )
                    }
                    MahmsStatusChip(if (rx.dispensed) "Dispensed" else "Active")
                }
            }

            // Patient & prescriber
            MahmsCard {
                Row {
                    LabeledValue(
                        "Patient",
                        patient?.let { "${it.firstName} ${it.lastName}" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    LabeledValue(
                        "Prescribed by",
                        doctor?.let { "Dr ${it.firstName} ${it.lastName}" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(Spacing.x3))
                Row {
                    LabeledValue("Issued", DateUtils.prettyDate(rx.issueDate), mono = true, modifier = Modifier.weight(1f))
                    LabeledValue(
                        "Reference",
                        "RX-%05d".format(rx.prescriptionId),
                        mono = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SectionHeader("Medication", Modifier.padding(0.dp))
            MahmsCard {
                Text(
                    rx.medicationName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Neutral900,
                )
                Spacer(Modifier.height(Spacing.x2))
                DataText("${rx.dosage} · ${rx.frequency}", color = Neutral900)
                Spacer(Modifier.height(Spacing.x1))
                DataText("Duration: ${rx.duration}", color = Neutral500)
                Spacer(Modifier.height(Spacing.x3))
                Text(
                    directions(rx),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Neutral900,
                )
            }

            // Pharmacist note
            Surface(
                shape = Radii.md,
                color = MpiloBlue50,
                border = androidx.compose.foundation.BorderStroke(1.dp, MpiloBlue100),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(Spacing.x4)) {
                    Icon(Icons.Filled.Info, null, tint = MpiloBlue600)
                    Text(
                        "Bring this prescription and your hospital ID to Pharmacy 03 on the ground floor. " +
                            "Open Mon–Fri 07:00–17:00, Sat 08:00–12:00.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neutral900,
                        modifier = Modifier.padding(start = Spacing.x3),
                    )
                }
            }

            // Pharmacy code
            SectionHeader("Pharmacy code", Modifier.padding(0.dp))
            MahmsCard {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(shape = Radii.md, color = Neutral25, modifier = Modifier.size(140.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.QrCode2,
                                contentDescription = "Pharmacy QR code",
                                tint = Neutral900,
                                modifier = Modifier.size(110.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.x2))
                    DataText("RX-%05d".format(rx.prescriptionId), color = Neutral500)
                    Text(
                        "Show this at the dispensary counter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral500,
                    )
                }
            }

            Text(
                "Confidential · Mpilo Central Hospital",
                style = MaterialTheme.typography.bodySmall,
                color = Neutral500,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.x6))
        }
    }
}

/** Human-readable dosing directions built from the stored fields. */
private fun directions(rx: PrescriptionEntity): String {
    val times = when (rx.frequency) {
        "Once daily" -> "once daily"
        "Twice daily" -> "twice daily"
        "Three times daily" -> "three times daily"
        else -> "as needed"
    }
    return "Take ${rx.dosage} by mouth, $times, after food. ${rx.duration}."
}
