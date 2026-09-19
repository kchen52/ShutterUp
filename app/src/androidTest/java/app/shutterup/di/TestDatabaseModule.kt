package app.shutterup.di

import android.content.Context
import androidx.room.Room
import app.shutterup.data.di.DatabaseModule
import app.shutterup.data.local.DayPromptDao
import app.shutterup.data.local.EntryDao
import app.shutterup.data.local.GamificationDao
import app.shutterup.data.local.ShutterUpDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class],
)
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShutterUpDatabase =
        Room.inMemoryDatabaseBuilder(context, ShutterUpDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    fun provideDayPromptDao(db: ShutterUpDatabase): DayPromptDao = db.dayPromptDao()

    @Provides
    fun provideEntryDao(db: ShutterUpDatabase): EntryDao = db.entryDao()

    @Provides
    fun provideGamificationDao(db: ShutterUpDatabase): GamificationDao = db.gamificationDao()
}
