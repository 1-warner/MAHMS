package com.doodoobops.mahms.data

import com.doodoobops.mahms.data.cloud.toCloudMap
import com.doodoobops.mahms.data.entity.AppointmentEntity
import com.doodoobops.mahms.data.entity.PatientEntity
import com.doodoobops.mahms.data.entity.PaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CloudMappersTest {

    private val patient = PatientEntity(
        patientId = 7, firstName = "Thabo", lastName = "Moyo", dateOfBirth = "1987-03-12",
        gender = "Male", phone = "+263772345678", email = "thabo@mail.co.zw",
        address = "14 Fife St", emergencyContact = "+263772999111",
    )

    @Test fun patient_mapsKeyFields() {
        val m = patient.toCloudMap()
        assertEquals(7L, m["patientId"])
        assertEquals("Thabo", m["firstName"])
        assertEquals("thabo@mail.co.zw", m["email"])
    }

    @Test fun appointment_mapsForeignKeysAndStatus() {
        val m = AppointmentEntity(
            appointmentId = 3, patientId = 7, doctorId = 2, appointmentDate = "2026-09-04",
            appointmentTime = "09:00", status = "Confirmed", reason = "Check-up",
        ).toCloudMap()
        assertEquals(7L, m["patientId"])
        assertEquals(2L, m["doctorId"])
        assertEquals("Confirmed", m["status"])
    }

    @Test fun payment_mapsAmount() {
        val m = PaymentEntity(
            paymentId = 1, appointmentId = 3, amount = 25.0, paymentMethod = "EcoCash",
            paymentDate = "2026-09-04", status = "Completed",
        ).toCloudMap()
        assertEquals(25.0, m["amount"])
    }

    @Test fun cloudMaps_neverContainPasswordFields() {
        val keys = patient.toCloudMap().keys.map { it.lowercase() }
        assertFalse(keys.any { "password" in it || "hash" in it })
    }
}
