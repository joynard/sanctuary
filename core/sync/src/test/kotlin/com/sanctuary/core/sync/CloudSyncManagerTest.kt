package com.sanctuary.core.sync

import com.sanctuary.core.data.ProfileDraft
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.ScrollRepository
import com.sanctuary.core.model.ScrollPlatform
import com.sanctuary.core.model.ScrollSession
import com.sanctuary.core.model.UserProfile
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudSyncManagerTest {

    private lateinit var server: MockWebServer
    private lateinit var sessionStore: FakeSessionStore
    private lateinit var scrollRepository: FakeScrollRepository
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        sessionStore = FakeSessionStore()
        scrollRepository = FakeScrollRepository(
            sessions = mutableListOf(
                ScrollSession(
                    id = "s1",
                    platform = ScrollPlatform.TIKTOK,
                    localDay = LocalDate.of(2026, 10, 27),
                    startedAt = Instant.parse("2026-10-27T09:00:00Z"),
                    endedAt = Instant.parse("2026-10-27T09:02:00Z"),
                    scrollCount = 3,
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `first sync creates anonymous account and marks sessions synced`() = runTest {
        enqueueSession()
        enqueueProfile()
        enqueueSessions()
        val manager = manager()

        val result = manager.syncNow()

        assertTrue(
            "expected success, got ${result.exceptionOrNull()} state=${manager.state.value}",
            result.isSuccess,
        )
        assertEquals(1, result.getOrThrow().sessions)
        assertEquals("user-123", sessionStore.session?.userId)
        assertEquals(listOf("s1"), scrollRepository.marked)
        assertTrue(manager.state.value is CloudSyncState.Synced)

        val signup = server.takeRequest()
        assertEquals("/auth/v1/signup", signup.path)
        assertEquals("/rest/v1/profiles?on_conflict=id", server.takeRequest().path)
        assertEquals(
            "/rest/v1/scroll_sessions?on_conflict=user_id,client_id",
            server.takeRequest().path,
        )
    }

    @Test
    fun `linked sync refreshes expired token without creating another account`() = runTest {
        sessionStore.session = AuthSession(
            accessToken = "old-access",
            refreshToken = "old-refresh",
            expiresAtEpochSeconds = fixedClock.instant().epochSecond - 10,
            userId = "user-123",
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "access_token": "new-access",
                  "refresh_token": "new-refresh",
                  "expires_at": 2000000000,
                  "user": {"id": "user-123"}
                }
                """.trimIndent(),
            ),
        )
        enqueueProfile()
        enqueueSessions()

        val secondManager = manager()
        val result = secondManager.syncNow()

        assertTrue(
            "expected success, got ${result.exceptionOrNull()} state=${secondManager.state.value}",
            result.isSuccess,
        )
        assertEquals("new-access", sessionStore.session?.accessToken)
        assertEquals("/auth/v1/token?grant_type=refresh_token", server.takeRequest().path)
        assertEquals("/rest/v1/profiles?on_conflict=id", server.takeRequest().path)
        assertEquals(
            "/rest/v1/scroll_sessions?on_conflict=user_id,client_id",
            server.takeRequest().path,
        )
    }

    @Test
    fun `later sync reuses the anonymous session`() = runTest {
        enqueueSession()
        enqueueProfile()
        enqueueSessions()
        val manager = manager()
        manager.syncNow()
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()

        enqueueProfile()
        val second = manager.syncNow()

        assertTrue(second.isSuccess)
        assertEquals(0, second.getOrThrow().sessions)
        assertEquals("/rest/v1/profiles?on_conflict=id", server.takeRequest().path)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `offline failure keeps session and pending work`() = runTest {
        sessionStore.session = AuthSession(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAtEpochSeconds = 2_000_000_000L,
            userId = "user-123",
        )
        val manager = manager()
        server.shutdown()

        val result = manager.syncNow()

        assertTrue(
            "expected failure, got success state=${manager.state.value}",
            result.isFailure,
        )
        assertTrue(manager.state.value is CloudSyncState.Failed)
        assertNotNull(sessionStore.session)
        assertEquals(1, scrollRepository.pending().size)
        assertEquals(emptyList<String>(), scrollRepository.marked)
    }

    private fun manager(): CloudSyncManager {
        val config = SupabaseConfig(
            projectUrl = "http://127.0.0.1:${server.port}",
            publishableKey = "publishable-key",
        )
        return CloudSyncManager(
            config = config,
            sessionStore = sessionStore,
            profileRepository = FakeProfileRepository(),
            scrollRepository = scrollRepository,
            metadata = FakeMetadataStore(),
            clock = fixedClock,
        )
    }

    private fun enqueueSession() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "access_token": "access-token",
                  "refresh_token": "refresh-token",
                  "expires_at": 2000000000,
                  "user": {"id": "user-123"}
                }
                """.trimIndent(),
            ),
        )
    }

    private fun enqueueProfile() {
        server.enqueue(MockResponse().setResponseCode(201))
    }

    private fun enqueueSessions() {
        server.enqueue(MockResponse().setResponseCode(201))
    }
}

private class FakeSessionStore(var session: AuthSession? = null) : SessionStore {
    override suspend fun read(): AuthSession? = session

    override suspend fun write(session: AuthSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}

private class FakeMetadataStore : SyncMetadataStore {
    private var last: Instant? = null

    override suspend fun deviceId(): String = "device-1"

    override fun lastSyncedAt(): Instant? = last

    override fun setLastSyncedAt(at: Instant) {
        last = at
    }
}

private class FakeProfileRepository : ProfileRepository {
    private val profileValue = UserProfile(
        id = "local-profile",
        displayName = "Alex",
        dailyGoal = 100,
        intention = null,
        timezoneId = "Asia/Jakarta",
        onboardingComplete = true,
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-10-02T00:00:00Z"),
    )

    override val profile: Flow<UserProfile?> = flowOf(profileValue)

    override suspend fun ensureDefault(): UserProfile = profileValue

    override suspend fun save(
        draft: ProfileDraft,
        markOnboardingComplete: Boolean,
    ): Result<UserProfile> = Result.success(profileValue)
}

private class FakeScrollRepository(
    private val sessions: MutableList<ScrollSession>,
) : ScrollRepository {
    val marked = mutableListOf<String>()

    fun pending(): List<ScrollSession> = sessions.toList()

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<ScrollSession>> =
        flowOf(sessions.toList())

    override suspend fun recordScroll(
        platform: ScrollPlatform,
        at: Instant,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun allSessions(): List<ScrollSession> = sessions.toList()

    override suspend fun pendingForSync(limit: Int): List<ScrollSession> =
        sessions.take(limit)

    override suspend fun markSynced(ids: List<String>) {
        marked += ids
        sessions.removeAll { it.id in ids }
    }
}
