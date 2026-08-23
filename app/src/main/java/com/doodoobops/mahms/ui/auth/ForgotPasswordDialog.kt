package com.doodoobops.mahms.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.doodoobops.mahms.data.entity.UserEntity
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * Self-service password reset for the local build. Identity is proven with the
 * email plus the phone number registered on the account (no backend, so there is
 * no e-mail/SMS delivery); an administrator can also reset a password in-app.
 */
@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var verified by remember { mutableStateOf<UserEntity?>(null) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val user = verified
    if (user == null) {
        val canVerify = ValidationUtils.emailError(email) == null && phone.isNotBlank()
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Reset your password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    Text(
                        "Confirm the email and phone number registered on your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neutral500,
                    )
                    MahmsTextField("Email", email, { email = it; error = null }, keyboardType = KeyboardType.Email)
                    MahmsTextField(
                        "Registered phone", phone, { phone = it; error = null },
                        keyboardType = KeyboardType.Phone, placeholder = "+263771234567",
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Text(
                        "Can't remember these details? Contact your administrator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral500,
                    )
                }
            },
            confirmButton = {
                MahmsPrimaryButton("Continue", enabled = canVerify, loading = busy, onClick = {
                    busy = true
                    scope.launch {
                        val found = container.users.verifyIdentity(email, ValidationUtils.normalizePhone(phone))
                        busy = false
                        if (found == null) error = "Those details don't match an account." else verified = found
                    }
                })
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    } else {
        val passErr = ValidationUtils.passwordError(newPassword)
        val confirmErr = ValidationUtils.confirmError(newPassword, confirm)
        val canSave = passErr == null && confirmErr == null
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Choose a new password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    Text(
                        "Signing in as ${user.email}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neutral500,
                    )
                    MahmsTextField("New password", newPassword, { newPassword = it }, error = passErr.takeIf { newPassword.isNotEmpty() }, isPassword = true)
                    MahmsTextField("Confirm password", confirm, { confirm = it }, error = confirmErr.takeIf { confirm.isNotEmpty() }, isPassword = true)
                }
            },
            confirmButton = {
                MahmsPrimaryButton("Save password", enabled = canSave, loading = busy, onClick = {
                    busy = true
                    scope.launch {
                        container.users.setPassword(user.userId, newPassword)
                        busy = false
                        onDone("Password updated. You can sign in now.")
                    }
                })
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }
}
