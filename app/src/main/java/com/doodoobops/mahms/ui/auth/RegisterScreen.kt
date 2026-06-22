package com.doodoobops.mahms.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.doodoobops.mahms.domain.model.Role
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.theme.MpiloBlue600
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.ValidationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RegisterScreen(onRegistered: () -> Unit, onHaveAccount: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.PATIENT) }
    var roleExpanded by remember { mutableStateOf(false) }

    var submitted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val nameErr = ValidationUtils.requiredError(name, "Full name")
    val emailErr = ValidationUtils.emailError(email)
    val phoneErr = ValidationUtils.phoneError(phone)
    val passErr = ValidationUtils.passwordError(password)
    val confirmErr = ValidationUtils.confirmError(password, confirm)
    val formValid = listOf(nameErr, emailErr, phoneErr, passErr, confirmErr).all { it == null }

    fun show(e: String?) = if (submitted) e else null

    Surface(color = Neutral25, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.x6),
            verticalArrangement = Arrangement.spacedBy(Spacing.x4),
        ) {
            Spacer(Modifier.height(Spacing.x4))
            Text("Create your account", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Register to access Mpilo Central services.",
                style = MaterialTheme.typography.bodyMedium,
                color = Neutral500,
            )

            MahmsTextField("Full name", name, { name = it }, error = show(nameErr))
            MahmsTextField("Email", email, { email = it }, error = show(emailErr), keyboardType = KeyboardType.Email)
            MahmsTextField("Phone (+263…)", phone, { phone = it }, error = show(phoneErr), keyboardType = KeyboardType.Phone, placeholder = "+263771234567")
            MahmsTextField("Password", password, { password = it }, error = show(passErr), isPassword = true)
            MahmsTextField("Confirm password", confirm, { confirm = it }, error = show(confirmErr), isPassword = true)

            ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                OutlinedTextField(
                    value = role.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Role") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    shape = Radii.md,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                    Role.all.forEach { r ->
                        DropdownMenuItem(text = { Text(r.label) }, onClick = {
                            role = r; roleExpanded = false
                        })
                    }
                }
            }

            generalError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            MahmsPrimaryButton(
                text = "Create account",
                enabled = formValid,
                loading = loading,
                onClick = {
                    submitted = true
                    if (!formValid) return@MahmsPrimaryButton
                    keyboard?.hide()
                    loading = true
                    generalError = null
                    scope.launch {
                        val existing = container.users.login(email, password)
                        // crude existing-email check via findByEmail-less path: rely on register insert
                        val id = container.users.register(name.trim(), email, ValidationUtils.normalizePhone(phone), password, role.label)
                        container.session.isRegistered = true
                        loading = false
                        if (id > 0 || existing != null) onRegistered() else generalError = "Could not create account"
                    }
                },
            )

            TextButton(onClick = onHaveAccount, modifier = Modifier.fillMaxWidth()) {
                Text("I already have an account", color = MpiloBlue600)
            }
            Spacer(Modifier.height(Spacing.x6))
        }
    }
}
