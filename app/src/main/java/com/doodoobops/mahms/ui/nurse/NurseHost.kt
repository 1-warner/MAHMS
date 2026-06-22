package com.doodoobops.mahms.ui.nurse

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonitorHeart
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
    BottomNavItem("queue", "Queue", Icons.Filled.Groups),
    BottomNavItem("vitals", "Vitals", Icons.Filled.MonitorHeart),
    BottomNavItem("profile", "Profile", Icons.Filled.Person),
)

@Composable
fun NurseHost(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf("queue") }
    Scaffold(
        containerColor = Neutral25,
        bottomBar = { MahmsBottomNav(tabs, tab, onSelect = { tab = it }) },
    ) { padding ->
        val m = Modifier.padding(padding)
        when (tab) {
            "queue" -> NurseQueueScreen(m)
            "vitals" -> NurseVitalsScreen(m)
            "profile" -> RoleProfileScreen(m, onLogout)
        }
    }
}
