package app.shutterup.data.local

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationsTest {

    @Test
    fun migrate2to3_addsSeriesTableAndNullableColumns() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("mig-2-3.db")
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("mig-2-3.db")
            .callback(
                object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `day_prompts` (
                                `date` INTEGER NOT NULL,
                                `title` TEXT NOT NULL,
                                `oneLiner` TEXT NOT NULL,
                                `details` TEXT NOT NULL,
                                `constraint` TEXT,
                                `theme` TEXT NOT NULL,
                                `tips` TEXT NOT NULL,
                                `source` TEXT NOT NULL,
                                `libraryId` TEXT,
                                `modelName` TEXT,
                                `generatedAt` INTEGER NOT NULL,
                                `status` TEXT NOT NULL,
                                `frozen` INTEGER NOT NULL,
                                `rerollUsed` INTEGER NOT NULL,
                                PRIMARY KEY(`date`)
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `superseded_prompts` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `date` INTEGER NOT NULL,
                                `title` TEXT NOT NULL,
                                `theme` TEXT NOT NULL,
                                `generatedAt` INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `entries` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `date` INTEGER NOT NULL,
                                `mediaUri` TEXT NOT NULL,
                                `thumbPath` TEXT NOT NULL,
                                `capturedAt` INTEGER NOT NULL,
                                `width` INTEGER NOT NULL,
                                `height` INTEGER NOT NULL,
                                `note` TEXT,
                                `importedFromGallery` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                `mediaKind` TEXT NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_date` ON `entries` (`date`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `achievements` (
                                `id` TEXT NOT NULL,
                                `unlockedAt` INTEGER NOT NULL,
                                `unlockedOnDate` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `streak_state` (
                                `id` INTEGER NOT NULL,
                                `current` INTEGER NOT NULL,
                                `longest` INTEGER NOT NULL,
                                `freezes` INTEGER NOT NULL,
                                `lastProcessedDate` INTEGER,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `library_usage` (
                                `libraryId` TEXT NOT NULL,
                                `usedOnDate` INTEGER NOT NULL,
                                PRIMARY KEY(`libraryId`)
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqlite = helper.writableDatabase
        val epoch = LocalDate.of(2026, 9, 19).toEpochDay()
        sqlite.execSQL(
            """
            INSERT INTO day_prompts (
                date, title, oneLiner, details, `constraint`, theme, tips, source,
                libraryId, modelName, generatedAt, status, frozen, rerollUsed
            ) VALUES (
                $epoch, 'Window light', 'Find a slice of window light.', 'Details here.',
                NULL, 'Light', 'tip', 'LIBRARY', NULL, NULL, 0, 'PENDING', 0, 0
            )
            """.trimIndent(),
        )
        MIGRATION_2_3.migrate(sqlite)
        val columns = mutableListOf<String>()
        sqlite.query("PRAGMA table_info(day_prompts)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex)
            }
        }
        assertTrue(columns.contains("seriesId"))
        assertTrue(columns.contains("seriesIndex"))
        sqlite.query("SELECT title, seriesId, seriesIndex FROM day_prompts").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Window light", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }
        sqlite.query("SELECT name FROM sqlite_master WHERE type='table' AND name='series'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        sqlite.close()
    }

    @Test
    fun migrate3to4_addsMonthlyIssuesTable() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("mig-3-4.db")
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("mig-3-4.db")
            .callback(
                object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `day_prompts` (
                                `date` INTEGER NOT NULL,
                                `title` TEXT NOT NULL,
                                `oneLiner` TEXT NOT NULL,
                                `details` TEXT NOT NULL,
                                `constraint` TEXT,
                                `theme` TEXT NOT NULL,
                                `tips` TEXT NOT NULL,
                                `source` TEXT NOT NULL,
                                `libraryId` TEXT,
                                `modelName` TEXT,
                                `generatedAt` INTEGER NOT NULL,
                                `status` TEXT NOT NULL,
                                `frozen` INTEGER NOT NULL,
                                `rerollUsed` INTEGER NOT NULL,
                                `seriesId` INTEGER,
                                `seriesIndex` INTEGER,
                                PRIMARY KEY(`date`)
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS `index_day_prompts_seriesId` ON `day_prompts` (`seriesId`)",
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `series` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `title` TEXT NOT NULL,
                                `startDate` INTEGER NOT NULL,
                                `endDate` INTEGER NOT NULL,
                                `theme` TEXT NOT NULL,
                                `source` TEXT NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `entries` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `date` INTEGER NOT NULL,
                                `mediaUri` TEXT NOT NULL,
                                `thumbPath` TEXT NOT NULL,
                                `capturedAt` INTEGER NOT NULL,
                                `width` INTEGER NOT NULL,
                                `height` INTEGER NOT NULL,
                                `note` TEXT,
                                `importedFromGallery` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                `mediaKind` TEXT NOT NULL
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqlite = helper.writableDatabase
        MIGRATION_3_4.migrate(sqlite)
        sqlite.query("SELECT name FROM sqlite_master WHERE type='table' AND name='monthly_issues'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        val columns = mutableListOf<String>()
        sqlite.query("PRAGMA table_info(monthly_issues)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex)
            }
        }
        assertTrue(columns.contains("yearMonth"))
        assertTrue(columns.contains("headline"))
        assertTrue(columns.contains("dismissedFromFeed"))
        sqlite.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_monthly_issues_yearMonth'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        sqlite.close()
    }
}
