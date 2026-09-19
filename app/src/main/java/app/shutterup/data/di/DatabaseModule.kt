package app.shutterup.data.di

import android.content.Context
import androidx.room.Room
import app.shutterup.data.local.DayPromptDao
import app.shutterup.data.local.EntryDao
import app.shutterup.data.local.GamificationDao
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
        Room.databaseBuilder(ctx, ShutterUpDatabase::class.java, "shutterup.db").build()

    @Provides
    fun provideDayPromptDao(db: ShutterUpDatabase): DayPromptDao = db.dayPromptDao()

    @Provides
    fun provideEntryDao(db: ShutterUpDatabase): EntryDao = db.entryDao()

    @Provides
    fun provideGamificationDao(db: ShutterUpDatabase): GamificationDao = db.gamificationDao()
}
