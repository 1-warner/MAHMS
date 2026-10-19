package com.doodoobops.mahms.ui.nurse

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.ui.common.patientName
import com.doodoobops.mahms.ui.common.rememberPatientMap
import com.doodoobops.mahms.ui.components.MahmsPrimaryButton
import com.doodoobops.mahms.ui.components.MahmsTextField
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Radii
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseVitalsScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val patients by rememberPatientMap()

    var patientId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var hr by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    val canSave = patientId != null && systolic.isNotBlank() && diastolic.isNotBlank()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Neutral25) { pad ->
        Column(modifier.fillMaxSize().padding(pad)) {
            MahmsTopBar("Record vitals")
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.x4),
                verticalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = patientId?.let { patientName(patients[it]) } ?: "Select patient",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Patient") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        shape = Radii.md,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        patients.values.forEach { p ->
                            DropdownMenuItem(text = { Text(patientName(p)) }, onClick = { patientId = p.patientId; expanded = false })
                        }
                    }
                }

                SectionHeader("Blood pressure (mmHg)", Modifier.padding(0.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    MahmsTextField("Systolic", systolic, { systolic = it.filter(Char::isDigit) }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    MahmsTextField("Diastolic", diastolic, { diastolic = it.filter(Char::isDigit) }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
                MahmsTextField("Heart rate (bpm)", hr, { hr = it.filter(Char::isDigit) }, keyboardType = KeyboardType.Number)
                MahmsTextField("Temperature (°C)", temp, { temp = it }, keyboardType = KeyboardType.Decimal)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    MahmsTextField("Weight (kg)", weight, { weight = it }, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                    MahmsTextField("Height (cm)", height, { height = it }, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                }

                MahmsPrimaryButton("Save vitals", enabled = canSave, onClick = {
                    scope.launch {
                        val p = container.patients.get(patientId!!) ?: return@launch
                        val line = "[${DateUtils.prettyStamp(System.currentTimeMillis())}] " +
                            "BP $systolic/$diastolic mmHg, HR ${hr.ifBlank { "—" }} bpm, " +
                            "Temp ${temp.ifBlank { "—" }}°C, Wt ${weight.ifBlank { "—" }}kg, Ht ${height.ifBlank { "—" }}cm"
                        val merged = if (p.notes.isBlank()) line else "$line\n${p.notes}"
                        AppLog.i("Nurse", "Vitals saved for patient ${p.patientId}")
                        container.patients.update(p.copy(notes = merged))
                        systolic = ""; diastolic = ""; hr = ""; temp = ""; weight = ""; height = ""
                        snackbar.showSnackbar("Vitals saved")
                    }
                })
                Spacer(Modifier.height(Spacing.x6))
            }
        }
    }
}
