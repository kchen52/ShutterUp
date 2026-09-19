package app.shutterup.capture

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CameraXCaptureConfigTest {
    @Test
    fun stillsUseMaximumJpegQuality() {
        assertEquals(100, CameraXCaptureConfig.imageCapture().jpegQuality)
    }
}
