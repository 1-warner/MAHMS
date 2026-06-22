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
 * For this local-only build, clinical screens bind to seeded records:
 * a patient session reads patient #1, a doctor session reads doctor #1.
 * A real build would link UserEntity to a PatientEntity/DoctorEntity.
 */
object Demo {
    const val PATIENT_ID = 1L
    const val DOCTOR_ID = 1L
}
