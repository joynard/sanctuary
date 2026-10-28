package com.sanctuary.core.sync

import android.content.Context
import java.time.Instant

class LastSyncStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): Instant? = prefs.getLong(KEY_LAST_SYNC, 0L)
        .takeIf { it > 0L }
        ?.let(Instant::ofEpochMilli)

    fun write(at: Instant) {
        prefs.edit().putLong(KEY_LAST_SYNC, at.toEpochMilli()).apply()
    }

    private companion object {
        const val PREFS_NAME = "sanctuary_cloud_meta"
        const val KEY_LAST_SYNC = "last_sync_at"
    }
}
