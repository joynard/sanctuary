package com.sanctuary.core.tracking

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.sanctuary.core.model.ScrollPlatform
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SanctuaryAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventGate = ScrollEventGate()
    private val shortsDetector = ShortsTransitionDetector()

    // Fail closed: do not count until the persisted pause preference has loaded.
    @Volatile
    private var paused = true

    @Volatile
    private var dependencies: TrackingDependencies? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val provider = applicationContext as? TrackingDependenciesProvider
        val resolved = provider?.trackingDependencies
        if (provider == null || resolved == null) {
            Log.e(TAG, "Tracking dependencies unavailable; service will not record.")
            return
        }
        dependencies = resolved
        serviceScope.launch {
            resolved.trackingPreferences.paused.collectLatest {
                paused = it
                if (it) shortsDetector.reset()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || paused) return

        val platform = ScrollPlatform.fromPackageName(event.packageName?.toString()) ?: return
        if (platform != ScrollPlatform.YOUTUBE) {
            shortsDetector.reset()
        }

        val nowElapsed = SystemClock.elapsedRealtime()

        // Shorts' pager emits no TYPE_VIEW_SCROLLED. Read only YouTube's title
        // descriptions to notice a page transition; never log or persist them.
        if (platform == ScrollPlatform.YOUTUBE && event.isContentEvent()) {
            if (!shortsDetector.shouldEvaluate(nowElapsed)) return
            val root = runCatching { rootInActiveWindow }.getOrNull()
            if (shortsDetector.onContentEvent(root)) {
                record(platform)
            }
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        if (platform == ScrollPlatform.YOUTUBE && shortsDetector.isInShorts) return
        if (!eventGate.accept(platform, nowElapsed)) return
        record(platform)
    }

    private fun record(platform: ScrollPlatform) {
        val repository = dependencies?.scrollRepository ?: return
        serviceScope.launch {
            repository.recordScroll(platform, Instant.now())
                .onFailure { error ->
                    // Never log window content or package UI state.
                    Log.w(TAG, "Scroll could not be stored.", error)
                }
        }
    }

    override fun onInterrupt() {
        shortsDetector.reset()
    }

    override fun onDestroy() {
        shortsDetector.reset()
        serviceScope.cancel()
        dependencies = null
        super.onDestroy()
    }

    private fun AccessibilityEvent.isContentEvent(): Boolean =
        eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

    private companion object {
        const val TAG = "SanctuaryTracking"
    }
}
