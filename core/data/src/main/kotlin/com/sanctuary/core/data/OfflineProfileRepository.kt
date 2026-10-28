package com.sanctuary.core.data

import com.sanctuary.core.database.ProfileDao
import com.sanctuary.core.database.ProfileEntity
import com.sanctuary.core.model.UserProfile
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineProfileRepository(
    private val profileDao: ProfileDao,
    private val clock: Clock,
) : ProfileRepository {

    override val profile: Flow<UserProfile?> =
        profileDao.observeFirst().map { it?.toDomain() }

    override suspend fun ensureDefault(): UserProfile {
        profileDao.getFirst()?.let { return it.toDomain() }

        val now = clock.instant()
        val entity = ProfileEntity(
            id = LOCAL_PROFILE_ID,
            displayName = "Sahabat",
            dailyGoal = UserProfile.DEFAULT_DAILY_GOAL,
            intention = null,
            timezoneId = ZoneId.systemDefault().id,
            onboardingComplete = false,
            createdAtMillis = now.toEpochMilli(),
            updatedAtMillis = now.toEpochMilli(),
        )
        profileDao.upsert(entity)
        return profileDao.getFirst()?.toDomain() ?: entity.toDomain()
    }

    override suspend fun save(
        draft: ProfileDraft,
        markOnboardingComplete: Boolean,
    ): Result<UserProfile> = runCatching {
        val current = profileDao.getFirst() ?: ensureDefault().toEntity()
        val now = clock.instant()
        val updated = current.copy(
            displayName = InputSanitizer.displayName(draft.displayName),
            dailyGoal = InputSanitizer.dailyGoal(draft.dailyGoal),
            intention = InputSanitizer.intention(draft.intention),
            onboardingComplete = current.onboardingComplete || markOnboardingComplete,
            updatedAtMillis = now.toEpochMilli(),
        )
        profileDao.upsert(updated)
        updated.toDomain()
    }

    private companion object {
        const val LOCAL_PROFILE_ID = "local-profile"
    }
}
