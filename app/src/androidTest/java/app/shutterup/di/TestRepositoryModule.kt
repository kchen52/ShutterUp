package app.shutterup.di

import app.shutterup.data.di.RepositoryModule
import app.shutterup.data.repository.RoomDayPromptRepository
import app.shutterup.data.repository.RoomEntryRepository
import app.shutterup.data.repository.RoomGamificationRepository
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.testutil.InMemoryPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class],
)
abstract class TestRepositoryModule {
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
    abstract fun bindPreferencesRepository(impl: InMemoryPreferencesRepository): PreferencesRepository
}
