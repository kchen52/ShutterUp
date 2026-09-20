package app.shutterup.ui.issue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSheetLayoutTest {
    @Test
    fun columnsFallWithThePhotoCount() {
        assertEquals(1, pageSheetColumns(1))
        assertEquals(2, pageSheetColumns(2))
        assertEquals(3, pageSheetColumns(3))
        assertEquals(2, pageSheetColumns(4))
        assertEquals(3, pageSheetColumns(8))
        assertEquals(3, pageSheetColumns(15))
        assertEquals(3, pageSheetColumns(24))
        assertEquals(3, pageSheetColumns(30))
    }

    @Test
    fun onlyDenseMonthsFillTheViewport() {
        assertFalse(pageSheetFillsViewport(1))
        assertFalse(pageSheetFillsViewport(3))
        assertFalse(pageSheetFillsViewport(8))
        assertFalse(pageSheetFillsViewport(15))
        assertTrue(pageSheetFillsViewport(24))
        assertTrue(pageSheetFillsViewport(30))
    }
}
