package com.doodoobops.mahms.data

import com.doodoobops.mahms.data.cloud.toAppointment
import com.doodoobops.mahms.data.cloud.toCloudMap
import com.doodoobops.mahms.data.cloud.toDoctor
import com.doodoobops.mahms.data.cloud.toEmergency
import com.doodoobops.mahms.data.cloud.toPatient
import com.doodoobops.mahms.data.cloud.toPayment
import com.doodoobops.mahms.data.cloud.toPrescription
import com.doodoobops.mahms.data.cloud.toReminder
import com.doodoobops.mahms.data.cloud.toStaff
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.DoctorEntity
import com.doodoobops.mahms.data.entity.EmergencyEntity
import com.doodoobops.mahms.data.entity.MedicationReminderEntity
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import com.doodoobops.mahms.data.entity.PrescriptionEntity
import com.doodoobops.mahms.data.entity.StaffEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pulling from Cloud Firestore: every entity must survive upload (toCloudMap) and download (toX). */
class CloudPullMappersTest {

    @Test fun patient_roundTrips() {
        val p = PatientEntity(
            patientId = 7, firstName = "Thabo", lastName = "Moyo", dateOfBirth = "1987-03-12", gender = "Male",
            phone = "+263772345678", email = "thabo@mail.co.zw", address = "14 Fife St",
            emergencyContact = "+263772999111", registrationDate = 1_700_000_000_000L, notes = "BP ok",
        )
        assertEquals(p, p.toCloudMap().toPatient())
    }

    @Test fun doctor_roundTrips() {
        val d = DoctorEntity(doctorId = 2, firstName = "Tariro", lastName = "Sibanda", specialization = "General Practitioner", department = "Outpatient", phone = "+263772000002", email = "tsibanda@mpilo.co.zw")
        assertEquals(d, d.toCloudMap().toDoctor())
    }

    @Test fun appointment_roundTrips() {
        val a = AppointmentEntity(appointmentId = 3, patientId = 7, doctorId = 2, appointmentDate = "2026-09-04", appointmentTime = "09:00", status = "Confirmed", reason = "Check-up")
        assertEquals(a, a.toCloudMap().toAppointment())
    }

    @Test fun prescription_roundTrips() {
        val r = PrescriptionEntity(prescriptionId = 4, appointmentId = 3, patientId = 7, doctorId = 2, medicationName = "Amoxicillin", dosage = "500 mg", frequency = "Twice daily", duration = "7 days", issueDate = "2026-09-04", dispensed = true)
        assertEquals(r, r.toCloudMap().toPrescription())
    }

    @Test fun emergency_roundTrips_withAndWithoutResolvedTime() {
        val open = EmergencyEntity(emergencyId = 5, patientId = 7, timestamp = 1_700_000_000_000L, location = "GPS -20.150000, 28.580000", status = "Active", description = "SOS")
        assertEquals(open, open.toCloudMap().toEmergency())
        assertNull(open.toCloudMap().toEmergency()!!.resolvedAt)
        val done = open.copy(status = "Resolved", resolvedAt = 1_700_000_900_000L)
        assertEquals(done, done.toCloudMap().toEmergency())
    }

    @Test fun payment_roundTrips() {
        val p = PaymentEntity(paymentId = 1_766_111_222_333L, appointmentId = 3, amount = 25.0, paymentMethod = "EcoCash", paymentDate = "2026-09-04", status = "Completed")
        assertEquals(p, p.toCloudMap().toPayment())
    }

    @Test fun reminderAndStaff_roundTrip() {
        val r = MedicationReminderEntity(reminderId = 6, prescriptionId = 4, patientId = 7, medicationName = "Atenolol", dosage = "50 mg", reminderTime = "08:00", isActive = true)
        assertEquals(r, r.toCloudMap().toReminder())
        val s = StaffEntity(staffId = 8, firstName = "Grace", lastName = "Mutasa", role = "Nurse", department = "Outpatient", phone = "+263772100004", email = "gmutasa@mpilo.co.zw")
        assertEquals(s, s.toCloudMap().toStaff())
    }

    @Test fun firestoreNumbers_areAcceptedAsIntLongOrDouble() {
        val doc = mapOf<String, Any?>(
            "paymentId" to 9, "appointmentId" to 3L, "amount" to 25, "paymentMethod" to "Cash",
            "paymentDate" to "2026-09-04", "status" to "Completed",
        )
        val p = doc.toPayment()!!
        assertEquals(9L, p.paymentId)
        assertEquals(25.0, p.amount, 0.001)
    }

    @Test fun documentWithoutAnId_orMissingParent_isSkipped() {
        assertNull(mapOf<String, Any?>("firstName" to "No id").toPatient())
        assertNull(mapOf<String, Any?>("appointmentId" to 1L, "doctorId" to 2L).toAppointment())
    }
}
