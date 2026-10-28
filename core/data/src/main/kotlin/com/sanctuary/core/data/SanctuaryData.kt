package com.sanctuary.core.data

import android.content.Context
import com.sanctuary.core.database.SanctuaryDatabase
import java.time.Clock

/**
 * Composition root for the data layer. The app module deliberately does not know
 * about Room or its DAOs; swapping persistence later only touches this class.
 */
class SanctuaryData(
    context: Context,
    appVersion: String,
    val clock: Clock,
) {
    private val appContext = context.applicationContext
    private val database: SanctuaryDatabase by lazy { SanctuaryDatabase.get(appContext) }

    val profileRepository: ProfileRepository by lazy {
        OfflineProfileRepository(
            profileDao = database.profileDao(),
            clock = clock,
        )
    }

    val scrollRepository: ScrollRepository by lazy {
        OfflineScrollRepository(
            database = database,
            scrollSessionDao = database.scrollSessionDao(),
            profileDao = database.profileDao(),
        )
    }

    val trackingPreferences: TrackingPreferences by lazy {
        TrackingPreferences(appContext)
    }

    val backupRepository: BackupRepository by lazy {
        JsonBackupRepository(
            contentResolver = appContext.contentResolver,
            profileRepository = profileRepository,
            scrollRepository = scrollRepository,
            appVersion = appVersion,
            clock = clock,
        )
    }
}
