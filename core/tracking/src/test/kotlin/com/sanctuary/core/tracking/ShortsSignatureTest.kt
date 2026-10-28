package com.sanctuary.core.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortsSignatureTest {

    @Test
    fun `channel and title form a stable signature`() {
        val signature = signatureFromDescriptions(
            listOf(
                "Go to channel Slow Living",
                "Like this video",
                "A quiet morning",
                "Home",
            ),
        )

        assertEquals("Go to channel Slow Living|A quiet morning", signature)
    }

    @Test
    fun `indonesian channel label is recognized`() {
        val signature = signatureFromDescriptions(
            listOf(
                "Buka channel Alam",
                "Pagi yang tenang",
                "Komentar",
            ),
        )

        assertEquals("Buka channel Alam|Pagi yang tenang", signature)
    }

    @Test
    fun `chrome and navigation labels alone do not count`() {
        assertNull(
            signatureFromDescriptions(
                listOf("Shorts", "Like this video", "Comment", "Share", "Subscribe"),
            ),
        )
    }

    @Test
    fun `progress timer is not treated as a title`() {
        assertNull(signatureFromDescriptions(listOf("0:12 of 0:45")))
    }

    @Test
    fun `a normal title containing like is kept`() {
        val signature = signatureFromDescriptions(
            listOf("Go to channel Studio", "I like this song"),
        )

        assertEquals("Go to channel Studio|I like this song", signature)
    }

    @Test
    fun `different shorts produce different signatures`() {
        val first = signatureFromDescriptions(
            listOf("Go to channel Studio", "First short"),
        )
        val second = signatureFromDescriptions(
            listOf("Go to channel Studio", "Second short"),
        )

        assertNotEquals(first, second)
    }
}
