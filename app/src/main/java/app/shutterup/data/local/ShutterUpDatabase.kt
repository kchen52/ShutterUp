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
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(ShutterUpConverters::class)
abstract class ShutterUpDatabase : RoomDatabase() {
    abstract fun dayPromptDao(): DayPromptDao
    abstract fun entryDao(): EntryDao
    abstract fun gamificationDao(): GamificationDao
}
