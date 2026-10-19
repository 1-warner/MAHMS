package com.doodoobops.mahms.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.doodoobops.mahms.domain.model.Role
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.LocalNavController
import com.doodoobops.mahms.utils.AppLog
import com.doodoobops.mahms.ui.help.HelpScreen
import com.doodoobops.mahms.ui.settings.SettingsScreen
import com.doodoobops.mahms.ui.admin.AdminHost
import com.doodoobops.mahms.ui.auth.LoginScreen
import com.doodoobops.mahms.ui.auth.PostSplashScreen
import com.doodoobops.mahms.ui.auth.RegisterScreen
import com.doodoobops.mahms.ui.doctor.DoctorHost
import com.doodoobops.mahms.ui.nurse.NurseHost
import com.doodoobops.mahms.ui.patient.BookAppointmentScreen
import com.doodoobops.mahms.ui.patient.PatientHost
import com.doodoobops.mahms.ui.pharmacist.PharmacistHost
import com.doodoobops.mahms.ui.responder.ResponderHost

@Composable
fun MahmsNavGraph(navController: NavHostController) {
    val container = LocalAppContainer.current
    val session = container.session

    fun goHome() {
        val dest = Screen.homeFor(Role.fromLabel(session.currentRole))
        navController.navigate(dest) {
            popUpTo(Screen.Splash.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun logout() {
        AppLog.i("Session", "User logged out")
        container.authRepo.signOut()
        session.endSession()
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    CompositionLocalProvider(LocalNavController provides navController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            PostSplashScreen(
                onContinue = {
                    when {
                        session.isLoggedIn -> goHome()
                        session.isRegistered -> navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        else -> navController.navigate(Screen.Register.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onHaveAccount = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(onLoggedIn = { goHome() })
        }

        composable(Screen.PatientHome.route) {
            PatientHost(
                onLogout = ::logout,
                onBookAppointment = { navController.navigate(Screen.BookAppointment.route) },
            )
        }
        composable(Screen.DoctorHome.route) { DoctorHost(onLogout = ::logout) }
        composable(Screen.NurseHome.route) { NurseHost(onLogout = ::logout) }
        composable(Screen.AdminHome.route) { AdminHost(onLogout = ::logout) }
        composable(Screen.PharmacistHome.route) { PharmacistHost(onLogout = ::logout) }
        composable(Screen.ResponderHome.route) { ResponderHost(onLogout = ::logout) }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenHelp = { navController.navigate(Screen.Help.route) },
                onLogout = ::logout,
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.BookAppointment.route) {
            BookAppointmentScreen(onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }
    }
    }
}
