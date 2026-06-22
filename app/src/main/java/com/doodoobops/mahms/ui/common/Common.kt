package com.doodoobops.mahms.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.InitialsAvatar
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsDangerButton
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.LabeledValue
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing

/** Patients keyed by id, for resolving names without exposing raw IDs. */
@Composable
fun rememberPatientMap(): State<Map<Long, PatientEntity>> {
    val container = LocalAppContainer.current
    val flow = container.patients.all()
    return produceState(initialValue = emptyMap(), flow) {
        flow.collect { list -> value = list.associateBy { it.patientId } }
    }
}

fun patientName(p: PatientEntity?): String =
    p?.let { "${it.firstName} ${it.lastName}" } ?: "Patient"

/** Generic profile tab shared by staff roles: shows the logged-in account + logout. */
@Composable
fun RoleProfileScreen(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    val session = LocalAppContainer.current.session
    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Profile")
        Column(
            Modifier.fillMaxSize().padding(Spacing.x4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x4),
        ) {
            Spacer(Modifier.height(Spacing.x4))
            val name = session.currentName ?: "MAHMS user"
            val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
            InitialsAvatar(initials, size = 84)
            Text(name, style = MaterialTheme.typography.headlineSmall, color = Neutral900)
            Text(session.currentRole ?: "", style = MaterialTheme.typography.bodyMedium, color = Neutral500)
            MahmsCard {
                LabeledValue("Role", session.currentRole ?: "—")
            }
            MahmsDangerButton("Log out", onClick = onLogout)
        }
    }
}
