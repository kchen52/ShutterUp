package app.shutterup.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApertureCheckGeometryTest {

    @Test
    fun openFrameIsFullApertureWithNoCheck() {
        val frame = apertureCheckFrame(0f)
        assertEquals(0f, frame.closeAmount, 0.001f)
        assertEquals(0f, frame.checkReveal, 0.001f)
        assertEquals(1f, frame.apertureAlpha, 0.001f)
        assertEquals(ApertureCheckOpenInnerRadius, frame.innerRadius, 0.001f)
        assertEquals(ApertureCheckOpenTwistDeg, frame.innerTwistDeg, 0.001f)
        assertEquals(0f, frame.irisRotationDeg, 0.001f)
    }

    @Test
    fun finalFrameIsCheckOnly() {
        val frame = apertureCheckFrame(1f)
        assertEquals(1f, frame.closeAmount, 0.001f)
        assertEquals(1f, frame.checkReveal, 0.001f)
        assertEquals(0f, frame.apertureAlpha, 0.001f)
        assertEquals(ApertureCheckClosedInnerRadius, frame.innerRadius, 0.001f)
    }

    @Test
    fun bladesAreClosedBeforeTheCheckFullyResolves() {
        val closing = apertureCheckFrame(ApertureCheckCloseEnd)
        assertEquals(1f, closing.closeAmount, 0.001f)
        assertTrue(closing.checkReveal > 0f)
        assertTrue(closing.checkReveal < 1f)
        assertTrue(closing.apertureAlpha > 0f)
        assertTrue(closing.apertureAlpha < 1f)
    }

    @Test
    fun progressIsClamped() {
        assertEquals(apertureCheckFrame(0f), apertureCheckFrame(-1f))
        assertEquals(apertureCheckFrame(1f), apertureCheckFrame(2f))
    }

    @Test
    fun animationOnlyWhenPlayOnceWithoutExplicitProgress() {
        assertTrue(apertureCheckAnimates(progress = null, playOnce = true, inspect = false))
        assertFalse(apertureCheckAnimates(progress = 1f, playOnce = true, inspect = false))
        assertFalse(apertureCheckAnimates(progress = 0f, playOnce = true, inspect = false))
        assertFalse(apertureCheckAnimates(progress = null, playOnce = false, inspect = false))
        assertFalse(apertureCheckAnimates(progress = null, playOnce = true, inspect = true))
    }

    @Test
    fun vectorPathDataUsesSharedPoints() {
        assertEquals("M6,12.5 L10.25,16.75 L18.5,7.5", apertureCheckVectorPathData())
    }
}
