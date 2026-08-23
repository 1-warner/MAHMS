package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodoobops.mahms.ui.theme.Emergency500
import com.doodoobops.mahms.ui.theme.MpiloBlue600
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.Neutral100
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Radii

@Composable
fun PatientHost(onLogout: () -> Unit, onBookAppointment: () -> Unit) {
    var tab by remember { mutableStateOf("home") }

    Scaffold(
        containerColor = Neutral25,
        bottomBar = { PatientBottomNav(selected = tab, onSelect = { tab = it }) },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (tab) {
            "home" -> PatientHomeScreen(content, onBook = onBookAppointment, onGoToTab = { tab = it })
            "appts" -> PatientAppointmentsScreen(content, onBook = onBookAppointment)
            "meds" -> PatientPrescriptionsScreen(content)
            "sos" -> PatientSosScreen(content)
            "profile" -> PatientProfileScreen(content, onLogout = onLogout)
        }
    }
}

/**
 * Bottom navigation with the design kit's raised SOS capsule in the centre slot,
 * so emergency access is always one deliberate tap away.
 */
@Composable
private fun PatientBottomNav(selected: String, onSelect: (String) -> Unit) {
    Surface(color = Neutral0, shadowElevation = 8.dp) {
        Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Neutral100))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavSlot("home", "Home", Icons.Filled.Home, selected, onSelect, Modifier.weight(1f))
                NavSlot("appts", "Appts", Icons.Filled.CalendarMonth, selected, onSelect, Modifier.weight(1f))
                SosSlot(selected == "sos", onClick = { onSelect("sos") }, modifier = Modifier.weight(1f))
                NavSlot("meds", "Meds", Icons.Filled.Medication, selected, onSelect, Modifier.weight(1f))
                NavSlot("profile", "Profile", Icons.Filled.Person, selected, onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NavSlot(
    route: String,
    label: String,
    icon: ImageVector,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = selected == route
    val tint = if (active) MpiloBlue600 else Neutral500
    Column(
        modifier
            .clip(Radii.sm)
            .clickable { onSelect(route) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Raised red capsule that overlaps the nav's top edge. */
@Composable
private fun SosSlot(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Surface(
            color = Emergency500,
            shape = RoundedCornerShape(999.dp),
            shadowElevation = if (active) 2.dp else 6.dp,
            modifier = Modifier
                .padding(bottom = 14.dp)
                .width(58.dp)
                .height(46.dp)
                .clickable { onClick() },
        ) {
            Column(
                Modifier.padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Emergency,
                    contentDescription = "Emergency SOS",
                    tint = Neutral0,
                    modifier = Modifier.size(20.dp),
                )
                Text("SOS", color = Neutral0, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
