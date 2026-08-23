package com.doodoobops.mahms.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.doodoobops.mahms.R
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.theme.MpiloBlue600
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.ValidationUtils
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }

    val emailErr = ValidationUtils.emailError(email)
    val passEmpty = if (password.isBlank()) "Password is required" else null
    val canSubmit = emailErr == null && passEmpty == null

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
            Image(
                painter = painterResource(R.drawable.mpilo_logo),
                contentDescription = "Mpilo Central",
                modifier = Modifier.size(96.dp),
            )
            Text("Welcome back", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Sign in with your registered email.",
                style = MaterialTheme.typography.bodyMedium,
                color = Neutral500,
            )
            Spacer(Modifier.height(Spacing.x2))

            MahmsTextField("Email", email, { email = it }, error = if (submitted) emailErr else null, keyboardType = KeyboardType.Email)
            MahmsTextField("Password", password, { password = it }, error = if (submitted) passEmpty else null, isPassword = true)

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            MahmsPrimaryButton(
                text = "Sign in",
                enabled = canSubmit,
                loading = loading,
                onClick = {
                    submitted = true
                    if (!canSubmit) return@MahmsPrimaryButton
                    loading = true
                    error = null
                    scope.launch {
                        val user = container.users.login(email, password)
                        loading = false
                        if (user != null) {
                            container.session.startSession(
                                user.userId, user.role, user.fullName,
                                user.linkedPatientId, user.linkedDoctorId,
                            )
                            onLoggedIn()
                        } else {
                            error = "Incorrect email or password"
                        }
                    }
                },
            )

            TextButton(onClick = { showReset = true }) {
                Text("Forgot password?", color = MpiloBlue600)
            }
            Spacer(Modifier.height(Spacing.x4))
            Text(
                "Demo: patient@mpilo.co.zw · Password1 (also doctor@, nurse@, admin@, pharmacist@, responder@)",
                style = MaterialTheme.typography.bodySmall,
                color = Neutral500,
            )
        }
    }

    if (showReset) {
        ForgotPasswordDialog(
            onDismiss = { showReset = false },
            onDone = { msg ->
                showReset = false
                scope.launch { snackbar.showSnackbar(msg) }
            },
        )
    }
}
