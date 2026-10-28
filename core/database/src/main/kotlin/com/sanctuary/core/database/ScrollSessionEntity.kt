package com.sanctuary.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scroll_sessions",
    indices = [
        Index("localDay"),
        Index("platform", "localDay"),
        Index("endedAtMillis"),
        Index("syncState"),
    ],
)
data class ScrollSessionEntity(
    @PrimaryKey val id: String,
    val platform: String,
    val localDay: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val scrollCount: Int,
    val source: String,
    val syncState: String,
)
