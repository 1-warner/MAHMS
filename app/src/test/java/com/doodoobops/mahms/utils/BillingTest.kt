package com.doodoobops.mahms.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingTest {

    @Test fun fee_dependsOnSpecialisation() {
        assertEquals(20.0, Billing.consultationFee("General Practitioner"), 0.001)
        assertEquals(20.0, Billing.consultationFee("Paediatrician"), 0.001)
        assertEquals(40.0, Billing.consultationFee("Orthopaedic Surgeon"), 0.001)
        assertEquals(40.0, Billing.consultationFee("Radiologist"), 0.001)
        assertEquals(30.0, Billing.consultationFee("Cardiologist"), 0.001)
    }

    @Test fun fee_unknownSpecialisation_usesDefault() {
        assertEquals(25.0, Billing.consultationFee(null), 0.001)
        assertEquals(25.0, Billing.consultationFee("  "), 0.001)
    }

    @Test fun receiptNumber_isPaddedAndUsesPaymentYear() {
        assertEquals("RCP-2026-00005", Billing.receiptNumber(5, "2026-09-04"))
    }

    @Test fun receiptNumber_longTimeBasedId_usesLastFiveDigits() {
        assertEquals("RCP-2026-22333", Billing.receiptNumber(1_766_111_222_333L, "2026-10-19"))
    }

    @Test fun receiptNumber_badDate_fallsBackToZeroYear() {
        assertEquals("RCP-0000-00012", Billing.receiptNumber(12, "garbage"))
    }

    @Test fun paymentMethods_includeEcoCash() {
        assertTrue("EcoCash" in Billing.PAYMENT_METHODS)
    }

    @Test
    fun `display receipt number prefers the number the API issued`() {
        assertEquals(
            "RCP-2026-00042",
            Billing.displayReceiptNumber("RCP-2026-00042", 7L, "2026-03-01"),
        )
    }

    @Test
    fun `display receipt number falls back to the local one when the API gave none`() {
        assertEquals(
            Billing.receiptNumber(7L, "2026-03-01"),
            Billing.displayReceiptNumber(null, 7L, "2026-03-01"),
        )
    }

    @Test
    fun `display receipt number treats a blank server number as missing`() {
        assertEquals(
            Billing.receiptNumber(7L, "2026-03-01"),
            Billing.displayReceiptNumber("   ", 7L, "2026-03-01"),
        )
    }
}
