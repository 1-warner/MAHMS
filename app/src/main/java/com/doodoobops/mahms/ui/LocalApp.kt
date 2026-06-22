package com.doodoobops.mahms.ui

import androidx.compose.runtime.compositionLocalOf
import com.doodoobops.mahms.di.AppContainer

/** Provides the manual DI container to the composable tree. */
val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
