package app.shutterup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DayPromptEntity::class,
        SupersededPromptEntity::class,
        EntryEntity::class,
        AchievementEntity::class,
        StreakStateEntity::class,
        LibraryUsageEntity::class,
        SeriesEntity::class,
        MonthlyIssueEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(ShutterUpConverters::class)
abstract class ShutterUpDatabase : RoomDatabase() {
    abstract fun dayPromptDao(): DayPromptDao
    abstract fun entryDao(): EntryDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun seriesDao(): SeriesDao
    abstract fun monthlyIssueDao(): MonthlyIssueDao
}
