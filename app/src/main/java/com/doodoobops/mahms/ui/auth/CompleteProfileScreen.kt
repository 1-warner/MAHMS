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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.doodoobops.mahms.data.cloud.AuthOutcome
import com.doodoobops.mahms.domain.model.Role
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.ValidationUtils
import kotlinx.coroutines.launch

@Composable
fun CompleteProfileScreen(
    uid: String,
    suggestedName: String,
    email: String,
    onComplete: () -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var name by remember { mutableStateOf(suggestedName) }
    var phone by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val nameErr = ValidationUtils.requiredError(name, "Full name")
    val phoneErr = if (phone.isBlank()) null else ValidationUtils.phoneError(phone)
    val formValid = nameErr == null && phoneErr == null

    fun show(e: String?) = if (submitted) e else null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Neutral25,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.x6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x4),
        ) {
            Spacer(Modifier.height(Spacing.x8))
            Text("Complete your profile", style = MaterialTheme.typography.headlineSmall)
            Text(
                "One last step before you get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = Neutral500,
            )
            Spacer(Modifier.height(Spacing.x2))

            MahmsTextField(
                label = "Full name",
                value = name,
                onChange = { name = it },
                error = show(nameErr),
            )
            MahmsTextField(
                label = "Email",
                value = email,
                onChange = {},
                keyboardType = KeyboardType.Email,
            )
            MahmsTextField(
                label = "Phone (optional)",
                value = phone,
                onChange = { phone = it },
                error = show(phoneErr),
                keyboardType = KeyboardType.Phone,
            )

            // Self-registration creates patient accounts only.
            // Staff accounts are issued by the hospital administrator.
            MahmsTextField(
                label = "Role",
                value = Role.PATIENT.label,
                onChange = {},
            )
            Text(
                "Staff accounts (doctor, nurse, admin, etc.) are created by the hospital administrator.",
                style = MaterialTheme.typography.bodySmall,
                color = Neutral500,
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            MahmsPrimaryButton(
                text = "Continue",
                enabled = formValid,
                loading = loading,
                onClick = {
                    submitted = true
                    if (!formValid) return@MahmsPrimaryButton
                    loading = true
                    error = null
                    scope.launch {
                        when (val outcome = container.authRepo.completeGoogleProfile(
                            uid = uid,
                            name = name.trim(),
                            email = email,
                            phone = phone.trim(),
                            role = Role.PATIENT.label,
                        )) {
                            is AuthOutcome.Success -> {
                                container.startLinkedSession(outcome.user)
                                loading = false
                                onComplete()
                            }
                            is AuthOutcome.Failure -> {
                                loading = false
                                error = outcome.message
                            }
                            is AuthOutcome.NeedsProfile -> {
                                loading = false
                            }
                        }
                    }
                },
            )
        }
    }
}
