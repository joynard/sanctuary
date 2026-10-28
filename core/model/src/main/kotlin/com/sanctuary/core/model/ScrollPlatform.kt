package com.sanctuary.core.model

/**
 * Platforms are intentionally scoped to vertical, scroll-driven short video feeds.
 *
 * Android exposes scroll events per package, not per in-app tab. A count therefore
 * represents committed scrolls while the package is in use, including the feed
 * around the short-video surface. The UI must not claim "videos watched".
 */
enum class ScrollPlatform(
    val key: String,
    val displayName: String,
    val initial: String,
    val packageNames: Set<String>,
) {
    TIKTOK(
        key = "tiktok",
        displayName = "TikTok",
        initial = "T",
        packageNames = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
    ),
    INSTAGRAM(
        key = "instagram",
        displayName = "Instagram",
        initial = "I",
        packageNames = setOf("com.instagram.android"),
    ),
    YOUTUBE(
        key = "youtube",
        displayName = "YouTube",
        initial = "Y",
        packageNames = setOf("com.google.android.youtube"),
    ),
    ;

    companion object {
        fun fromKey(key: String): ScrollPlatform? = entries.firstOrNull { it.key == key }

        fun fromPackageName(packageName: String?): ScrollPlatform? {
            if (packageName.isNullOrBlank()) return null
            return entries.firstOrNull { packageName in it.packageNames }
        }
    }
}
