package com.sanctuary.core.data

import com.sanctuary.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputSanitizerTest {

    @Test
    fun `control characters and repeated whitespace are removed`() {
        assertEquals(
            "Alex Rivera",
            InputSanitizer.displayName("  Alex\u0000   Rivera  "),
        )
    }

    @Test
    fun `blank display name falls back to Sahabat`() {
        assertEquals("Sahabat", InputSanitizer.displayName("   "))
    }

    @Test
    fun `display name is bounded`() {
        val result = InputSanitizer.displayName("a".repeat(200))
        assertEquals(UserProfile.MAX_DISPLAY_NAME_LENGTH, result.length)
    }

    @Test
    fun `blank intention becomes null`() {
        assertNull(InputSanitizer.intention(" \n\t "))
        assertNull(InputSanitizer.intention(null))
    }

    @Test
    fun `intention is bounded`() {
        val result = InputSanitizer.intention("b".repeat(500))
        assertEquals(UserProfile.MAX_INTENTION_LENGTH, result?.length)
    }

    @Test
    fun `daily goal is clamped`() {
        assertEquals(UserProfile.MIN_DAILY_GOAL, InputSanitizer.dailyGoal(-10))
        assertEquals(UserProfile.MAX_DAILY_GOAL, InputSanitizer.dailyGoal(10_000))
        assertEquals(100, InputSanitizer.dailyGoal(100))
    }
}
