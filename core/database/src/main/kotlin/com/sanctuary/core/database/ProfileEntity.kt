package com.sanctuary.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val dailyGoal: Int,
    val intention: String?,
    val timezoneId: String,
    val onboardingComplete: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
