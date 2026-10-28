package com.sanctuary.core.sync

import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface AuthGateway {
    suspend fun signInAnonymously(): AuthSession

    suspend fun refresh(refreshToken: String): AuthSession
}

internal class SupabaseAuthClient(
    private val config: SupabaseConfig,
    private val clock: Clock,
    private val http: SupabaseHttp = SupabaseHttp(config),
) : AuthGateway {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun signInAnonymously(): AuthSession = withContext(Dispatchers.IO) {
        val response = http.post(
            path = "/auth/v1/signup",
            body = """{"data":{},"gotrue_meta_security":{}}""",
            bearerToken = config.publishableKey,
        )
        parseSession(response)
    }

    override suspend fun refresh(refreshToken: String): AuthSession =
        withContext(Dispatchers.IO) {
            val response = http.post(
                path = "/auth/v1/token?grant_type=refresh_token",
                body = json.encodeToString(RefreshRequest(refreshToken)),
                bearerToken = config.publishableKey,
            )
            parseSession(response)
        }

    private fun parseSession(response: HttpResponse): AuthSession {
        if (response.statusCode !in 200..299) {
            throw SupabaseAuthException(
                statusCode = response.statusCode,
                message = errorMessage(response),
            )
        }

        val payload = runCatching {
            json.decodeFromString<SessionPayload>(response.body)
        }.getOrElse {
            throw SupabaseAuthException(response.statusCode, "Respons autentikasi tidak valid.")
        }

        val accessToken = payload.accessToken
        val refreshToken = payload.refreshToken
        val userId = payload.user?.id
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank() || userId.isNullOrBlank()) {
            throw SupabaseAuthException(response.statusCode, "Sesi Supabase tidak lengkap.")
        }

        val expiresAt = payload.expiresAt
            ?: clock.instant().epochSecond + (payload.expiresIn ?: 3_600L)

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAt,
            userId = userId,
        )
    }

    private fun errorMessage(response: HttpResponse): String {
        val body = runCatching {
            json.decodeFromString<ErrorPayload>(response.body)
        }.getOrNull()
        val raw = body?.errorDescription
            ?: body?.message
            ?: body?.msg
            ?: body?.error
            ?: "Permintaan autentikasi gagal."
        // Server messages are developer-facing; keep them short and token-free.
        return raw.take(240)
    }

    @Serializable
    private data class RefreshRequest(
        @SerialName("refresh_token") val refreshToken: String,
    )

    @Serializable
    private data class SessionPayload(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_at") val expiresAt: Long? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
        val user: UserPayload? = null,
    )

    @Serializable
    private data class UserPayload(
        val id: String? = null,
    )

    @Serializable
    private data class ErrorPayload(
        @SerialName("error_description") val errorDescription: String? = null,
        val message: String? = null,
        val msg: String? = null,
        val error: String? = null,
    )
}
