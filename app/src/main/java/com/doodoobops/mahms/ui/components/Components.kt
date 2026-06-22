package com.doodoobops.mahms.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodoobops.mahms.ui.theme.DataTextStyle
import com.doodoobops.mahms.ui.theme.Emergency500
import com.doodoobops.mahms.ui.theme.Emergency700
import com.doodoobops.mahms.ui.theme.MpiloBlue500
import com.doodoobops.mahms.ui.theme.MpiloBlue600
import com.doodoobops.mahms.ui.theme.Neutral0
import com.doodoobops.mahms.ui.theme.Neutral100
import com.doodoobops.mahms.ui.theme.Neutral400
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.ui.theme.statusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahmsTopBar(
    title: String,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = Neutral900) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MpiloBlue600)
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Neutral0,
            titleContentColor = Neutral900,
        ),
    )
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun MahmsBottomNav(
    items: List<BottomNavItem>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    NavigationBar(containerColor = Neutral0, tonalElevation = 0.dp) {
        items.forEach { item ->
            NavigationBarItem(
                selected = selected == item.route,
                onClick = { onSelect(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MpiloBlue500,
                    selectedTextColor = MpiloBlue600,
                    indicatorColor = com.doodoobops.mahms.ui.theme.MpiloBlue50,
                    unselectedIconColor = Neutral400,
                    unselectedTextColor = Neutral500,
                ),
            )
        }
    }
}

@Composable
fun MahmsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Radii.md,
        color = Neutral0,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Neutral100),
    ) {
        Column(Modifier.padding(Spacing.x4)) { content() }
    }
}

@Composable
fun MahmsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        enabled = enabled && !loading,
        shape = Radii.md,
        colors = ButtonDefaults.buttonColors(
            containerColor = MpiloBlue500,
            contentColor = Neutral0,
            disabledContainerColor = MpiloBlue500.copy(alpha = 0.5f),
            disabledContentColor = Neutral0.copy(alpha = 0.9f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Neutral0,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun MahmsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        enabled = enabled,
        shape = Radii.md,
        border = BorderStroke(1.dp, MpiloBlue500),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MpiloBlue600),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun MahmsDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        enabled = enabled,
        shape = Radii.md,
        colors = ButtonDefaults.buttonColors(containerColor = Emergency500, contentColor = Neutral0),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahmsTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it, color = Neutral400) } },
            isError = error != null,
            singleLine = singleLine,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = trailingIcon,
            shape = Radii.md,
        )
        if (error != null) {
            Text(
                error,
                color = Emergency500,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = Spacing.x3, top = Spacing.x1),
            )
        }
    }
}

@Composable
fun MahmsStatusChip(status: String, modifier: Modifier = Modifier) {
    val (bg, fg) = statusColors(status)
    Surface(shape = Radii.pill, color = bg, modifier = modifier) {
        Text(
            status,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = Spacing.x3, vertical = Spacing.x1),
        )
    }
}

@Composable
fun MahmsEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).padding(0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(shape = CircleShape, color = Neutral100, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Neutral400, modifier = Modifier.size(28.dp))
                }
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = Neutral900)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Neutral500,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun MahmsLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(Spacing.x10), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MpiloBlue500)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Neutral500,
        modifier = modifier.padding(horizontal = Spacing.x4, vertical = Spacing.x2),
    )
}

/** Mono clinical-data text (vitals, dosages, IDs, timestamps). */
@Composable
fun DataText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Neutral900,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
) {
    Text(text, modifier = modifier, style = DataTextStyle.copy(color = color, fontSize = fontSize))
}

/** Initials avatar in a blue circle. */
@Composable
fun InitialsAvatar(initials: String, size: Int = 56, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = com.doodoobops.mahms.ui.theme.MpiloBlue100, modifier = modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                initials.take(2).uppercase(),
                color = MpiloBlue600,
                fontWeight = FontWeight.Bold,
                fontSize = (size / 3).sp,
            )
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String, mono: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Neutral500)
        Spacer(Modifier.height(2.dp))
        if (mono) DataText(value) else Text(value, style = MaterialTheme.typography.bodyLarge, color = Neutral900)
    }
}

@Composable
fun FullScreenLoading() {
    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MpiloBlue500)
    }
}

@Composable
fun RowDivider() {
    Surface(color = Neutral100, modifier = Modifier.fillMaxWidth().height(1.dp)) {}
}
