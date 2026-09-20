package app.shutterup.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 date-PK entries → v2 auto-id rows with [app.shutterup.domain.model.MediaKind]. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `entries_new` (
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
        db.execSQL(
            """
            INSERT INTO `entries_new` (
                `date`, `mediaUri`, `thumbPath`, `capturedAt`, `width`, `height`,
                `note`, `importedFromGallery`, `createdAt`, `mediaKind`
            )
            SELECT `date`, `mediaUri`, `thumbPath`, `capturedAt`, `width`, `height`,
                `note`, `importedFromGallery`, `createdAt`, 'PHOTO'
            FROM `entries`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `entries`")
        db.execSQL("ALTER TABLE `entries_new` RENAME TO `entries`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_date` ON `entries` (`date`)")
    }
}

/** v2 day prompts → v3 series table plus nullable series columns on day_prompts. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
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
        db.execSQL("ALTER TABLE `day_prompts` ADD COLUMN `seriesId` INTEGER")
        db.execSQL("ALTER TABLE `day_prompts` ADD COLUMN `seriesIndex` INTEGER")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_day_prompts_seriesId` ON `day_prompts` (`seriesId`)",
        )
    }
}

/** v3 series → v4 monthly issues table. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `monthly_issues` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `yearMonth` TEXT NOT NULL,
                `startDate` INTEGER NOT NULL,
                `endDate` INTEGER NOT NULL,
                `completedDayCount` INTEGER NOT NULL,
                `headline` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `dominantTheme` TEXT NOT NULL,
                `loudestThemes` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `generatedAt` INTEGER NOT NULL,
                `dismissedFromFeed` INTEGER NOT NULL,
                `modelName` TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_monthly_issues_yearMonth` ON `monthly_issues` (`yearMonth`)",
        )
    }
}
