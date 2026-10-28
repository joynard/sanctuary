package com.sanctuary.core.sync

import com.sanctuary.core.model.ScrollSession
import com.sanctuary.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SyncGateway {
    suspend fun upsertProfile(profile: UserProfile, session: AuthSession)

    suspend fun upsertSessions(
        sessions: List<ScrollSession>,
        userId: String,
        deviceId: String,
        session: AuthSession,
    )
}

internal class SupabaseRestClient(
    private val config: SupabaseConfig,
    private val http: SupabaseHttp = SupabaseHttp(config),
) : SyncGateway {

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    override suspend fun upsertProfile(
        profile: UserProfile,
        session: AuthSession,
    ) = withContext(Dispatchers.IO) {
        val payload = listOf(
            ProfilePayload(
                id = session.userId,
                displayName = profile.displayName,
                dailyGoal = profile.dailyGoal,
                intention = profile.intention,
                timezoneId = profile.timezoneId,
                onboardingComplete = profile.onboardingComplete,
                createdAt = profile.createdAt.toString(),
                updatedAt = profile.updatedAt.toString(),
            ),
        )
        val response = http.post(
            path = "/rest/v1/profiles?on_conflict=id",
            body = json.encodeToString(payload),
            bearerToken = session.accessToken,
            extraHeaders = MERGE_HEADERS,
        )
        ensureSuccess(response, "Profil")
    }

    override suspend fun upsertSessions(
        sessions: List<ScrollSession>,
        userId: String,
        deviceId: String,
        session: AuthSession,
    ) = withContext(Dispatchers.IO) {
        if (sessions.isEmpty()) return@withContext

        val payload = sessions.map { item ->
            SessionPayload(
                userId = userId,
                clientId = item.id,
                platform = item.platform.key,
                localDay = item.localDay.toString(),
                startedAt = item.startedAt.toString(),
                endedAt = item.endedAt.toString(),
                scrollCount = item.scrollCount,
                source = item.source.key,
                deviceId = deviceId,
            )
        }
        val response = http.post(
            path = "/rest/v1/scroll_sessions?on_conflict=user_id,client_id",
            body = json.encodeToString(payload),
            bearerToken = session.accessToken,
            extraHeaders = MERGE_HEADERS,
        )
        ensureSuccess(response, "Sesi")
    }

    private fun ensureSuccess(response: HttpResponse, label: String) {
        if (response.statusCode in 200..299) return
        val message = runCatching {
            json.decodeFromString<ErrorPayload>(response.body).message
        }.getOrNull()
            ?: "$label tidak dapat disinkronkan."
        throw SupabaseRestException(
            statusCode = response.statusCode,
            message = message.take(240),
        )
    }

    @Serializable
    private data class ProfilePayload(
        val id: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("daily_goal") val dailyGoal: Int,
        val intention: String?,
        @SerialName("timezone_id") val timezoneId: String,
        @SerialName("onboarding_complete") val onboardingComplete: Boolean,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    private data class SessionPayload(
        @SerialName("user_id") val userId: String,
        @SerialName("client_id") val clientId: String,
        val platform: String,
        @SerialName("local_day") val localDay: String,
        @SerialName("started_at") val startedAt: String,
        @SerialName("ended_at") val endedAt: String,
        @SerialName("scroll_count") val scrollCount: Int,
        val source: String,
        @SerialName("device_id") val deviceId: String,
    )

    @Serializable
    private data class ErrorPayload(
        val message: String? = null,
    )

    private companion object {
        val MERGE_HEADERS = mapOf(
            "Prefer" to "resolution=merge-duplicates,return=minimal",
        )
    }
}
