package com.sanctuary.core.data

import com.sanctuary.core.model.ScrollPlatform
import com.sanctuary.core.model.ScrollSession
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ScrollRepository {
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<ScrollSession>>

    suspend fun recordScroll(platform: ScrollPlatform, at: Instant): Result<Unit>

    suspend fun allSessions(): List<ScrollSession>

    suspend fun pendingForSync(limit: Int): List<ScrollSession>

    suspend fun markSynced(ids: List<String>)
}
