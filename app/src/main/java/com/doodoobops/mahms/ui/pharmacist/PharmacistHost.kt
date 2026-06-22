package com.doodoobops.mahms.ui.pharmacist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
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
    BottomNavItem("pending", "Pending", Icons.Filled.Medication),
    BottomNavItem("history", "History", Icons.Filled.History),
    BottomNavItem("profile", "Profile", Icons.Filled.Person),
)

@Composable
fun PharmacistHost(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf("pending") }
    Scaffold(
        containerColor = Neutral25,
        bottomBar = { MahmsBottomNav(tabs, tab, onSelect = { tab = it }) },
    ) { padding ->
        val m = Modifier.padding(padding)
        when (tab) {
            "pending" -> PharmacistPendingScreen(m)
            "history" -> PharmacistHistoryScreen(m)
            "profile" -> RoleProfileScreen(m, onLogout)
        }
    }
}
