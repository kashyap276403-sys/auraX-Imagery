package com.aurax.imagery.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        fun create(context: Context): AuraDatabase =
            Room.databaseBuilder(context.applicationContext, AuraDatabase::class.java, "aurax.db").build()
    }
}
