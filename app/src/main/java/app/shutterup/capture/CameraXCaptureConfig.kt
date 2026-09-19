package app.shutterup.capture

import androidx.camera.core.ImageCapture
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder

/**
 * L0/L1 encoding for in-app capture: JPEG at maximum quality; video quality
 * falls through UHD → FHD → HD → SD. Output is always a private file URI,
 * never [android.provider.MediaStore.OutputOptions].
 */
object CameraXCaptureConfig {
    /** Ordered fallback used by [Recorder] / [androidx.camera.video.VideoCapture]. */
    fun qualitySelector(): QualitySelector = QualitySelector.fromOrderedList(
        listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
    )

    /** JPEG stills at maximum encoder quality. */
    fun imageCapture(): ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setJpegQuality(100)
        .build()

    /** [Recorder] bound to [qualitySelector]. */
    fun recorder(): Recorder = Recorder.Builder()
        .setQualitySelector(qualitySelector())
        .build()
}
