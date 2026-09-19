package app.shutterup.ui.badges

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class BadgeEmblemCacheTest {

    private val density = Density(2f)
    private val colors = BadgeEmblemColors(
        family = Color(0xFF6750A4),
        ringColor = Color(0xFFCAC4D0),
        symbolColor = Color(0xFF6750A4),
        discColor = Color(0xFFE8DEF8),
        surfaceColor = Color(0xFFFFFBFE),
    )

    @After
    fun tearDown() {
        clearBadgeEmblemBitmapCache()
    }

    @Test
    fun rasterizeMatchesRequestedPixelSize() {
        val image = rasterizeBadgeEmblem(
            pixelSize = 96,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            spec = BadgeCatalog.getValue(BadgeIds.STREAK_7),
            unlocked = false,
            colors = colors,
            blurPx = 8f,
        )
        assertEquals(96, image.width)
        assertEquals(96, image.height)
    }

    @Test
    fun cacheReusesTheSameBitmapForTheSameKey() {
        val first = cached(badgeId = BadgeIds.STREAK_30, unlocked = false)
        val second = cached(badgeId = BadgeIds.STREAK_30, unlocked = false)
        assertSame(first, second)
    }

    @Test
    fun lockedAndUnlockedAreCachedSeparately() {
        val locked = cached(badgeId = BadgeIds.FIRST_LIGHT, unlocked = false)
        val unlocked = cached(badgeId = BadgeIds.FIRST_LIGHT, unlocked = true)
        assertNotEquals(locked, unlocked)
    }

    private fun cached(
        badgeId: String,
        unlocked: Boolean,
    ) = badgeEmblemBitmap(
        badgeId = badgeId,
        unlocked = unlocked,
        pixelSize = 96,
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        spec = BadgeCatalog[badgeId],
        colors = colors,
        blurPx = 8f,
    )
}
