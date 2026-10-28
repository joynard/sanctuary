package com.sanctuary.core.sync

import com.sanctuary.core.model.ScrollPlatform
import com.sanctuary.core.model.ScrollSession
import com.sanctuary.core.model.UserProfile
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupabaseRestClientTest {

    private lateinit var server: MockWebServer
    private val session = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAtEpochSeconds = 2_000_000_000L,
        userId = "user-123",
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `profile upsert uses merge precedence and auth token`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        val client = client()
        val profile = UserProfile(
            id = "local-profile",
            displayName = "Alex",
            dailyGoal = 120,
            intention = null,
            timezoneId = "Asia/Jakarta",
            onboardingComplete = true,
            createdAt = Instant.parse("2026-10-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-10-02T00:00:00Z"),
        )

        client.upsertProfile(profile, session)

        val request = server.takeRequest()
        assertEquals("/rest/v1/profiles?on_conflict=id", request.path)
        assertEquals("publishable-key", request.getHeader("apikey"))
        assertEquals("Bearer access-token", request.getHeader("Authorization"))
        assertTrue(request.getHeader("Prefer").orEmpty().contains("merge-duplicates"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"id\":\"user-123\""))
        assertTrue(body.contains("\"display_name\":\"Alex\""))
        assertTrue(body.contains("\"intention\":null"))
    }

    @Test
    fun `session upsert keeps client id for idempotency`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        val client = client()
        val item = ScrollSession(
            id = "client-session-1",
            platform = ScrollPlatform.INSTAGRAM,
            localDay = LocalDate.of(2026, 10, 27),
            startedAt = Instant.parse("2026-10-27T08:00:00Z"),
            endedAt = Instant.parse("2026-10-27T08:05:00Z"),
            scrollCount = 14,
        )

        client.upsertSessions(
            sessions = listOf(item),
            userId = "user-123",
            deviceId = "device-1",
            session = session,
        )

        val request = server.takeRequest()
        assertEquals(
            "/rest/v1/scroll_sessions?on_conflict=user_id,client_id",
            request.path,
        )
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"client_id\":\"client-session-1\""))
        assertTrue(body.contains("\"platform\":\"instagram\""))
        assertTrue(body.contains("\"scroll_count\":14"))
        assertTrue(body.contains("\"device_id\":\"device-1\""))
    }

    @Test
    fun `unauthorized response becomes typed exception`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"message":"JWT expired"}"""),
        )

        val error = assertThrows(SupabaseRestException::class.java) {
            kotlinx.coroutines.runBlocking {
                client().upsertSessions(
                    sessions = listOf(
                        ScrollSession(
                            id = "s1",
                            platform = ScrollPlatform.TIKTOK,
                            localDay = LocalDate.of(2026, 10, 27),
                            startedAt = Instant.parse("2026-10-27T08:00:00Z"),
                            endedAt = Instant.parse("2026-10-27T08:01:00Z"),
                            scrollCount = 1,
                        ),
                    ),
                    userId = "user-123",
                    deviceId = "device-1",
                    session = session,
                )
            }
        }

        assertEquals(401, error.statusCode)
    }

    private fun client(): SupabaseRestClient = SupabaseRestClient(
        config = SupabaseConfig(
            projectUrl = server.url("/").toString().trimEnd('/'),
            publishableKey = "publishable-key",
        ),
    )
}
