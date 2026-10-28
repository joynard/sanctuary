package com.sanctuary.core.tracking

import com.sanctuary.core.model.ScrollPlatform
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollEventGateTest {

    @Test
    fun `a long fling produces one count`() {
        val gate = ScrollEventGate(quietPeriodMillis = 350)

        assertTrue(gate.accept(ScrollPlatform.TIKTOK, 1_000))
        // Events every 100 ms for a full second belong to the same gesture.
        for (elapsed in 1_100..2_000 step 100) {
            assertFalse(gate.accept(ScrollPlatform.TIKTOK, elapsed.toLong()))
        }
        assertFalse(gate.accept(ScrollPlatform.TIKTOK, 2_300))
        assertTrue(gate.accept(ScrollPlatform.TIKTOK, 2_650))
    }

    @Test
    fun `platforms have independent gates`() {
        val gate = ScrollEventGate(quietPeriodMillis = 350)

        assertTrue(gate.accept(ScrollPlatform.TIKTOK, 1_000))
        assertTrue(gate.accept(ScrollPlatform.INSTAGRAM, 1_050))
        assertTrue(gate.accept(ScrollPlatform.YOUTUBE, 1_100))
        assertFalse(gate.accept(ScrollPlatform.INSTAGRAM, 1_200))
    }

    @Test
    fun `backwards elapsed time does not suppress forever`() {
        val gate = ScrollEventGate(quietPeriodMillis = 350)

        assertTrue(gate.accept(ScrollPlatform.TIKTOK, 10_000))
        assertTrue(gate.accept(ScrollPlatform.TIKTOK, 100))
    }
}
