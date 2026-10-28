package com.sanctuary.core.model

import java.time.Instant
import java.time.LocalDate

data class ScrollSession(
    val id: String,
    val platform: ScrollPlatform,
    val localDay: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant,
    val scrollCount: Int,
    val source: TrackingSource = TrackingSource.ACCESSIBILITY,
    val syncState: SyncState = SyncState.PENDING,
) {
    init {
        require(scrollCount >= 0) { "scrollCount must be non-negative" }
        require(!endedAt.isBefore(startedAt)) { "endedAt must not precede startedAt" }
    }

    val durationMillis: Long
        get() = endedAt.toEpochMilli() - startedAt.toEpochMilli()
}

enum class TrackingSource(val key: String) {
    ACCESSIBILITY("accessibility"),
    MANUAL("manual"),
    IMPORT("import"),
    ;

    companion object {
        fun fromKey(key: String): TrackingSource =
            entries.firstOrNull { it.key == key } ?: ACCESSIBILITY
    }
}

enum class SyncState(val key: String) {
    LOCAL_ONLY("local_only"),
    PENDING("pending"),
    SYNCED("synced"),
    FAILED("failed"),
    ;

    companion object {
        fun fromKey(key: String): SyncState =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

data class DailyScrollPoint(
    val date: LocalDate,
    val total: Int,
)

data class PlatformScrollTotal(
    val platform: ScrollPlatform,
    val total: Int,
)

data class DailyScrollSummary(
    val date: LocalDate,
    val platformTotals: List<PlatformScrollTotal>,
    val sessionCount: Int,
) {
    val total: Int = platformTotals.sumOf { it.total }
}

data class ScrollStats(
    val points: List<DailyScrollPoint>,
    val platformTotals: List<PlatformScrollTotal>,
    val sessionCount: Int,
) {
    val total: Int = points.sumOf { it.total }
    val averagePerDay: Double =
        if (points.isEmpty()) 0.0 else total.toDouble() / points.size
    val peak: DailyScrollPoint? = points.maxByOrNull { it.total }
}
