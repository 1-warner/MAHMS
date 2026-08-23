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

