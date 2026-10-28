package com.sanctuary.core.sync

data class SupabaseConfig(
    val projectUrl: String,
    val publishableKey: String,
) {
    val baseUrl: String = projectUrl.trim().trimEnd('/')

    val isConfigured: Boolean
        get() = publishableKey.isNotBlank() && (
            baseUrl.startsWith("https://") ||
                baseUrl.startsWith("http://127.0.0.1") ||
                baseUrl.startsWith("http://localhost")
            )
}
