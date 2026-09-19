package app.shutterup.data.di

import app.shutterup.data.ai.AssetLibraryPromptSource
import app.shutterup.data.ai.BlocklistProvider
import app.shutterup.domain.ai.LibraryPromptGenerator
import app.shutterup.domain.ai.LibraryPromptSource
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.repository.GamificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    @Named("blockedTerms")
    fun provideBlockedTerms(provider: BlocklistProvider): Set<String> = provider.blockedTerms()

    @Provides
    @Singleton
    @Named("gearTerms")
    fun provideGearTerms(provider: BlocklistProvider): Set<String> = provider.gearTerms()

    @Provides
    fun provideLibraryPromptSource(impl: AssetLibraryPromptSource): LibraryPromptSource = impl

    @Provides
    fun provideLibraryPromptGenerator(
        source: LibraryPromptSource,
        gamification: GamificationRepository,
        clock: Clock,
    ): LibraryPromptGenerator = LibraryPromptGenerator(source, gamification, clock, Random.Default)

    /**
     * Temporary: Milestone 4 rewires this binding to NanoPromptGenerator.
     * Until then the library generator is the primary so a prompt is always available.
     */
    @Provides
    @Named("primaryGenerator")
    fun providePrimaryGenerator(library: LibraryPromptGenerator): PromptGenerator = library
}
