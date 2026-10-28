package com.sanctuary.core.model

import java.time.Instant

data class UserProfile(
    val id: String,
    val displayName: String,
    val dailyGoal: Int,
    val intention: String?,
    val timezoneId: String,
    val onboardingComplete: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(dailyGoal > 0) { "dailyGoal must be positive" }
    }

    companion object {
        const val DEFAULT_DAILY_GOAL = 100
        const val MIN_DAILY_GOAL = 20
        const val MAX_DAILY_GOAL = 500
        const val MAX_DISPLAY_NAME_LENGTH = 40
        const val MAX_INTENTION_LENGTH = 120
    }
}
