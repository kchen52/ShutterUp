package app.shutterup

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Privacy-guarantee guard (SPEC §11, §15.1): the merged manifest must never
 * contain INTERNET, READ_MEDIA_IMAGES, or USE_EXACT_ALARM.
 */
class ManifestGuardTest {

    private val forbiddenPermissions = listOf(
        "android.permission.INTERNET",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.USE_EXACT_ALARM",
    )

    @Test
    fun mergedManifest_declaresNoForbiddenPermissions() {
        val manifest = File(
            System.getProperty("shutterup.mergedManifest")
                ?: error("shutterup.mergedManifest system property not set; check app/build.gradle.kts."),
        )
        assertTrue(
            "Merged manifest missing at ${manifest.path}; run :app:processDebugMainManifest first.",
            manifest.isFile,
        )
        val declared = declaredPermissions(manifest)
        for (forbidden in forbiddenPermissions) {
            assertTrue(
                "$forbidden must not be declared in the merged manifest.",
                forbidden !in declared,
            )
        }
    }

    @Test
    fun sourceManifest_declaresNoForbiddenPermissions() {
        val source = File(
            System.getProperty("shutterup.sourceManifest")
                ?: "src/main/AndroidManifest.xml",
        )
        assertTrue("Source manifest missing at ${source.path}.", source.isFile)
        val declared = declaredPermissions(source)
        for (forbidden in forbiddenPermissions) {
            assertTrue(
                "$forbidden must not be declared in the source manifest.",
                forbidden !in declared,
            )
        }
    }

    private fun declaredPermissions(manifest: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(manifest)
        val nodes = doc.getElementsByTagName("uses-permission")
        return (0 until nodes.length)
            .map { nodes.item(it) }
            // A tools:node="remove" entry strips a library permission; it grants nothing.
            .filter { it.attributes.getNamedItem("tools:node")?.nodeValue != "remove" }
            .map { it.attributes.getNamedItem("android:name")?.nodeValue }
            .filterNotNull()
            .toSet()
    }
}
