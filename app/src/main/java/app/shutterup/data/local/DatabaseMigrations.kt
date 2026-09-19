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
