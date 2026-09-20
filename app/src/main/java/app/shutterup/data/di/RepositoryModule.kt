package app.shutterup.data.di

import app.shutterup.data.prefs.PreferencesDataStore
import app.shutterup.data.repository.RoomDayPromptRepository
import app.shutterup.data.repository.RoomEntryRepository
import app.shutterup.data.repository.RoomGamificationRepository
import app.shutterup.data.repository.RoomMonthlyIssueRepository
import app.shutterup.data.repository.RoomSeriesRepository
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.MonthlyIssueRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.repository.SeriesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDayPromptRepository(impl: RoomDayPromptRepository): DayPromptRepository

    @Binds
    @Singleton
    abstract fun bindEntryRepository(impl: RoomEntryRepository): EntryRepository

    @Binds
    @Singleton
    abstract fun bindGamificationRepository(impl: RoomGamificationRepository): GamificationRepository

    @Binds
    @Singleton
    abstract fun bindSeriesRepository(impl: RoomSeriesRepository): SeriesRepository

    @Binds
    @Singleton
    abstract fun bindMonthlyIssueRepository(impl: RoomMonthlyIssueRepository): MonthlyIssueRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesDataStore): PreferencesRepository
}
