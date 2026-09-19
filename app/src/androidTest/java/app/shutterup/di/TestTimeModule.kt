package app.shutterup.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class],
)
object TestTimeModule {
    val ZONE: ZoneId = ZoneId.of("UTC")
    val NOW: Instant = Instant.parse("2026-09-19T15:00:00Z")
    val TODAY: LocalDate = LocalDate.of(2026, 9, 19)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.fixed(NOW, ZONE)

    @Provides
    @Singleton
    fun provideZoneId(): ZoneId = ZONE
}
