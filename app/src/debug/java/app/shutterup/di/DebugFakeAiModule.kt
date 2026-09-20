package app.shutterup.di

import app.shutterup.data.ai.FakePromptGenerator
import app.shutterup.domain.ai.PromptGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object DebugFakeAiModule {
    @Provides
    @IntoSet
    @FakeAiBinding
    fun fakeAi(fake: FakePromptGenerator): PromptGenerator = fake
}
