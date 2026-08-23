package com.doodoobops.mahms.ui.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ManageAccounts
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
    BottomNavItem("dashboard", "Dashboard", Icons.Filled.BarChart),
    BottomNavItem("staff", "Staff", Icons.Filled.Badge),
    BottomNavItem("accounts", "Accounts", Icons.Filled.ManageAccounts),
    BottomNavItem("reports", "Reports", Icons.Filled.Description),
    BottomNavItem("profile", "Profile", Icons.Filled.Person),
)

@Composable
fun AdminHost(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf("dashboard") }
    Scaffold(
        containerColor = Neutral25,
        bottomBar = { MahmsBottomNav(tabs, tab, onSelect = { tab = it }) },
    ) { padding ->
        val m = Modifier.padding(padding)
        when (tab) {
            "dashboard" -> AdminDashboardScreen(m)
            "staff" -> AdminStaffScreen(m)
            "accounts" -> AdminAccountsScreen(m)
            "reports" -> AdminReportsScreen(m)
            "profile" -> RoleProfileScreen(m, onLogout)
        }
    }
}
