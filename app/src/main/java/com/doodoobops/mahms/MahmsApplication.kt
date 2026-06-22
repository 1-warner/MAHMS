package com.doodoobops.mahms

import android.app.Application
import com.doodoobops.mahms.data.seed.DatabaseSeeder
import com.doodoobops.mahms.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MahmsApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannel()
        appScope.launch {
            DatabaseSeeder.seedIfEmpty(container.database)
        }
    }
}
