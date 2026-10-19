package com.doodoobops.mahms.ui.admin

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.components.DataText
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.Emergency500
import com.doodoobops.mahms.ui.theme.MpiloBlue500
import com.doodoobops.mahms.ui.theme.Neutral500
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing
import com.doodoobops.mahms.ui.theme.Success500
import com.doodoobops.mahms.utils.DateUtils

@Composable
fun AdminDashboardScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val today = DateUtils.todayStore()
    val totalPatients by container.patients.count().collectAsStateWithLifecycle(0)
    val todaysAppts by container.appointments.countForDate(today).collectAsStateWithLifecycle(0)
    val revenue by container.payments.revenueForDate(today).collectAsStateWithLifecycle(0.0)
    val activeEmergencies by container.emergencies.countActive().collectAsStateWithLifecycle(0)
    val perDay by container.appointments.countPerDay().collectAsStateWithLifecycle(emptyList())

    val last7 = remember { DateUtils.lastNDates(7) }
    val countMap = perDay.associate { it.date to it.total }
    val barValues = last7.map { (countMap[it] ?: 0).toFloat() }
    val barLabels = last7.map { it.takeLast(2) }
    // 30-day mock patient-volume trend (deterministic)
    val lineValues = remember { (0 until 30).map { 12f + 6f * kotlin.math.sin(it / 3.0).toFloat() + (it % 5) } }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MahmsTopBar("Admin dashboard")
        Column(Modifier.padding(Spacing.x4), verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                SummaryCard("Total patients", totalPatients.toString(), MpiloBlue500, Modifier.weight(1f))
                SummaryCard("Today's appts", todaysAppts.toString(), Success500, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                SummaryCard("Revenue today", DateUtils.currency(revenue), MpiloBlue500, Modifier.weight(1f), mono = true)
                SummaryCard("Active emergencies", activeEmergencies.toString(), Emergency500, Modifier.weight(1f))
            }

            SectionHeader("Appointments · last 7 days", Modifier.padding(0.dp))
            MahmsCard { BarChart(barValues, barLabels) }

            SectionHeader("Patient volume · last 30 days", Modifier.padding(0.dp))
            MahmsCard { LineChart(lineValues, Modifier) }
            Spacer(Modifier.height(Spacing.x6))
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier, mono: Boolean = false) {
    MahmsCard(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Neutral500)
        Spacer(Modifier.height(Spacing.x1))
        if (mono) DataText(value, color = accent, fontSize = 18.sp)
        else Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
    }
}
