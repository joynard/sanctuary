package com.sanctuary.core.model

import java.time.LocalDate

object ScrollStatsCalculator {
    fun summarize(
        sessions: List<ScrollSession>,
        start: LocalDate,
        end: LocalDate,
        platforms: List<ScrollPlatform> = ScrollPlatform.entries,
    ): ScrollStats {
        require(!end.isBefore(start)) { "end must not precede start" }

        val inRange = sessions.filter { it.localDay in start..end }
        val totalsByDay = inRange
            .groupBy { it.localDay }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.scrollCount } }

        val points = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { day -> DailyScrollPoint(day, totalsByDay[day] ?: 0) }
            .toList()

        val totalsByPlatform = inRange
            .groupBy { it.platform }
            .mapValues { (_, platformSessions) -> platformSessions.sumOf { it.scrollCount } }

        return ScrollStats(
            points = points,
            platformTotals = platforms.map { platform ->
                PlatformScrollTotal(platform, totalsByPlatform[platform] ?: 0)
            },
            sessionCount = inRange.size,
        )
    }

    fun summarizeDay(
        sessions: List<ScrollSession>,
        day: LocalDate,
        platforms: List<ScrollPlatform> = ScrollPlatform.entries,
    ): DailyScrollSummary {
        val daySessions = sessions.filter { it.localDay == day }
        val totals = daySessions.groupBy { it.platform }
            .mapValues { (_, platformSessions) -> platformSessions.sumOf { it.scrollCount } }
        return DailyScrollSummary(
            date = day,
            platformTotals = platforms.map { PlatformScrollTotal(it, totals[it] ?: 0) },
            sessionCount = daySessions.size,
        )
    }
}
