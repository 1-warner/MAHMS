package com.doodoobops.mahms.ui.doctor

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.doodoobops.mahms.ui.common.RoleProfileScreen
import com.doodoobops.mahms.ui.components.BottomNavItem
import com.doodoobops.mahms.ui.components.MahmsBottomNav
import com.doodoobops.mahms.ui.theme.Neutral25

private val tabs = listOf(
    BottomNavItem("home", "Home", Icons.Filled.Home),
    BottomNavItem("schedule", "Schedule", Icons.Filled.DateRange),
    BottomNavItem("prescribe", "Prescribe", Icons.Filled.EditNote),
    BottomNavItem("records", "Records", Icons.Filled.FolderShared),
    BottomNavItem("profile", "Profile", Icons.Filled.Person),
)

@Composable
fun DoctorHost(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf("home") }
    Scaffold(
        containerColor = Neutral25,
        bottomBar = { MahmsBottomNav(tabs, tab, onSelect = { tab = it }) },
    ) { padding ->
        val m = Modifier.padding(padding)
        when (tab) {
            "home" -> DoctorHomeScreen(m)
            "schedule" -> DoctorScheduleScreen(m)
            "prescribe" -> CreatePrescriptionScreen(m)
            "records" -> DoctorRecordsScreen(m)
            "profile" -> RoleProfileScreen(m, onLogout)
        }
    }
}
