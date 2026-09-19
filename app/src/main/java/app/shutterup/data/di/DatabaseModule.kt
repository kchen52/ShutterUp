package app.shutterup.data.di

import android.content.Context
import androidx.room.Room
import app.shutterup.data.local.DayPromptDao
import app.shutterup.data.local.EntryDao
import app.shutterup.data.local.GamificationDao
import app.shutterup.data.local.MIGRATION_1_2
import app.shutterup.data.local.MIGRATION_2_3
import app.shutterup.data.local.MIGRATION_3_4
import app.shutterup.data.local.MonthlyIssueDao
import app.shutterup.data.local.SeriesDao
import app.shutterup.data.local.ShutterUpDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): ShutterUpDatabase =
        Room.databaseBuilder(ctx, ShutterUpDatabase::class.java, "shutterup.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun provideDayPromptDao(db: ShutterUpDatabase): DayPromptDao = db.dayPromptDao()

    @Provides
    fun provideEntryDao(db: ShutterUpDatabase): EntryDao = db.entryDao()

    @Provides
    fun provideGamificationDao(db: ShutterUpDatabase): GamificationDao = db.gamificationDao()

    @Provides
    fun provideSeriesDao(db: ShutterUpDatabase): SeriesDao = db.seriesDao()

    @Provides
    fun provideMonthlyIssueDao(db: ShutterUpDatabase): MonthlyIssueDao = db.monthlyIssueDao()
}
