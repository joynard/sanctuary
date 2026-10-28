package com.sanctuary.core.sync

import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.ScrollRepository
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class CloudSyncFailure {
    OFFLINE,
    ANONYMOUS_DISABLED,
    RATE_LIMITED,
    AUTH_REJECTED,
    SERVER,
    UNKNOWN,
}

sealed interface CloudSyncState {
    data object NotConfigured : CloudSyncState
    data object NotLinked : CloudSyncState
    data class Linked(val lastSyncedAt: Instant?) : CloudSyncState
    data object Syncing : CloudSyncState
    data class Synced(val sessions: Int, val at: Instant) : CloudSyncState
    data class Failed(val reason: CloudSyncFailure) : CloudSyncState
}

data class SyncSummary(
    val sessions: Int,
    val at: Instant,
)

/**
 * One anonymous account per install, created only when the user activates cloud
 * sync. There is no email/Google flow. The refresh token is the only credential
 * and is loss if app data is cleared, so JSON export remains the recovery path.
 */
class CloudSyncManager(
    private val config: SupabaseConfig,
    private val sessionStore: SessionStore,
    private val profileRepository: ProfileRepository,
    private val scrollRepository: ScrollRepository,
    private val metadata: SyncMetadataStore,
    private val clock: Clock,
    private val auth: AuthGateway = SupabaseAuthClient(config, clock),
    private val gateway: SyncGateway = SupabaseRestClient(config),
) {
    private val syncMutex = Mutex()
    private val _state = MutableStateFlow<CloudSyncState>(
        if (config.isConfigured) CloudSyncState.NotLinked else CloudSyncState.NotConfigured,
    )
    val state: StateFlow<CloudSyncState> = _state.asStateFlow()

    suspend fun refreshState() {
        if (!config.isConfigured) {
            _state.value = CloudSyncState.NotConfigured
            return
        }
        val session = sessionStore.read()
        _state.value = if (session == null) {
            CloudSyncState.NotLinked
        } else {
            CloudSyncState.Linked(metadata.lastSyncedAt())
        }
    }

    /**
     * Creates the anonymous account on first call, then pushes the local profile
     * and every pending session. Safe to call repeatedly; upserts are idempotent.
     */
    suspend fun syncNow(): Result<SyncSummary> = syncMutex.withLock {
        if (!config.isConfigured) {
            return@withLock Result.failure(
                IllegalStateException("Supabase belum dikonfigurasi."),
            )
        }

        _state.value = CloudSyncState.Syncing
        runCatching {
            val session = activeSession()
            val profile = profileRepository.ensureDefault()
            val deviceId = metadata.deviceId()

            gateway.upsertProfile(profile, session)

            var pushed = 0
            while (true) {
                val pending = scrollRepository.pendingForSync(SYNC_BATCH_SIZE)
                if (pending.isEmpty()) break
                gateway.upsertSessions(
                    sessions = pending,
                    userId = session.userId,
                    deviceId = deviceId,
                    session = session,
                )
                scrollRepository.markSynced(pending.map { it.id })
                pushed += pending.size
            }

            val at = clock.instant()
            metadata.setLastSyncedAt(at)
            _state.value = CloudSyncState.Synced(pushed, at)
            SyncSummary(pushed, at)
        }.onFailure { error ->
            _state.value = CloudSyncState.Failed(classify(error))
        }
    }

    /**
     * Called on app start. It never creates a new account; only an already
     * activated install syncs silently.
     */
    suspend fun syncIfLinked() {
        if (!config.isConfigured) return
        val session = sessionStore.read() ?: run {
            _state.value = CloudSyncState.NotLinked
            return
        }
        if (session.accessToken.isNotBlank()) {
            syncNow()
        }
    }

    private suspend fun activeSession(): AuthSession {
        var session = sessionStore.read()
            ?: auth.signInAnonymously().also { sessionStore.write(it) }

        val refreshThreshold = clock.instant().epochSecond + REFRESH_SKEW_SECONDS
        if (session.expiresAtEpochSeconds <= refreshThreshold) {
            session = auth.refresh(session.refreshToken)
            sessionStore.write(session)
        }
        return session
    }

    private fun classify(error: Throwable): CloudSyncFailure {
        if (error is UnknownHostException ||
            error is ConnectException ||
            error is SocketTimeoutException
        ) {
            return CloudSyncFailure.OFFLINE
        }
        if (error is SupabaseAuthException) {
            return when {
                error.statusCode == 422 -> CloudSyncFailure.ANONYMOUS_DISABLED
                error.statusCode == 429 -> CloudSyncFailure.RATE_LIMITED
                error.statusCode == 401 || error.statusCode == 403 ->
                    CloudSyncFailure.AUTH_REJECTED
                error.statusCode >= 500 -> CloudSyncFailure.SERVER
                else -> CloudSyncFailure.UNKNOWN
            }
        }
        if (error is SupabaseRestException) {
            return when {
                error.statusCode == 401 || error.statusCode == 403 ->
                    CloudSyncFailure.AUTH_REJECTED
                error.statusCode == 429 -> CloudSyncFailure.RATE_LIMITED
                error.statusCode >= 500 -> CloudSyncFailure.SERVER
                else -> CloudSyncFailure.UNKNOWN
            }
        }
        if (error is IOException) return CloudSyncFailure.OFFLINE
        return CloudSyncFailure.UNKNOWN
    }

    private companion object {
        const val SYNC_BATCH_SIZE = 200
        const val REFRESH_SKEW_SECONDS = 60L
    }
}
