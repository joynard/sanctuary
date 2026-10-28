package com.sanctuary.app

import android.app.Application
import com.sanctuary.core.tracking.TrackingDependencies
import com.sanctuary.core.tracking.TrackingDependenciesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SanctuaryApp : Application(), TrackingDependenciesProvider {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    override val trackingDependencies: TrackingDependencies
        get() = container

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            context = this,
            appVersion = BuildConfig.VERSION_NAME,
        )
        // The accessibility service can receive an event before the first screen
        // is opened, so the profile row must exist as early as possible.
        appScope.launch {
            runCatching { container.profileRepository.ensureDefault() }
            runCatching { container.cloudSyncManager.syncIfLinked() }
        }
    }
}
