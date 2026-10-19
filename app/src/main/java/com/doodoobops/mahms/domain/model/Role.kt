package com.doodoobops.mahms.domain.model

/** The six MAHMS roles. The label is stored verbatim on UserEntity.role. */
enum class Role(val label: String) {
    PATIENT("Patient"),
    DOCTOR("Doctor"),
    NURSE("Nurse"),
    ADMIN("Hospital Administrator"),
    PHARMACIST("Pharmacist"),
    RESPONDER("Emergency Responder");

    companion object {
        val all: List<Role> = entries
        fun fromLabel(label: String?): Role? = entries.firstOrNull { it.label == label }
    }
}

/**
 * Links a signed-in account to its own clinical record. Accounts are matched to a patient or
 * doctor by e-mail; new patients get a record created at first sign-in. The two seeded demo
 * logins use different e-mail addresses from their records, so they are mapped here.
 */
object Demo {
    val PATIENT_BY_EMAIL: Map<String, Long> = mapOf("patient@mpilo.co.zw" to 1L)
    val DOCTOR_BY_EMAIL: Map<String, Long> = mapOf("doctor@mpilo.co.zw" to 1L)
}
