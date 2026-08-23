package com.doodoobops.mahms.ui.patient

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.theme.Emergency500
import com.doodoobops.mahms.ui.theme.Emergency700
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.delay

private enum class SosState { ARMED, SENT }

private const val HOLD_MILLIS = 2000

/**
 * Full-screen emergency surface. Following the design kit, the alert requires a
 * deliberate press-and-hold (~2s) so it cannot be fired by an accidental tap;
 * releasing early cancels. The siren pulse is the only looping animation here.
 */
@Composable
fun PatientSosScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val myEmergencies by container.emergencies
        .byPatient(container.session.patientId)
        .collectAsStateWithLifecycle(emptyList())
    val active = myEmergencies.filter { it.status != "Resolved" }

    var state by remember { mutableStateOf(SosState.ARMED) }
    var holding by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (holding) HOLD_MILLIS else 180,
            easing = LinearEasing,
        ),
        label = "holdProgress",
    )

    // Completing the hold is what actually raises the alert.
    LaunchedEffect(holding) {
        if (holding) {
            delay(HOLD_MILLIS.toLong())
            container.emergencies.insert(
                EmergencyEntity(
                    patientId = container.session.patientId,
                    timestamp = System.currentTimeMillis(),
                    location = "GPS not available (mock)",
                    status = "Active",
                    description = "SOS triggered from the app",
                )
            )
            state = SosState.SENT
            holding = false
        }
    }

    val pulse = rememberInfiniteTransition(label = "sosPulse")
    val ring by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "ring",
    )

    Surface(color = Emergency500, modifier = modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(Spacing.x6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.x6))
            Text(
                if (state == SosState.SENT) "ALERT SENT" else "EMERGENCY SOS",
                color = Neutral0.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(Spacing.x2))
            Text(
                if (state == SosState.SENT) "Help is on the way." else "Hold the button to alert Casualty.",
                color = Neutral0,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.x2))
            Text(
                if (state == SosState.SENT) {
                    "Casualty has been notified. Stay where you are if it is safe to do so."
                } else {
                    "Your name and location are sent to the emergency desk. Use only for life-threatening situations."
                },
                color = Neutral0.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(Spacing.x8))
            Box(contentAlignment = Alignment.Center) {
                if (state != SosState.SENT) {
                    Box(
                        Modifier.size(150.dp).scale(ring).clip(CircleShape)
                            .background(Neutral0.copy(alpha = 0.2f)),
                    )
                }
                if (holding) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(158.dp),
                        color = Neutral0,
                        strokeWidth = 6.dp,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Neutral0,
                    modifier = Modifier
                        .size(140.dp)
                        .pointerInput(state) {
                            if (state == SosState.SENT) return@pointerInput
                            detectTapGestures(
                                onPress = {
                                    holding = true
                                    tryAwaitRelease()
                                    // Released or cancelled: abort unless the hold already fired.
                                    holding = false
                                },
                            )
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (state == SosState.SENT) Icons.Filled.Check else Icons.Filled.Emergency,
                                contentDescription = null,
                                tint = Emergency500,
                                modifier = Modifier.size(44.dp),
                            )
                            Text(
                                if (state == SosState.SENT) "SENT" else "SOS",
                                color = Emergency700,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.x5))
            Text(
                when {
                    state == SosState.SENT -> "A responder will contact you shortly."
                    holding -> "Keep holding…"
                    else -> "Only use in genuine emergencies"
                },
                color = Neutral0.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(Spacing.x6))
            if (active.isNotEmpty()) {
                Text(
                    "YOUR ACTIVE ALERTS",
                    color = Neutral0,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.x2))
                active.forEach { e -> SosEventCard(e) }
            }
        }
    }
}

@Composable
private fun SosEventCard(e: EmergencyEntity) {
    Surface(
        shape = Radii.md,
        color = Neutral0,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.x1),
    ) {
        Row(Modifier.padding(Spacing.x4), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.description, style = MaterialTheme.typography.bodyMedium, color = Neutral900)
                DataText(DateUtils.prettyStamp(e.timestamp), color = Neutral500)
            }
            MahmsStatusChip(e.status)
        }
    }
}
