package app.shutterup.capture

import androidx.media3.common.util.UnstableApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@UnstableApi
abstract class CaptureModule {
    @Binds
    @Singleton
    abstract fun bindVideoClipExporter(impl: TransformerVideoClipExporter): VideoClipExporter
}
