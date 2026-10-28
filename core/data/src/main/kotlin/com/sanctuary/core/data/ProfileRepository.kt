package com.sanctuary.core.data

import com.sanctuary.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

data class ProfileDraft(
    val displayName: String,
    val dailyGoal: Int,
    val intention: String?,
)

interface ProfileRepository {
    val profile: Flow<UserProfile?>

    suspend fun ensureDefault(): UserProfile

    suspend fun save(
        draft: ProfileDraft,
        markOnboardingComplete: Boolean,
    ): Result<UserProfile>
}
