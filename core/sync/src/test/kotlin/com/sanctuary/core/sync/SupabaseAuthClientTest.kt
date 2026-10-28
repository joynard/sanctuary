package com.sanctuary.core.sync

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupabaseAuthClientTest {

    private lateinit var server: MockWebServer

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
    fun `anonymous signup sends publishable key and parses session`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
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
        val client = client()

        val session = client.signInAnonymously()

        val request = server.takeRequest()
        assertEquals("/auth/v1/signup", request.path)
        assertEquals("publishable-key", request.getHeader("apikey"))
        assertEquals("Bearer publishable-key", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"data\""))
        assertEquals("access-token", session.accessToken)
        assertEquals("refresh-token", session.refreshToken)
        assertEquals("user-123", session.userId)
        assertEquals(2_000_000_000L, session.expiresAtEpochSeconds)
    }

    @Test
    fun `refresh uses refresh token grant and replaces session`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "access_token": "new-access",
                      "refresh_token": "new-refresh",
                      "expires_in": 3600,
                      "user": {"id": "user-123"}
                    }
                    """.trimIndent(),
                ),
        )

        val session = client().refresh("old-refresh")

        val request = server.takeRequest()
        assertEquals("/auth/v1/token?grant_type=refresh_token", request.path)
        assertTrue(request.body.readUtf8().contains("old-refresh"))
        assertEquals("new-access", session.accessToken)
        assertEquals("new-refresh", session.refreshToken)
        assertEquals(1_700_000_000L + 3_600L, session.expiresAtEpochSeconds)
    }

    @Test
    fun `disabled anonymous signup surfaces typed exception`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"error":"anonymous_provider_disabled","msg":"Anonymous sign-ins are disabled"}"""),
        )

        val error = assertThrows(SupabaseAuthException::class.java) {
            kotlinx.coroutines.runBlocking { client().signInAnonymously() }
        }

        assertEquals(422, error.statusCode)
    }

    private fun client(): SupabaseAuthClient = SupabaseAuthClient(
        config = SupabaseConfig(
            projectUrl = server.url("/").toString().trimEnd('/'),
            publishableKey = "publishable-key",
        ),
        clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC),
    )
}
