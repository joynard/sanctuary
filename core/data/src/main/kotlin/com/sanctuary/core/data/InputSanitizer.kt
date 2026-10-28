package com.sanctuary.core.data

import com.sanctuary.core.model.UserProfile

/**
 * All user-authored text passes through here before persistence. Compose does not
 * interpret markup, but control characters and unbounded values can still corrupt
 * exports or break layouts.
 */
object InputSanitizer {
    private val CONTROL_CHARS = Regex("[\\u0000-\\u001F\\u007F]")
    private val WHITESPACE = Regex("\\s+")

    fun displayName(raw: String): String {
        val cleaned = clean(raw)
        return cleaned
            .take(UserProfile.MAX_DISPLAY_NAME_LENGTH)
            .ifBlank { "Sahabat" }
    }

    fun intention(raw: String?): String? =
        raw?.let(::clean)
            ?.take(UserProfile.MAX_INTENTION_LENGTH)
            ?.ifBlank { null }

    fun dailyGoal(raw: Int): Int =
        raw.coerceIn(UserProfile.MIN_DAILY_GOAL, UserProfile.MAX_DAILY_GOAL)

    private fun clean(raw: String): String =
        raw
            .replace(CONTROL_CHARS, " ")
            .replace(WHITESPACE, " ")
            .trim()
}
