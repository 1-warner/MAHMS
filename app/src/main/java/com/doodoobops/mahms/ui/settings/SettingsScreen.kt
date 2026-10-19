package com.doodoobops.mahms.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.LabeledValue
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsDangerButton
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.ValidationUtils
import kotlinx.coroutines.launch

/** Settings available to every role: account, password, notifications, cloud sync, help. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenHelp: () -> Unit, onLogout: () -> Unit) {
    val container = LocalAppContainer.current
    val session = container.session
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var notifications by remember { mutableStateOf(session.notificationsEnabled) }

    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var changing by remember { mutableStateOf(false) }

    val newErr = ValidationUtils.passwordError(newPass)
    val confirmErr = ValidationUtils.confirmError(newPass, confirm)
    val currentErr = if (current.isBlank()) "Current password is required" else null

    Scaffold(
        containerColor = Neutral25,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { MahmsTopBar("Settings", showBack = true, onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.x4),
            verticalArrangement = Arrangement.spacedBy(Spacing.x4),
        ) {
            SectionHeader("Account")
            MahmsCard {
                LabeledValue("Name", session.currentName ?: "—")
                Spacer(Modifier.height(Spacing.x3))
                LabeledValue("Role", session.currentRole ?: "—")
            }

            SectionHeader("Notifications")
            MahmsCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Medication reminders", style = MaterialTheme.typography.titleMedium, color = Neutral900)
                        Text(
                            "Show a notification when it is time to take a medicine.",
                            style = MaterialTheme.typography.bodySmall, color = Neutral500,
                        )
                    }
                    Switch(checked = notifications, onCheckedChange = {
                        notifications = it
                        session.notificationsEnabled = it
                    })
                }
            }

            SectionHeader("Cloud sync")
            MahmsCard {
                Text(
                    "Your records are stored on this phone and kept in step with the hospital's online database (Cloud Firestore) in both directions when you are online.",
                    style = MaterialTheme.typography.bodySmall, color = Neutral500,
                )
                Spacer(Modifier.height(Spacing.x3))
                MahmsSecondaryButton("Sync now", onClick = {
                    val started = container.authRepo.syncNow(session.currentRole)
                    scope.launch {
                        snackbar.showSnackbar(
                            if (started) "Sync started. Records are downloading and uploading." else "Sign in with an online account to sync."
                        )
                    }
                })
            }

            if (container.authRepo.hasPassword()) {
                SectionHeader("Change password")
                MahmsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                        MahmsTextField("Current password", current, { current = it },
                            error = if (submitted) currentErr else null, isPassword = true)
                        MahmsTextField("New password", newPass, { newPass = it },
                            error = if (submitted) newErr else null, isPassword = true)
                        MahmsTextField("Confirm new password", confirm, { confirm = it },
                            error = if (submitted) confirmErr else null, isPassword = true)
                        MahmsPrimaryButton("Update password", loading = changing, onClick = {
                            submitted = true
                            if (currentErr != null || newErr != null || confirmErr != null) return@MahmsPrimaryButton
                            changing = true
                            scope.launch {
                                val error = container.authRepo.changePassword(session.currentUserId, current, newPass)
                                changing = false
                                if (error == null) {
                                    current = ""; newPass = ""; confirm = ""; submitted = false
                                    snackbar.showSnackbar("Password updated.")
                                } else {
                                    snackbar.showSnackbar(error)
                                }
                            }
                        })
                    }
                }
            }

            SectionHeader("Support")
            MahmsSecondaryButton("Help and user guide", onClick = onOpenHelp)

            SectionHeader("About")
            MahmsCard {
                LabeledValue("App", "MAHMS · Mpilo Central Hospital")
                Spacer(Modifier.height(Spacing.x3))
                LabeledValue("Version", "1.0", mono = true)
                Spacer(Modifier.height(Spacing.x3))
                LabeledValue("Team", "doodoobops")
            }

            MahmsDangerButton("Log out", onClick = onLogout)
            Spacer(Modifier.height(Spacing.x6))
        }
    }
}
