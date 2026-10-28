package com.sanctuary.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDocumentTest {

    @Test
    fun `backup contract round trips with stable keys`() {
        val document = BackupDocument(
            exportedAt = "2026-10-27T08:00:00Z",
            appVersion = "0.1.0",
            profile = BackupProfile(
                localId = "local-profile",
                displayName = "Sahabat",
                dailyGoal = 100,
                intention = null,
                timezoneId = "Asia/Jakarta",
                onboardingComplete = true,
                createdAt = "2026-10-20T08:00:00Z",
                updatedAt = "2026-10-27T08:00:00Z",
            ),
            sessions = listOf(
                BackupSession(
                    id = "session-1",
                    platform = "tiktok",
                    localDay = "2026-10-27",
                    startedAt = "2026-10-27T07:00:00Z",
                    endedAt = "2026-10-27T07:02:00Z",
                    scrollCount = 14,
                    source = "accessibility",
                ),
            ),
        )

        val encoder = Json { encodeDefaults = true }
        val json = encoder.encodeToString(document)
        assertTrue(json.contains("\"format\":\"sanctuary.backup\""))
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"daily_goal\":100"))
        assertTrue(json.contains("\"scroll_count\":14"))

        val decoded = encoder.decodeFromString<BackupDocument>(json)
        assertEquals(document, decoded)
    }
}
