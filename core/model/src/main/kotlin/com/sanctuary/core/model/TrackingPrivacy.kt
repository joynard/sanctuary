package com.sanctuary.core.model

/**
 * User-facing precision statement. Keep this in sync with the accessibility
 * configuration: Sanctuary observes only scroll events from named packages and
 * never reads screen content.
 */
object TrackingPrivacy {
    val trackedPackages: Set<String> = ScrollPlatform.entries
        .flatMap { it.packageNames }
        .toSet()

    const val CONTENT_READING_ENABLED = false
    const val SESSION_GAP_MINUTES = 30
}
