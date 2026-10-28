package com.sanctuary.core.data

import androidx.room.withTransaction
import com.sanctuary.core.database.ProfileDao
import com.sanctuary.core.database.SanctuaryDatabase
import com.sanctuary.core.database.ScrollSessionDao
import com.sanctuary.core.database.ScrollSessionEntity
import com.sanctuary.core.model.ScrollPlatform
import com.sanctuary.core.model.ScrollSession
import com.sanctuary.core.model.SyncState
import com.sanctuary.core.model.TrackingPrivacy
import com.sanctuary.core.model.TrackingSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineScrollRepository(
    private val database: SanctuaryDatabase,
    private val scrollSessionDao: ScrollSessionDao,
    private val profileDao: ProfileDao,
) : ScrollRepository {

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<ScrollSession>> =
        scrollSessionDao
            .observeRange(start.toString(), end.toString())
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun recordScroll(
        platform: ScrollPlatform,
        at: Instant,
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val profile = profileDao.getFirst()
            val zone = profile?.timezoneId
                ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: ZoneId.systemDefault()
            val eventAtMillis = at.toEpochMilli()
            val localDay = at.atZone(zone).toLocalDate().toString()
            val gapMillis = TimeUnit.MINUTES.toMillis(TrackingPrivacy.SESSION_GAP_MINUTES.toLong())
            val existing = scrollSessionDao.findRecent(
                platform = platform.key,
                localDay = localDay,
                sinceMillis = eventAtMillis - gapMillis,
            )

            if (existing == null) {
                scrollSessionDao.insert(
                    ScrollSessionEntity(
                        id = UUID.randomUUID().toString(),
                        platform = platform.key,
                        localDay = localDay,
                        startedAtMillis = eventAtMillis,
                        endedAtMillis = eventAtMillis,
                        scrollCount = 1,
                        source = TrackingSource.ACCESSIBILITY.key,
                        syncState = SyncState.PENDING.key,
                    ),
                )
            } else {
                scrollSessionDao.increment(
                    id = existing.id,
                    eventAtMillis = maxOf(eventAtMillis, existing.endedAtMillis),
                    syncState = SyncState.PENDING.key,
                )
            }
        }
    }

    override suspend fun allSessions(): List<ScrollSession> =
        scrollSessionDao.getAll().map { it.toDomain() }

    override suspend fun pendingForSync(limit: Int): List<ScrollSession> =
        scrollSessionDao.getPending(limit).map { it.toDomain() }

    override suspend fun markSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        scrollSessionDao.updateSyncState(ids, SyncState.SYNCED.key)
    }
}
