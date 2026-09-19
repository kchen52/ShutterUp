package app.shutterup.di

import app.shutterup.data.ai.AssetLibraryPromptSource
import app.shutterup.data.ai.BlocklistProvider
import app.shutterup.data.ai.NanoPromptGenerator
import app.shutterup.data.di.AiModule
import app.shutterup.domain.ai.LibraryPromptGenerator
import app.shutterup.domain.ai.LibraryPromptSource
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.testutil.InstrumentationPromptGenerator
import com.google.mlkit.genai.prompt.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton
import kotlin.random.Random

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AiModule::class],
)
object TestAiModule {
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

    @Provides
    @Singleton
    @Named("primaryGenerator")
    fun providePrimaryGenerator(fake: InstrumentationPromptGenerator): PromptGenerator = fake

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel = NanoPromptGenerator.defaultClient()
}
