package com.sanctuary.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAtMillis ASC LIMIT 1")
    fun observeFirst(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles ORDER BY createdAtMillis ASC LIMIT 1")
    suspend fun getFirst(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}
