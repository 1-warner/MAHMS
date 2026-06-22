package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.doodoobops.mahms.ui.components.BottomNavItem
import com.doodoobops.mahms.ui.components.MahmsBottomNav
import com.doodoobops.mahms.ui.theme.Neutral25

private val tabs = listOf(
    BottomNavItem("home", "Home", Icons.Filled.Home),
    BottomNavItem("appts", "Appointments", Icons.Filled.CalendarMonth),
    BottomNavItem("meds", "Prescriptions", Icons.Filled.Medication),
    BottomNavItem("sos", "SOS", Icons.Filled.Emergency),
    BottomNavItem("profile", "Profile", Icons.Filled.Person),
)

@Composable
fun PatientHost(onLogout: () -> Unit, onBookAppointment: () -> Unit) {
    var tab by remember { mutableStateOf("home") }

    Scaffold(
        containerColor = Neutral25,
        bottomBar = { MahmsBottomNav(tabs, tab, onSelect = { tab = it }) },
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
