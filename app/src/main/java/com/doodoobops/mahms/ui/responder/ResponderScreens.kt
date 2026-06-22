package com.doodoobops.mahms.ui.responder

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
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.common.patientName
import com.doodoobops.mahms.ui.common.rememberPatientMap
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsDangerButton
import com.doodoobops.mahms.ui.components.MahmsEmptyState
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun ResponderActiveScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val active by container.emergencies.active().collectAsStateWithLifecycle(emptyList())
    val patients by rememberPatientMap()

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Active emergencies")
        if (active.isEmpty()) {
            MahmsEmptyState(Icons.Filled.NotificationsOff, "No active emergencies", "New SOS alerts will appear here.")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(active, key = { it.emergencyId }) { e ->
                    MahmsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(patientName(patients[e.patientId]), style = MaterialTheme.typography.titleMedium, color = Neutral900, modifier = Modifier.weight(1f))
                            MahmsStatusChip(e.status)
                        }
                        Spacer(Modifier.height(Spacing.x1))
                        DataText(DateUtils.prettyStamp(e.timestamp), color = Neutral500)
                        Text(e.location, style = MaterialTheme.typography.bodySmall, color = Neutral500)
                        Spacer(Modifier.height(Spacing.x1))
                        Text(e.description, style = MaterialTheme.typography.bodyMedium, color = Neutral900)
                        Spacer(Modifier.height(Spacing.x3))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                            if (e.status == "Active") {
                                MahmsSecondaryButton("Respond", modifier = Modifier.weight(1f), onClick = {
                                    scope.launch { container.emergencies.update(e.copy(status = "In Progress")) }
                                })
                            }
                            MahmsDangerButton("Resolve", modifier = Modifier.weight(1f), onClick = {
                                scope.launch { container.emergencies.update(e.copy(status = "Resolved", resolvedAt = System.currentTimeMillis())) }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResponderHistoryScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val resolved by container.emergencies.resolved().collectAsStateWithLifecycle(emptyList())
    val patients by rememberPatientMap()

    Column(modifier.fillMaxSize()) {
        MahmsTopBar("Resolved emergencies")
        if (resolved.isEmpty()) {
            MahmsEmptyState(Icons.Filled.CheckCircle, "No resolved emergencies", "Resolved incidents appear here.")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(resolved.sortedByDescending { it.timestamp }, key = { it.emergencyId }) { e ->
                    MahmsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(patientName(patients[e.patientId]), style = MaterialTheme.typography.titleMedium, color = Neutral900, modifier = Modifier.weight(1f))
                            MahmsStatusChip(e.status)
                        }
                        Spacer(Modifier.height(Spacing.x1))
                        DataText(DateUtils.prettyStamp(e.timestamp), color = Neutral500)
                        e.resolvedAt?.let { DataText("Resolved ${DateUtils.prettyStamp(it)}", color = Neutral500) }
                        Text(e.description, style = MaterialTheme.typography.bodyMedium, color = Neutral900)
                    }
                }
            }
        }
    }
}
