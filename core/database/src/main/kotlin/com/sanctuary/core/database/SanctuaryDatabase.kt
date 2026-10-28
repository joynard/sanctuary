package com.sanctuary.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, ScrollSessionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SanctuaryDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun scrollSessionDao(): ScrollSessionDao

    companion object {
        private const val DATABASE_NAME = "sanctuary.db"

        @Volatile
        private var instance: SanctuaryDatabase? = null

        fun get(context: Context): SanctuaryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SanctuaryDatabase::class.java,
                    DATABASE_NAME,
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { instance = it }
            }
    }
}
