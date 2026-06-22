package com.doodoobops.mahms.navigation

import com.doodoobops.mahms.domain.model.Role

/** Top-level destinations driven by the outer NavController. */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Register : Screen("register")
    data object Login : Screen("login")

    data object PatientHome : Screen("patient_home")
    data object DoctorHome : Screen("doctor_home")
    data object NurseHome : Screen("nurse_home")
    data object AdminHome : Screen("admin_home")
    data object PharmacistHome : Screen("pharmacist_home")
    data object ResponderHome : Screen("responder_home")

    data object BookAppointment : Screen("book_appointment")

    companion object {
        fun homeFor(role: Role?): String = when (role) {
            Role.PATIENT -> PatientHome.route
            Role.DOCTOR -> DoctorHome.route
            Role.NURSE -> NurseHome.route
            Role.ADMIN -> AdminHome.route
            Role.PHARMACIST -> PharmacistHome.route
            Role.RESPONDER -> ResponderHome.route
            null -> Login.route
        }
    }
}
