package com.sanctuary.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Portable, deliberately flat backup contract. It is versioned independently of
 * Room so a future import can migrate without knowing database internals.
 */
@Serializable
data class BackupDocument(
    val format: String = FORMAT,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: String,
    val appVersion: String,
    val profile: BackupProfile?,
    val sessions: List<BackupSession>,
) {
    companion object {
        const val FORMAT = "sanctuary.backup"
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupProfile(
    @SerialName("local_id") val localId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("daily_goal") val dailyGoal: Int,
    val intention: String?,
    @SerialName("timezone_id") val timezoneId: String,
    @SerialName("onboarding_complete") val onboardingComplete: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class BackupSession(
    val id: String,
    val platform: String,
    @SerialName("local_day") val localDay: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("scroll_count") val scrollCount: Int,
    val source: String,
)
