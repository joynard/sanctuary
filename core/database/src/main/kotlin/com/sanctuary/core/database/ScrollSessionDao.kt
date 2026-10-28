package com.sanctuary.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrollSessionDao {
    @Query(
        """
        SELECT * FROM scroll_sessions
        WHERE localDay BETWEEN :startDay AND :endDay
        ORDER BY localDay DESC, endedAtMillis DESC
        """,
    )
    fun observeRange(startDay: String, endDay: String): Flow<List<ScrollSessionEntity>>

    @Query(
        """
        SELECT * FROM scroll_sessions
        WHERE platform = :platform
          AND localDay = :localDay
          AND endedAtMillis >= :sinceMillis
        ORDER BY endedAtMillis DESC
        LIMIT 1
        """,
    )
    suspend fun findRecent(
        platform: String,
        localDay: String,
        sinceMillis: Long,
    ): ScrollSessionEntity?

    @Insert
    suspend fun insert(session: ScrollSessionEntity)

    @Query(
        """
        UPDATE scroll_sessions
        SET scrollCount = scrollCount + 1,
            endedAtMillis = :eventAtMillis,
            syncState = :syncState
        WHERE id = :id
        """,
    )
    suspend fun increment(
        id: String,
        eventAtMillis: Long,
        syncState: String,
    )

    @Query("SELECT * FROM scroll_sessions ORDER BY localDay DESC, endedAtMillis DESC")
    suspend fun getAll(): List<ScrollSessionEntity>

    @Query("SELECT COUNT(*) FROM scroll_sessions")
    suspend fun count(): Int

    @Query(
        """
        SELECT * FROM scroll_sessions
        WHERE syncState IN ('PENDING', 'FAILED')
        ORDER BY endedAtMillis ASC
        LIMIT :limit
        """,
    )
    suspend fun getPending(limit: Int): List<ScrollSessionEntity>

    @Query("UPDATE scroll_sessions SET syncState = :state WHERE id IN (:ids)")
    suspend fun updateSyncState(ids: List<String>, state: String)

    @Query("DELETE FROM scroll_sessions")
    suspend fun clear()
}
