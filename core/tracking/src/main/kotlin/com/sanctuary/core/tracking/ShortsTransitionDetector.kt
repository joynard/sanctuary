package com.sanctuary.core.tracking

import android.view.accessibility.AccessibilityNodeInfo

/**
 * YouTube Shorts does not emit TYPE_VIEW_SCROLLED for its pager. The only stable
 * signal is the title/channel transition exposed through the accessibility node
 * tree, so this detector reads a small set of descriptions from YouTube only.
 * Nothing is persisted or logged.
 */
internal class ShortsTransitionDetector {

    @Volatile
    var isInShorts: Boolean = false
        private set

    private var lastSignature: String? = null
    private var lastEvaluationElapsed: Long = -EVALUATION_INTERVAL_MS

    fun shouldEvaluate(nowElapsed: Long): Boolean {
        if (nowElapsed - lastEvaluationElapsed < EVALUATION_INTERVAL_MS) return false
        lastEvaluationElapsed = nowElapsed
        return true
    }

    fun onContentEvent(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) {
            reset()
            return false
        }
        if (!root.isShortsSurface()) {
            reset()
            return false
        }

        val descriptions = ArrayList<String>(MAX_DESCRIPTIONS)
        collectDescriptions(root, descriptions, depth = 0)
        val signature = signatureFromDescriptions(descriptions) ?: return false
        isInShorts = true

        val previous = lastSignature
        lastSignature = signature
        return previous != null && previous != signature
    }

    fun reset() {
        isInShorts = false
        lastSignature = null
    }

    private fun collectDescriptions(
        node: AccessibilityNodeInfo,
        out: MutableList<String>,
        depth: Int,
    ) {
        if (depth > MAX_DEPTH || out.size >= MAX_DESCRIPTIONS) return
        node.contentDescription?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(out::add)
        node.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(out::add)
        for (index in 0 until node.childCount) {
            if (out.size >= MAX_DESCRIPTIONS) return
            node.getChild(index)?.let { collectDescriptions(it, out, depth + 1) }
        }
    }

    internal companion object {
        const val EVALUATION_INTERVAL_MS = 300L
        const val MAX_DEPTH = 40
        const val MAX_DESCRIPTIONS = 160

        val POSITIVE_VIEW_IDS = listOf(
            "com.google.android.youtube:id/reel_watch_player",
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_watch_fragment_root",
        )

        val NEGATIVE_VIEW_IDS = listOf(
            "com.google.android.youtube:id/reel_comments_bottom_sheet_background",
            "com.google.android.youtube:id/engagement_panel_content",
        )
    }
}

private fun AccessibilityNodeInfo.isShortsSurface(): Boolean {
    val hasPlayer = ShortsTransitionDetector.POSITIVE_VIEW_IDS.any { id ->
        findAccessibilityNodeInfosByViewId(id).any { it.isVisibleToUser }
    }
    if (!hasPlayer) return false
    return ShortsTransitionDetector.NEGATIVE_VIEW_IDS.none { id ->
        findAccessibilityNodeInfosByViewId(id).any { it.isVisibleToUser }
    }
}

internal fun signatureFromDescriptions(descriptions: List<String>): String? {
    val cleaned = descriptions
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()
    if (cleaned.isEmpty()) return null

    val channel = cleaned.firstOrNull { description ->
        CHANNEL_PREFIXES.any { description.startsWith(it, ignoreCase = true) }
    }

    val title = cleaned.asSequence()
        .filter { it != channel }
        .filter { it.length in MIN_TITLE_LENGTH..MAX_TITLE_LENGTH }
        .filterNot { description ->
            NAV_LABELS.any { it.equals(description, ignoreCase = true) }
        }
        .filterNot { description ->
            CHROME_PREFIXES.any { description.startsWith(it, ignoreCase = true) }
        }
        .filterNot(::looksLikeProgress)
        .maxByOrNull { it.length }
        ?: return null

    return "${channel.orEmpty()}|$title"
}

private fun looksLikeProgress(value: String): Boolean {
    val lower = value.lowercase()
    if (lower.matches(PROGRESS_REGEX)) return true
    if (INLINE_PROGRESS_REGEX.containsMatchIn(lower)) return true
    if (!lower.contains(" of ")) return false
    return lower.contains("second") || lower.contains("minute") ||
        lower.contains("detik") || lower.contains("menit")
}

private const val MIN_TITLE_LENGTH = 5
private const val MAX_TITLE_LENGTH = 200
private val PROGRESS_REGEX = Regex("""^\d{1,2}:\d{2}(?::\d{2})?$""")
private val INLINE_PROGRESS_REGEX = Regex("""\d{1,2}:\d{2}""")

private val CHANNEL_PREFIXES = listOf(
    "Go to channel",
    "Buka channel",
    "Kunjungi channel",
    "Buka saluran",
    "Kunjungi saluran",
)

private val NAV_LABELS = setOf(
    "Home",
    "Shorts",
    "Create",
    "You",
    "Search",
    "More",
    "Beranda",
    "Buat",
    "Kamu",
    "Telusuri",
)

private val CHROME_PREFIXES = listOf(
    "Like this video",
    "Dislike",
    "Comment",
    "Share",
    "Subscribe",
    "Remix",
    "Using this sound",
    "Save",
    "Add to playlist",
    "Not interested",
    "Report",
    "Suka video",
    "Tidak suka",
    "Komentar",
    "Bagikan",
    "Langganan",
    "Simpan",
    "Lainnya",
    "Tidak tertarik",
    "Laporkan",
)
