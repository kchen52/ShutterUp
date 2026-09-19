package app.shutterup.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import app.shutterup.domain.capture.CaptureLimits

/** Still capture into a [FileProvider] URI (no MediaStore output). */
class TakePrivatePicture : ActivityResultContracts.TakePicture()

/**
 * Video capture into a [FileProvider] URI with a 15 second ceiling.
 * Quality fallback for in-app recording lives on [CameraXCaptureConfig].
 */
class CapturePrivateVideo : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .putExtra(MediaStore.EXTRA_DURATION_LIMIT, CaptureLimits.MAX_RECORD_SECONDS)
            .putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == android.app.Activity.RESULT_OK
    }
}
