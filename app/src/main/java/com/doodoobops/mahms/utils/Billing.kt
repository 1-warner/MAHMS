package com.doodoobops.mahms.utils

/**
 * Consultation fees and receipt numbers. Pure functions (no Android classes) so they are
 * covered by fast JVM unit tests.
 */
object Billing {

    /** Payment methods offered on the Pay screen. The gateway is simulated in this build. */
    val PAYMENT_METHODS: List<String> = listOf("EcoCash", "Card", "Cash")

    /** Consultation fee in USD, chosen from the doctor's specialisation. */
    fun consultationFee(specialization: String?): Double {
        val s = specialization.orEmpty()
        return when {
            s.isBlank() -> 25.0
            s.contains("General", ignoreCase = true) -> 20.0
            s.contains("Paediatric", ignoreCase = true) -> 20.0
            s.contains("Surgeon", ignoreCase = true) -> 40.0
            s.contains("Radiolog", ignoreCase = true) -> 40.0
            else -> 30.0
        }
    }

    /**
     * Receipt number shown to the patient, e.g. RCP-2026-00005. The year comes from the payment
     * date ("yyyy-MM-dd") and the number from the last five digits of the payment id.
     */
    fun receiptNumber(paymentId: Long, paymentDate: String): String {
        val year = paymentDate.take(4).let { y -> if (y.length == 4 && y.all { it.isDigit() }) y else "0000" }
        val serial = (paymentId % 100_000L).toString().padStart(5, '0')
        return "RCP-$year-$serial"
    }
}
