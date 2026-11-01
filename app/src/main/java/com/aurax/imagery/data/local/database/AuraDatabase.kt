package com.aurax.imagery.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediaEntity::class, FavoriteEntity::class, BookmarkEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun create(context: Context): AuraDatabase =
            Room.databaseBuilder(context.applicationContext, AuraDatabase::class.java, "aurax.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** Adds video bookmarks; existing media index and favorites are untouched. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `video_bookmarks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`mediaId` INTEGER NOT NULL, " +
                        "`positionMs` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_bookmarks_mediaId` ON `video_bookmarks` (`mediaId`)")
            }
        }
    }
}
