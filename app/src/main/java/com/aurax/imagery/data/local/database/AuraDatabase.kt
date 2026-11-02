package com.aurax.imagery.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaEntity::class, FavoriteEntity::class, BookmarkEntity::class, PlaybackProgressEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun playbackProgressDao(): PlaybackProgressDao

    companion object {
        fun create(context: Context): AuraDatabase =
            Room.databaseBuilder(context.applicationContext, AuraDatabase::class.java, "aurax.db")
                .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
                .build()
    }
}
