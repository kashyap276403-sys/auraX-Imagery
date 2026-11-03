package com.aurax.imagery.data

import com.aurax.imagery.data.local.database.Migrations
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Room cannot be run on the JVM here, so this compares the hand-written migration SQL with the
 * schema JSON Room exported for version 4 when the project was compiled. A mismatch would mean
 * the app crashes on first launch after updating from an older database.
 */
class MigrationSqlTest {
    private val schema: String by lazy {
        val file = File("schemas/com.aurax.imagery.data.local.database.AuraDatabase/4.json")
        assertTrue("Room did not export ${file.absolutePath}; compile first", file.exists())
        file.readText()
    }

    private fun forTable(name: String) = schema.replace("\${TABLE_NAME}", name)

    @Test fun dateAddedColumnMatchesRoom() {
        // The migration's ALTER and the new entity column must describe the same column.
        assertTrue("column missing from Room's schema", schema.contains("`dateAdded` INTEGER NOT NULL DEFAULT 0"))
        assertTrue("migration SQL changed", Migrations.MEDIA_DATE_ADDED.contains("`dateAdded` INTEGER NOT NULL DEFAULT 0"))
    }

    @Test fun bookmarksTableMatchesRoom() {
        val json = forTable("video_bookmarks")
        assertTrue("table SQL differs from Room's", json.contains(Migrations.BOOKMARKS_TABLE))
        assertTrue("index SQL differs from Room's", json.contains(Migrations.BOOKMARKS_INDEX))
    }

    @Test fun progressTableMatchesRoom() {
        val json = forTable("playback_progress")
        assertTrue("table SQL differs from Room's", json.contains(Migrations.PROGRESS_TABLE))
        assertTrue("index SQL differs from Room's", json.contains(Migrations.PROGRESS_INDEX))
    }
}
