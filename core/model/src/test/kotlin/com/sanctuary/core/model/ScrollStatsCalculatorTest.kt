package com.sanctuary.core.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollStatsCalculatorTest {

    private val day1 = LocalDate.of(2026, 10, 20)
    private val day2 = LocalDate.of(2026, 10, 21)
    private val day3 = LocalDate.of(2026, 10, 22)

    @Test
    fun `summarize fills missing days and aggregates platforms`() {
        val sessions = listOf(
            session("1", day1, ScrollPlatform.TIKTOK, 12),
            session("2", day1, ScrollPlatform.INSTAGRAM, 8),
            session("3", day3, ScrollPlatform.YOUTUBE, 5),
        )

        val stats = ScrollStatsCalculator.summarize(sessions, day1, day3)

        assertEquals(listOf(20, 0, 5), stats.points.map { it.total })
        assertEquals(25, stats.total)
        assertEquals(3, stats.points.size)
        assertEquals(25.0 / 3, stats.averagePerDay, 0.0001)
        assertEquals(day1, stats.peak?.date)
        assertEquals(
            mapOf(
                ScrollPlatform.TIKTOK to 12,
                ScrollPlatform.INSTAGRAM to 8,
                ScrollPlatform.YOUTUBE to 5,
            ),
            stats.platformTotals.associate { it.platform to it.total },
        )
    }

    @Test
    fun `sessions outside the range are excluded`() {
        val sessions = listOf(
            session("before", day1.minusDays(1), ScrollPlatform.TIKTOK, 99),
            session("inside", day2, ScrollPlatform.TIKTOK, 7),
            session("after", day3.plusDays(1), ScrollPlatform.TIKTOK, 88),
        )

        val stats = ScrollStatsCalculator.summarize(sessions, day2, day3)

        assertEquals(7, stats.total)
        assertEquals(1, stats.sessionCount)
    }

    @Test
    fun `summarizeDay keeps empty platforms at zero`() {
        val summary = ScrollStatsCalculator.summarizeDay(
            sessions = listOf(session("1", day1, ScrollPlatform.TIKTOK, 4)),
            day = day1,
        )

        assertEquals(4, summary.total)
        assertEquals(ScrollPlatform.entries.size, summary.platformTotals.size)
        assertEquals(
            0,
            summary.platformTotals.first { it.platform == ScrollPlatform.YOUTUBE }.total,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reversed range is rejected`() {
        ScrollStatsCalculator.summarize(emptyList(), day2, day1)
    }

    private fun session(
        id: String,
        day: LocalDate,
        platform: ScrollPlatform,
        count: Int,
    ): ScrollSession = ScrollSession(
        id = id,
        platform = platform,
        localDay = day,
        startedAt = Instant.parse("${day}T10:00:00Z"),
        endedAt = Instant.parse("${day}T10:05:00Z"),
        scrollCount = count,
    )
}
