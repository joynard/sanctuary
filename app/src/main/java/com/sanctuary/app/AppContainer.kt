package com.sanctuary.app

import android.content.Context
import com.sanctuary.core.data.AppearancePreferences
import com.sanctuary.core.data.BackupRepository
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.SanctuaryData
import com.sanctuary.core.data.ScrollRepository
import com.sanctuary.core.data.TrackingPreferences
import com.sanctuary.core.sync.AndroidSyncMetadataStore
import com.sanctuary.core.sync.CloudSyncManager
import com.sanctuary.core.sync.EncryptedSessionStore
import com.sanctuary.core.sync.SupabaseConfig
import com.sanctuary.core.tracking.AccessibilityStatusChecker
import com.sanctuary.core.tracking.TrackingDependencies
import java.time.Clock

class AppContainer(
    context: Context,
    appVersion: String,
) : TrackingDependencies {
    private val appContext = context.applicationContext
    private val data = SanctuaryData(
        context = appContext,
        appVersion = appVersion,
        clock = Clock.systemDefaultZone(),
    )

    val clock: Clock = data.clock

    val profileRepository: ProfileRepository = data.profileRepository

    override val scrollRepository: ScrollRepository = data.scrollRepository

    override val trackingPreferences: TrackingPreferences = data.trackingPreferences

    val appearancePreferences: AppearancePreferences = data.appearancePreferences

    val backupRepository: BackupRepository = data.backupRepository

    val accessibilityStatusChecker: AccessibilityStatusChecker by lazy {
        AccessibilityStatusChecker(appContext)
    }

    val cloudSyncManager: CloudSyncManager by lazy {
        CloudSyncManager(
            config = SupabaseConfig(
                projectUrl = BuildConfig.SUPABASE_URL,
                publishableKey = BuildConfig.SUPABASE_ANON_KEY,
            ),
            sessionStore = EncryptedSessionStore(appContext),
            profileRepository = profileRepository,
            scrollRepository = scrollRepository,
            metadata = AndroidSyncMetadataStore(appContext),
            clock = clock,
        )
    }
}
