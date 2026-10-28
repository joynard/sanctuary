package com.sanctuary.core.tracking

import com.sanctuary.core.model.ScrollPlatform

/**
 * A fling emits dozens of TYPE_VIEW_SCROLLED events. A count is committed for
 * the first event of a burst, then the gate waits for a quiet period before it
 * can count another gesture. This prevents a one-second fling from becoming five
 * "scrolls".
 */
class ScrollEventGate(
    private val quietPeriodMillis: Long = DEFAULT_QUIET_PERIOD_MILLIS,
) {
    private val lastEventAt = mutableMapOf<ScrollPlatform, Long>()

    @Synchronized
    fun accept(platform: ScrollPlatform, elapsedRealtimeMillis: Long): Boolean {
        val previous = lastEventAt[platform]
        if (previous != null) {
            val delta = elapsedRealtimeMillis - previous
            // A backwards clock indicates a reboot; never suppress forever.
            if (delta in 0 until quietPeriodMillis) {
                lastEventAt[platform] = maxOf(previous, elapsedRealtimeMillis)
                return false
            }
        }
        lastEventAt[platform] = elapsedRealtimeMillis
        return true
    }

    companion object {
        const val DEFAULT_QUIET_PERIOD_MILLIS = 350L
    }
}
