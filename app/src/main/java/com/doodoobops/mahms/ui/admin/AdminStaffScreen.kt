package com.doodoobops.mahms.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.data.entity.StaffEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.InitialsAvatar
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsStatusChip
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.theme.MpiloBlue500
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import kotlinx.coroutines.launch

private val STAFF_ROLES = listOf("Doctor", "Nurse", "Pharmacist", "Emergency Responder", "Administrator", "Receptionist")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val staff by container.staff.all().collectAsStateWithLifecycle(emptyList())
    var showAdd by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MahmsTopBar("Staff")
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                items(staff, key = { it.staffId }) { s ->
                    MahmsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar("${s.firstName.first()}${s.lastName.first()}", size = 40)
                            Column(Modifier.weight(1f).padding(start = Spacing.x3)) {
                                Text("${s.firstName} ${s.lastName}", style = MaterialTheme.typography.titleMedium, color = Neutral900)
                                Text(s.department, style = MaterialTheme.typography.bodySmall, color = Neutral500)
                            }
                            MahmsStatusChip(s.role)
                        }
                        Spacer(Modifier.height(Spacing.x2))
                        DataText(s.phone, color = Neutral500)
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            containerColor = MpiloBlue500,
            contentColor = Neutral0,
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.x4),
        ) { Icon(Icons.Filled.Add, "Add staff") }
    }

    if (showAdd) {
        var first by remember { mutableStateOf("") }
        var last by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(STAFF_ROLES.first()) }
        var roleExpanded by remember { mutableStateOf(false) }
        var dept by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        val canAdd = first.isNotBlank() && last.isNotBlank() && dept.isNotBlank()

        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                MahmsPrimaryButton("Add staff", enabled = canAdd, onClick = {
                    scope.launch {
                        AppLog.i("Admin", "Adding a staff member with role $role")
                        container.staff.insert(StaffEntity(firstName = first.trim(), lastName = last.trim(), role = role, department = dept.trim(), phone = phone.trim(), email = email.trim()))
                        showAdd = false
                    }
                })
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
            title = { Text("Add staff member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                        MahmsTextField("First name", first, { first = it }, modifier = Modifier.weight(1f))
                        MahmsTextField("Last name", last, { last = it }, modifier = Modifier.weight(1f))
                    }
                    ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                        OutlinedTextField(
                            value = role, onValueChange = {}, readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            shape = Radii.md,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(roleExpanded, { roleExpanded = false }) {
                            STAFF_ROLES.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { role = r; roleExpanded = false }) }
                        }
                    }
                    MahmsTextField("Department", dept, { dept = it })
                    MahmsTextField("Phone", phone, { phone = it })
                    MahmsTextField("Email", email, { email = it })
                }
            },
        )
    }
}
