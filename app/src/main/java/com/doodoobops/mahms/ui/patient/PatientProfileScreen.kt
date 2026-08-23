package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.InitialsAvatar
import com.doodoobops.mahms.ui.components.LabeledValue
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsDangerButton
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun PatientProfileScreen(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val patient by container.patients.observe(container.session.patientId).collectAsStateWithLifecycle(null)
    val reminders by container.reminders.byPatient(container.session.patientId)
        .collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf(false) }

    // Adherence = doses marked taken over all doses recorded from the reminder sheet.
    val taken = reminders.sumOf { it.takenCount }
    val missed = reminders.sumOf { it.missedCount }
    val logged = taken + missed
    val adherence = if (logged == 0) 0 else (taken * 100) / logged

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Profile")
        val p = patient
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.x4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            Spacer(Modifier.height(Spacing.x2))
            InitialsAvatar("${p?.firstName?.firstOrNull() ?: ""}${p?.lastName?.firstOrNull() ?: ""}", size = 84)
            Text("${p?.firstName ?: ""} ${p?.lastName ?: ""}", style = MaterialTheme.typography.headlineSmall, color = Neutral900)
            Text(p?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Neutral500)

            if (logged > 0) {
                MahmsCard {
                    Text(
                        "MEDICATION ADHERENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Neutral500,
                    )
                    Spacer(Modifier.height(Spacing.x3))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdherenceRing(adherence)
                        Column(Modifier.padding(start = Spacing.x4)) {
                            Text(
                                if (adherence >= 80) "On track" else "Needs attention",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Neutral900,
                            )
                            Spacer(Modifier.height(Spacing.x1))
                            Text(
                                "You have taken $taken of $logged doses on time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Neutral500,
                            )
                        }
                    }
                }
            }

            MahmsCard {
                LabeledValue("Phone", p?.phone ?: "—", mono = true)
                Spacer(Modifier.height(Spacing.x3))
                LabeledValue("Date of birth", p?.dateOfBirth ?: "—", mono = true)
                Spacer(Modifier.height(Spacing.x3))
                LabeledValue("Address", p?.address ?: "—")
                Spacer(Modifier.height(Spacing.x3))
                LabeledValue("Emergency contact", p?.emergencyContact ?: "—", mono = true)
            }

            MahmsSecondaryButton("Edit profile", onClick = { editing = true }, enabled = p != null)
            MahmsDangerButton("Log out", onClick = onLogout)
            Spacer(Modifier.height(Spacing.x6))
        }
    }

    if (editing && patient != null) {
        val p = patient!!
        var phone by remember { mutableStateOf(p.phone) }
        var address by remember { mutableStateOf(p.address) }
        var contact by remember { mutableStateOf(p.emergencyContact) }
        AlertDialog(
            onDismissRequest = { editing = false },
            confirmButton = {
                MahmsPrimaryButton("Save", onClick = {
                    scope.launch {
                        container.patients.update(p.copy(phone = phone, address = address, emergencyContact = contact))
                        editing = false
                    }
                })
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
            title = { Text("Edit profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    MahmsTextField("Phone", phone, { phone = it })
                    MahmsTextField("Address", address, { address = it })
                    MahmsTextField("Emergency contact", contact, { contact = it })
                }
            },
        )
    }
}
