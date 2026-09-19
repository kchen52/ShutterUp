package app.shutterup.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PromptDetailCopyTest {
    @Test
    fun galleryAndDateCopyMatchDesign() {
        assertEquals(
            "Camera didn't return a photo. You can pick one you took today instead.",
            PromptDetailViewModel.COPY_GALLERY_CARD,
        )
        assertEquals(
            "That one's from another day — only today's photos count.",
            PromptDetailViewModel.COPY_NOT_TODAY,
        )
        assertEquals("Choose from Gallery", PromptDetailViewModel.COPY_CHOOSE_GALLERY)
        assertEquals("Today's capture isn't completed.", PromptDetailViewModel.COPY_STORAGE_BODY)
        assertEquals("Try again", PromptDetailViewModel.COPY_TRY_AGAIN)
        assertFalse(PromptDetailViewModel.COPY_NOT_TODAY.contains("Oops"))
    }
}
