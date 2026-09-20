package app.shutterup.share

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareCacheTest {

    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-19T10:00:00Z"), ZoneOffset.UTC)
    private lateinit var cache: ShareCache

    @Before
    fun setUp() {
        clearFileProviderPathCache()
        cache = ShareCache(RuntimeEnvironment.getApplication(), clock)
        cache.shareDir().deleteRecursively()
    }

    @Test
    fun writePng_returnsFileProviderUriWithGrantablePng() {
        val bitmap = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF336699.toInt())
        val uri = cache.writePng(bitmap)
        assertEquals("content", uri.scheme)
        assertEquals("app.shutterup.fileprovider", uri.authority)
        val file = cache.shareDir().listFiles()?.single { it.extension == "png" }
        assertNotNull(file)
        assertTrue(file!!.length() > 0L)
        assertTrue(file.readBytes().copyOfRange(0, 8).contentEquals(PNG_MAGIC))
        assertEquals("share-${clock.millis()}.png", file.name)
        val dest = File(cache.shareDir(), file.name)
        val roundTrip = ShareIntents.send(uri)
        assertEquals(uri, roundTrip.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        val exif = ExifInterface(dest.absolutePath)
        assertEquals(null, exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertEquals(null, exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertEquals(null, exif.getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun writePng_prunesOlderShareFiles() {
        val dir = cache.shareDir().also { it.mkdirs() }
        File(dir, "share-1.png").writeBytes(PNG_MAGIC)
        File(dir, "share-2.png.tmp").writeBytes(byteArrayOf(1, 2, 3))
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        cache.writePng(bitmap)
        val remaining = dir.listFiles()?.map { it.name }?.sorted()
        assertEquals(listOf("share-${clock.millis()}.png"), remaining)
    }

    @Test
    fun writePng_failureLeavesNoHalfWrittenFile() {
        val blocker = cache.shareDir()
        blocker.parentFile?.mkdirs()
        blocker.writeText("not a directory")
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val result = runCatching { cache.writePng(bitmap) }
        assertTrue(result.isFailure)
        val cacheRoot = RuntimeEnvironment.getApplication().cacheDir
        val leftovers = cacheRoot.walkTopDown().filter { file ->
            file.isFile && (file.extension == "png" || file.name.endsWith(".tmp"))
        }.toList()
        assertTrue(leftovers.isEmpty())
        assertFalse(File(blocker, "share-${clock.millis()}.png").exists())
    }

    @Test
    fun writePng_compressFailureCleansTemp() {
        cache.shareDir().mkdirs()
        cache.encoder = PngEncoder { _, _ -> false }
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val result = runCatching { cache.writePng(bitmap) }
        assertTrue(result.isFailure)
        val leftovers = cache.shareDir().listFiles()?.toList().orEmpty()
        assertTrue(leftovers.none { it.name.endsWith(".tmp") || it.extension == "png" })
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareIntentsTest {

    @Test
    fun send_grantsReadPermissionAndPutsPngStream() {
        val uri = Uri.parse("content://app.shutterup.fileprovider/share/share-1.png")
        val intent = ShareIntents.send(uri)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals(ShareIntents.MIME_PNG, intent.type)
        assertEquals(uri, intent.data)
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertNotEquals(0, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertNotNull(intent.clipData)
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
    }

    @Test
    fun chooser_wrapsSendIntent() {
        val uri = Uri.parse("content://app.shutterup.fileprovider/share/share-1.png")
        val chooser = ShareIntents.chooser(uri)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val inner = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull(inner)
        assertEquals(Intent.ACTION_SEND, inner!!.action)
        assertEquals(uri, inner.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertNotEquals(0, inner.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/**
 * Robolectric gives each test a fresh cacheDir, but [FileProvider] caches
 * path roots statically per authority. Clear it so URI resolution sees
 * this test's cache directory.
 */
private fun clearFileProviderPathCache() {
    val field = FileProvider::class.java.getDeclaredField("sCache")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    (field.get(null) as MutableMap<Any, Any>).clear()
}

private val PNG_MAGIC = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
)
