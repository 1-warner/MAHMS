package com.doodoobops.mahms.ui.admin

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
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.UserEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.InitialsAvatar
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsEmptyState
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsSecondaryButton
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * Administrator view of every login account, with the password reset that the
 * "contact your administrator" message on the login screen points at.
 */
@Composable
fun AdminAccountsScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val users by container.users.all().collectAsStateWithLifecycle(emptyList())
    var resetting by remember { mutableStateOf<UserEntity?>(null) }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Neutral25) { pad ->
        Column(modifier.fillMaxSize().padding(pad)) {
            MahmsTopBar("Accounts")
            if (users.isEmpty()) {
                MahmsEmptyState(Icons.Filled.ManageAccounts, "No accounts", "Registered accounts appear here.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                    verticalArrangement = Arrangement.spacedBy(Spacing.x3),
                ) {
                    items(users, key = { it.userId }) { u ->
                        MahmsCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InitialsAvatar(
                                    u.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                                        .take(2).joinToString(""),
                                    size = 40,
                                )
                                Column(Modifier.weight(1f).padding(start = Spacing.x3)) {
                                    Text(u.fullName, style = MaterialTheme.typography.titleMedium, color = Neutral900)
                                    Text(u.email, style = MaterialTheme.typography.bodySmall, color = Neutral500)
                                }
                                MahmsStatusChip(u.role)
                            }
                            Spacer(Modifier.height(Spacing.x2))
                            DataText(u.phone, color = Neutral500)
                            Spacer(Modifier.height(Spacing.x3))
                            MahmsSecondaryButton("Reset password", onClick = { resetting = u })
                        }
                    }
                }
            }
        }
    }

    resetting?.let { user ->
        var temp by remember(user.userId) { mutableStateOf("") }
        val err = ValidationUtils.passwordError(temp)
        AlertDialog(
            onDismissRequest = { resetting = null },
            title = { Text("Reset password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    Text(
                        "Set a temporary password for ${user.fullName}. Share it with them and ask them to change it after signing in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neutral500,
                    )
                    MahmsTextField(
                        "Temporary password", temp, { temp = it },
                        error = err.takeIf { temp.isNotEmpty() },
                    )
                }
            },
            confirmButton = {
                MahmsPrimaryButton("Set password", enabled = err == null, onClick = {
                    scope.launch {
                        container.users.setPassword(user.userId, temp)
                        resetting = null
                        snackbar.showSnackbar("Password reset for ${user.fullName}")
                    }
                })
            },
            dismissButton = { TextButton(onClick = { resetting = null }) { Text("Cancel") } },
        )
    }
}
