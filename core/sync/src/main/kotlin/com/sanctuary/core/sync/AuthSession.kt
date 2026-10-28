package com.sanctuary.core.sync

import kotlinx.serialization.Serializable

/**
 * A single anonymous Supabase identity. There is deliberately no email, phone,
 * or OAuth identity. Losing this refresh token (clear data/uninstall) loses the
 * cloud identity; the JSON backup remains the portable recovery path.
 */
@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val userId: String,
)

interface SessionStore {
    suspend fun read(): AuthSession?

    suspend fun write(session: AuthSession)

    suspend fun clear()
}
