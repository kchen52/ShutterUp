package app.shutterup.testutil

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File

object TestJpegs {
    fun write(file: File, width: Int = 48, height: Int = 32, color: Int = Color.rgb(40, 90, 140)) {
        file.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        file.outputStream().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out))
        }
    }
}
