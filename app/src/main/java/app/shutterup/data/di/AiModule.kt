package app.shutterup.data.di

import app.shutterup.data.ai.AssetLibraryPromptSource
import app.shutterup.data.ai.BlocklistProvider
import app.shutterup.data.ai.NanoPromptGenerator
import app.shutterup.domain.ai.LibraryPromptGenerator
import app.shutterup.domain.ai.LibraryPromptSource
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.repository.GamificationRepository
import com.google.mlkit.genai.prompt.GenerativeModel
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
     * Real Nano primary (SPEC §7.2). When Nano is unavailable or fails, the use case
     * falls back to the library, so a prompt is always available. Debug builds can
     * select FakePromptGenerator instead (Settings → Debug, later milestone).
     */
    @Provides
    @Named("primaryGenerator")
    fun providePrimaryGenerator(nano: NanoPromptGenerator): PromptGenerator = nano

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel = NanoPromptGenerator.defaultClient()
}
