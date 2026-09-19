package app.shutterup.capture

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Exports a clipped video. Failures must block save. */
interface VideoClipExporter {
    suspend fun clip(input: Uri, output: File, startMs: Long, endMs: Long): Result<File>
}

@Singleton
@UnstableApi
class TransformerVideoClipExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoClipExporter {
    override suspend fun clip(
        input: Uri,
        output: File,
        startMs: Long,
        endMs: Long,
    ): Result<File> = suspendCancellableCoroutine { cont ->
        output.parentFile?.mkdirs()
        if (output.exists()) output.delete()
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(endMs)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(input)
            .setClippingConfiguration(clipping)
            .build()
        val edited = EditedMediaItem.Builder(mediaItem).build()
        val transformer = Transformer.Builder(context)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .build()
        val listener = object : Transformer.Listener {
            override fun onCompleted(
                composition: androidx.media3.transformer.Composition,
                exportResult: ExportResult,
            ) {
                if (cont.isActive) cont.resume(Result.success(output))
            }

            override fun onError(
                composition: androidx.media3.transformer.Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                output.delete()
                if (cont.isActive) cont.resume(Result.failure(exportException))
            }
        }
        transformer.addListener(listener)
        cont.invokeOnCancellation { transformer.cancel() }
        try {
            transformer.start(edited, output.absolutePath)
        } catch (t: Throwable) {
            output.delete()
            if (cont.isActive) cont.resume(Result.failure(t))
        }
    }
}
