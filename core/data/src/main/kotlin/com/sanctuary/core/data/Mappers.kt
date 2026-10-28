package com.sanctuary.core.data

import com.sanctuary.core.database.ProfileEntity
import com.sanctuary.core.database.ScrollSessionEntity
import com.sanctuary.core.model.ScrollPlatform
import com.sanctuary.core.model.ScrollSession
import com.sanctuary.core.model.SyncState
import com.sanctuary.core.model.TrackingSource
import com.sanctuary.core.model.UserProfile
import java.time.Instant
import java.time.LocalDate

internal fun ProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    displayName = displayName,
    dailyGoal = dailyGoal,
    intention = intention,
    timezoneId = timezoneId,
    onboardingComplete = onboardingComplete,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
)

internal fun UserProfile.toEntity(): ProfileEntity = ProfileEntity(
    id = id,
    displayName = displayName,
    dailyGoal = dailyGoal,
    intention = intention,
    timezoneId = timezoneId,
    onboardingComplete = onboardingComplete,
    createdAtMillis = createdAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli(),
)

internal fun ScrollSessionEntity.toDomain(): ScrollSession = ScrollSession(
    id = id,
    platform = ScrollPlatform.fromKey(platform) ?: ScrollPlatform.TIKTOK,
    localDay = LocalDate.parse(localDay),
    startedAt = Instant.ofEpochMilli(startedAtMillis),
    endedAt = Instant.ofEpochMilli(endedAtMillis),
    scrollCount = scrollCount,
    source = TrackingSource.fromKey(source),
    syncState = SyncState.fromKey(syncState),
)
