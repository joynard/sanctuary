package com.sanctuary.core.sync

import android.content.Context
import com.sanctuary.core.data.TrackingPreferences
import java.time.Instant

interface SyncMetadataStore {
    suspend fun deviceId(): String

    fun lastSyncedAt(): Instant?

    fun setLastSyncedAt(at: Instant)
}

class AndroidSyncMetadataStore(context: Context) : SyncMetadataStore {
    private val trackingPreferences = TrackingPreferences(context)
    private val lastSyncStore = LastSyncStore(context)

    override suspend fun deviceId(): String = trackingPreferences.deviceId()

    override fun lastSyncedAt(): Instant? = lastSyncStore.read()

    override fun setLastSyncedAt(at: Instant) = lastSyncStore.write(at)
}
