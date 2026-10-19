package com.doodoobops.mahms.ui.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.doodoobops.mahms.ui.components.MahmsCard
import com.doodoobops.mahms.ui.components.MahmsTopBar
import com.doodoobops.mahms.ui.components.SectionHeader
import com.doodoobops.mahms.ui.theme.MpiloBlue600
import com.doodoobops.mahms.ui.theme.Neutral25
import com.doodoobops.mahms.ui.theme.Neutral700
import com.doodoobops.mahms.ui.theme.Neutral900
import com.doodoobops.mahms.ui.theme.Spacing

private data class HelpTopic(val title: String, val body: String)
private data class HelpSection(val title: String, val topics: List<HelpTopic>)

/** In-app help system. The same text is published as USER_GUIDE.md. */
private val helpSections = listOf(
    HelpSection("Getting started", listOf(
        HelpTopic("Creating an account",
            "Open the app and tap Register. Enter your full name, email, phone number (+263XXXXXXXXX or 07XXXXXXXX), and a password of at least 8 characters with one capital letter and one number. Tap Create account, then sign in. Registration creates a Patient account. Staff accounts (doctor, nurse, pharmacist, responder, administrator) are issued by the hospital administrator. Invalid or empty fields show a message under the field."),
        HelpTopic("Signing in",
            "Enter your email and password and tap Sign in, or tap Continue with Google to use your Google account. Your password is stored securely by the online authentication service, never in plain text."),
        HelpTopic("Demo accounts",
            "For testing, every role has a demo account with the password Password1: patient@mpilo.co.zw, doctor@mpilo.co.zw, nurse@mpilo.co.zw, admin@mpilo.co.zw, pharmacist@mpilo.co.zw and responder@mpilo.co.zw."),
        HelpTopic("Working offline",
            "If you lose your internet connection the app keeps working with the data on your phone. Changes are copied to the online database when you are connected again."),
    )),
    HelpSection("Patients", listOf(
        HelpTopic("Booking an appointment",
            "On Home or Appointments, tap Book appointment. Choose a department, a doctor, a date and a time slot, then confirm. The app will not let you continue until every choice is made."),
        HelpTopic("Viewing and cancelling appointments",
            "Open the Appointments tab to see upcoming and past visits with their status (Pending, Confirmed, Completed, Cancelled). Use Cancel appointment on an upcoming booking to cancel it."),
        HelpTopic("Prescriptions and reminders",
            "The Prescriptions tab lists your medicines. Switch on Reminder beside a medicine to get a daily notification (the default time is 08:00). You can turn all reminders off in Settings."),
        HelpTopic("Emergency SOS",
            "Open the SOS tab and tap the red button to alert the hospital casualty desk. Allow the location permission when asked: your GPS position and street address are sent with the alert, and the responder can open it in a maps app. If location is off the alert is still sent. The responder updates the status until you are helped. Only use this for real emergencies."),
        HelpTopic("Paying and receipts",
            "Open Appointments and tap the Payments tab. Completed or confirmed visits that are unpaid appear under Outstanding with a Pay now button. Choose EcoCash, Card or Cash and confirm. The payment gateway is simulated in this build. Paid visits appear under Receipts, where you can open a receipt and tap Save as PDF."),
        HelpTopic("Editing your profile",
            "Open the Profile tab and tap Edit profile to change your phone number, address and emergency contact."),
    )),
    HelpSection("Doctors", listOf(
        HelpTopic("Today's schedule",
            "The Schedule tab shows your appointments for the day. Tap Mark attended once you have seen the patient."),
        HelpTopic("Patient records",
            "The Records tab lets you search by name or phone and read the patient's details and notes."),
        HelpTopic("Writing a prescription",
            "Open the Prescribe tab, choose the patient, enter the medication name, dosage, frequency and duration, and tap Issue prescription. The pharmacist sees it in Pending dispensing."),
    )),
    HelpSection("Nurses", listOf(
        HelpTopic("Patient queue",
            "The Queue tab lists today's patients. Tap Mark arrived when a patient reaches you."),
        HelpTopic("Recording vitals",
            "Open the Vitals tab, choose the Patient, enter temperature, heart rate, systolic and diastolic blood pressure, weight and height, then tap Save vitals. The vitals are added to the patient's notes."),
    )),
    HelpSection("Pharmacists", listOf(
        HelpTopic("Dispensing medicine",
            "The Pending tab shows Pending dispensing. Tap Dispense once you have handed the medicine over; it then appears in the History tab under Dispensing history."),
    )),
    HelpSection("Emergency responders", listOf(
        HelpTopic("Responding to alerts",
            "The Active tab lists emergencies with the patient's name, description and time. Tap Respond when you are on your way and Resolve when it is finished. Closed alerts move to the History tab."),
    )),
    HelpSection("Hospital administrators", listOf(
        HelpTopic("Dashboard and reports",
            "The Dashboard tab shows today's appointments, active emergencies and revenue. In the Reports tab choose a date range, tap Generate report, then Export as PDF."),
        HelpTopic("Managing staff",
            "The Staff tab lists hospital staff. Tap Add staff to register a new member."),
    )),
    HelpSection("Settings and account", listOf(
        HelpTopic("Changing your password",
            "Open Profile, then Settings. Enter your current password and the new one twice, then tap Update password. Accounts that sign in with Google do not have a password to change."),
        HelpTopic("Notifications",
            "In Settings, use the Medication reminders switch to turn reminder notifications on or off."),
        HelpTopic("Sync now",
            "Tap Sync now in Settings to download the latest records from the online database and upload your changes. It needs an internet connection."),
        HelpTopic("Logging out",
            "Tap Log out in Settings or on the Profile tab. You will return to the sign-in screen."),
    )),
    HelpSection("Troubleshooting", listOf(
        HelpTopic("I cannot sign in",
            "Check the email and password. If you have no internet connection you can only sign in to an account that has already signed in on this phone. Too many wrong attempts will block sign-in for a short while. The Forgot password? link tells you to contact your administrator."),
        HelpTopic("My reminder did not appear",
            "Check that Medication reminders is on in Settings and that notifications are allowed for MAHMS in the phone's system settings."),
        HelpTopic("Google sign-in fails",
            "Make sure a Google account is added to the phone and that you are online. If it still fails, sign in with email and password instead."),
        HelpTopic("Contact support",
            "Contact your hospital administrator, or visit the front desk at Mpilo Central Hospital."),
    )),
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = Neutral25,
        topBar = { MahmsTopBar("Help and user guide", showBack = true, onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.x4),
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            helpSections.forEach { section ->
                SectionHeader(section.title)
                section.topics.forEach { HelpItem(it) }
            }
        }
    }
}

@Composable
private fun HelpItem(topic: HelpTopic) {
    var open by remember { mutableStateOf(false) }
    MahmsCard(Modifier.clickable { open = !open }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                topic.title,
                style = MaterialTheme.typography.titleMedium,
                color = Neutral900,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (open) "Collapse" else "Expand",
                tint = MpiloBlue600,
            )
        }
        if (open) {
            Text(
                topic.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Neutral700,
                modifier = Modifier.padding(top = Spacing.x2),
            )
        }
    }
}
