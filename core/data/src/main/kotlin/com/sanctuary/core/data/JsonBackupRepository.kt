package com.sanctuary.core.data

import android.content.ContentResolver
import android.net.Uri
import com.sanctuary.core.model.BackupDocument
import com.sanctuary.core.model.BackupProfile
import com.sanctuary.core.model.BackupSession
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
class JsonBackupRepository(
    private val contentResolver: ContentResolver,
    private val profileRepository: ProfileRepository,
    private val scrollRepository: ScrollRepository,
    private val appVersion: String,
    private val clock: Clock,
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        explicitNulls = true
    }

    override fun suggestedFileName(date: LocalDate): String =
        "sanctuary-backup-$date.json"

    override suspend fun export(uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val profile = profileRepository.ensureDefault()
            val sessions = scrollRepository.allSessions()
            val document = BackupDocument(
                exportedAt = clock.instant().toString(),
                appVersion = appVersion,
                profile = BackupProfile(
                    localId = profile.id,
                    displayName = profile.displayName,
                    dailyGoal = profile.dailyGoal,
                    intention = profile.intention,
                    timezoneId = profile.timezoneId,
                    onboardingComplete = profile.onboardingComplete,
                    createdAt = profile.createdAt.toString(),
                    updatedAt = profile.updatedAt.toString(),
                ),
                sessions = sessions.map { session ->
                    BackupSession(
                        id = session.id,
                        platform = session.platform.key,
                        localDay = session.localDay.toString(),
                        startedAt = session.startedAt.toString(),
                        endedAt = session.endedAt.toString(),
                        scrollCount = session.scrollCount,
                        source = session.source.key,
                    )
                },
            )

            val output = contentResolver.openOutputStream(uri, "wt")
                ?: error("Tidak bisa membuka tujuan file")
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(json.encodeToString(document))
            }
        }
    }
}
