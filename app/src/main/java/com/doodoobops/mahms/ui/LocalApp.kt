package com.doodoobops.mahms.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController
import com.doodoobops.mahms.di.AppContainer

/** Provides the manual DI container to the composable tree. */
val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

/** Lets shared screens (profile tabs) open Settings and Help without threading callbacks through every host. */
val LocalNavController = compositionLocalOf<NavHostController?> { null }
