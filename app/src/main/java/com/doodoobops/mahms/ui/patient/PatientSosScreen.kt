package com.doodoobops.mahms.ui.patient

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.api.NominatimApi
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.data.location.LocationProvider
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.theme.Emergency500
import com.doodoobops.mahms.ui.theme.Emergency700
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.utils.DateUtils
import com.doodoobops.mahms.utils.GeoUtils
import kotlinx.coroutines.launch

@Composable
fun PatientSosScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val myEmergencies by container.emergencies.byPatient(container.session.currentPatientId).collectAsStateWithLifecycle(emptyList())
    val active = myEmergencies.filter { it.status != "Resolved" }
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }
    var alertSent by remember { mutableStateOf(false) }
    var sosStatus by remember { mutableStateOf("Finding your location…") }

    fun startSos() {
        alertSent = false
        sosStatus = "Finding your location…"
        showConfirm = true
    }

    // Ask for location the first time. The alert is sent whether or not it is granted.
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        startSos()
    }

    val pulse = rememberInfiniteTransition(label = "sosPulse")
    val ring by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "ring",
    )

    Surface(color = Emergency500, modifier = modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(Spacing.x6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.x8))
            Text("Emergency SOS", color = Neutral0, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(Spacing.x2))
            Text(
                "Press the button to alert Casualty.",
                color = Neutral0.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(Spacing.x12))
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(120.dp).scale(ring).clip(CircleShape)
                        .background(Neutral0.copy(alpha = 0.25f)),
                )
                Surface(
                    shape = CircleShape, color = Neutral0, modifier = Modifier.size(120.dp),
                    onClick = {
                        if (LocationProvider.hasPermission(context)) {
                            startSos()
                        } else {
                            locationPermission.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Emergency, null, tint = Emergency500, modifier = Modifier.size(40.dp))
                            Text("SOS", color = Emergency700, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.x6))
            Text(
                "Only use in genuine emergencies",
                color = Neutral0.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(Spacing.x8))
            if (active.isNotEmpty()) {
                Text(
                    "Your active alerts",
                    color = Neutral0,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.x2))
                active.forEach { e -> SosEventCard(e) }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Close") } },
            title = { Text(if (alertSent) "Emergency alert sent" else "Sending alert") },
            text = { Text(sosStatus) },
        )
        // create the emergency exactly once when the dialog appears
        androidx.compose.runtime.LaunchedEffect(Unit) {
            scope.launch {
                // 1. real GPS fix from the phone (null without permission or signal)
                val fix = LocationProvider.currentLocation(context)
                // 2. readable address from the free OpenStreetMap Nominatim API (null when offline)
                val address = fix?.let { NominatimApi.reverseGeocode(it.latitude, it.longitude) }
                val where = if (fix != null) {
                    GeoUtils.formatLocation(fix.latitude, fix.longitude, address)
                } else {
                    "Location unavailable (permission off or no GPS signal)"
                }
                // 3. save the alert; it is also copied to Cloud Firestore for the responder
                val id = container.emergencies.insert(
                    EmergencyEntity(
                        patientId = container.session.currentPatientId,
                        timestamp = System.currentTimeMillis(),
                        location = where,
                        status = "Active",
                        description = "SOS triggered from the app",
                    )
                )
                AppLog.i("SOS", "Alert $id created (location ${if (fix != null) "found" else "missing"})")
                alertSent = true
                sosStatus = "Help is on the way. Stay where you are if it is safe to do so.\n\nLocation sent: $where"
            }
        }
    }
}

@Composable
private fun SosEventCard(e: EmergencyEntity) {
    Surface(
        shape = com.doodoobops.mahms.ui.theme.Radii.md,
        color = Neutral0,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.x1),
    ) {
        Row(Modifier.padding(Spacing.x4), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.description, style = MaterialTheme.typography.bodyMedium, color = Neutral900)
                DataText(DateUtils.prettyStamp(e.timestamp), color = com.doodoobops.mahms.ui.theme.Neutral500)
            }
            MahmsStatusChip(e.status)
        }
    }
}
