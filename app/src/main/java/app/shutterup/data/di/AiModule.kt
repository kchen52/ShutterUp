package app.shutterup.data.di

import app.shutterup.data.ai.AssetLibraryPromptSource
import app.shutterup.data.ai.BlocklistProvider
import app.shutterup.di.FakeAiBinding
import app.shutterup.di.SwitchingPromptGenerator
import app.shutterup.domain.ai.LibraryPromptGenerator
import app.shutterup.domain.ai.LibraryPromptSource
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
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
     * Library is the runtime primary (SPEC §7.2). Debug builds can swap in the
     * fake generator via FakeAiBinding and the Settings toggle. Nano is kept
     * in the tree for a later theme-focus revisit and is never requested here.
     */
    @Provides
    @ElementsIntoSet
    @FakeAiBinding
    fun noFakeAi(): Set<@JvmSuppressWildcards PromptGenerator> = emptySet()

    @Provides
    @Singleton
    @Named("primaryGenerator")
    fun providePrimaryGenerator(
        library: LibraryPromptGenerator,
        @FakeAiBinding fakes: Set<@JvmSuppressWildcards PromptGenerator>,
        prefs: PreferencesRepository,
    ): PromptGenerator = SwitchingPromptGenerator(
        onDevice = library,
        fake = fakes.firstOrNull(),
        preferences = prefs,
    )
}
