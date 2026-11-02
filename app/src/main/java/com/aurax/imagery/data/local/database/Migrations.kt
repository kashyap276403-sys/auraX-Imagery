package com.aurax.imagery.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations. The SQL lives in constants so a unit test can compare it, character for
 * character, with the schema Room exports at build time (see MigrationSqlTest).
 */
object Migrations {
    const val BOOKMARKS_TABLE =
        "CREATE TABLE IF NOT EXISTS `video_bookmarks` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`mediaId` INTEGER NOT NULL, " +
            "`positionMs` INTEGER NOT NULL, " +
            "`name` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL)"
    const val BOOKMARKS_INDEX =
        "CREATE INDEX IF NOT EXISTS `index_video_bookmarks_mediaId` ON `video_bookmarks` (`mediaId`)"

    const val PROGRESS_TABLE =
        "CREATE TABLE IF NOT EXISTS `playback_progress` (" +
            "`mediaId` INTEGER NOT NULL, " +
            "`positionMs` INTEGER NOT NULL, " +
            "`durationMs` INTEGER NOT NULL, " +
            "`lastPlayedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`mediaId`))"
    const val PROGRESS_INDEX =
        "CREATE INDEX IF NOT EXISTS `index_playback_progress_lastPlayedAt` ON `playback_progress` (`lastPlayedAt`)"

    /** Adds video bookmarks; the media index and favorites are untouched. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(BOOKMARKS_TABLE)
            db.execSQL(BOOKMARKS_INDEX)
        }
    }

    /** Adds resume positions / playback history. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(PROGRESS_TABLE)
            db.execSQL(PROGRESS_INDEX)
        }
    }
}
