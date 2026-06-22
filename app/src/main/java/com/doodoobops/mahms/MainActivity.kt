package com.doodoobops.mahms

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.doodoobops.mahms.navigation.MahmsNavGraph
import com.doodoobops.mahms.ui.LocalAppContainer
import com.doodoobops.mahms.ui.theme.MahmsTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val container = (application as MahmsApplication).container

        setContent {
            MahmsTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    val navController = rememberNavController()
                    MahmsNavGraph(navController)
                }
            }
        }
    }
}
