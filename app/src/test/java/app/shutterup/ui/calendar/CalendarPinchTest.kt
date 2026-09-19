package app.shutterup.ui.calendar

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarPinchTest {

    @Test
    fun pinchOut_movesTowardYearView() {
        val next = yearProgressAfterPinch(current = 0f, zoom = 1.5f)
        assertTrue(next > 0f)
        assertTrue(next <= 1f)
    }

    @Test
    fun pinchIn_movesTowardMonthView() {
        val next = yearProgressAfterPinch(current = 1f, zoom = 0.6f)
        assertTrue(next < 1f)
        assertTrue(next >= 0f)
    }

    @Test
    fun pinchIsClamped() {
        assertEquals(1f, yearProgressAfterPinch(0.9f, 8f), 0.0001f)
        assertEquals(0f, yearProgressAfterPinch(0.1f, 0.05f), 0.0001f)
    }

    @Test
    fun snapChoosesNearestEnd() {
        assertEquals(0f, snapYearProgress(0.49f), 0f)
        assertEquals(1f, snapYearProgress(0.5f), 0f)
        assertEquals(1f, snapYearProgress(0.8f), 0f)
    }

    @Test
    fun twoFingersPinchUnlessSwipeAlreadyWon() {
        assertTrue(pinchWinsOverSwipe(pointerCount = 2, horizontalSwipeWon = false))
        assertFalse(pinchWinsOverSwipe(pointerCount = 2, horizontalSwipeWon = true))
        assertFalse(pinchWinsOverSwipe(pointerCount = 1, horizontalSwipeWon = false))
    }

    @Test
    fun horizontalDragBeyondSlop_isASwipe() {
        assertTrue(horizontalSwipeWon(Offset(40f, 4f), touchSlop = 18f))
        assertFalse(horizontalSwipeWon(Offset(4f, 40f), touchSlop = 18f))
        assertFalse(horizontalSwipeWon(Offset(10f, 2f), touchSlop = 18f))
    }
}
