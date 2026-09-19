package app.shutterup.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptParserTest {

    private val validObjectJson = """
        {
          "title": "Steam Maps",
          "oneLiner": "Turn kitchen steam into contour lines of light.",
          "details": "Wait for a kettle or hot tap. Side-light the plume and expose for the brightest edge.",
          "tips": ["Use a dark backdrop.", "Shoot bursts as the plume shifts."],
          "constraint": "No zoom",
          "theme": "Steam"
        }
    """.trimIndent()

    @Test
    fun validFullJson_succeedsWithAllFields() {
        val result = PromptParser.parse(validObjectJson, PromptSource.ON_DEVICE_AI)
        assertTrue(result.isSuccess)
        val prompt = result.getOrThrow()
        assertEquals("Steam Maps", prompt.title)
        assertEquals("Turn kitchen steam into contour lines of light.", prompt.oneLiner)
        assertEquals(
            "Wait for a kettle or hot tap. Side-light the plume and expose for the brightest edge.",
            prompt.details,
        )
        assertEquals(listOf("Use a dark backdrop.", "Shoot bursts as the plume shifts."), prompt.tips)
        assertEquals("No zoom", prompt.constraint)
        assertEquals("Steam", prompt.theme)
        assertEquals(PromptSource.ON_DEVICE_AI, prompt.source)
    }

    @Test
    fun missingField_fails() {
        val json = """
            {
              "title": "Steam Maps",
              "oneLiner": "Turn kitchen steam into contour lines of light.",
              "details": "Wait for a kettle or hot tap. Side-light the plume.",
              "tips": ["Use a dark backdrop."],
              "constraint": "No zoom"
            }
        """.trimIndent()
        val result = PromptParser.parse(json, PromptSource.LIBRARY)
        assertFailureNames(result, "theme")
    }

    @Test
    fun overLengthTitle_fails() {
        val title = "x".repeat(41)
        val json = """
            {
              "title": "$title",
              "oneLiner": "Turn kitchen steam into contour lines of light.",
              "details": "Wait for a kettle or hot tap. Side-light the plume.",
              "tips": ["Use a dark backdrop."],
              "theme": "Steam"
            }
        """.trimIndent()
        val result = PromptParser.parse(json, PromptSource.ON_DEVICE_AI)
        assertFailureNames(result, "title")
    }

    @Test
    fun extraUnknownFields_succeed() {
        val json = """
            {
              "title": "Steam Maps",
              "oneLiner": "Turn kitchen steam into contour lines of light.",
              "details": "Wait for a kettle or hot tap. Side-light the plume.",
              "tips": ["Use a dark backdrop."],
              "theme": "Steam",
              "constraint": null,
              "unused": 1.5e-2,
              "flag": true,
              "nested": {"a": [false, null, "ok"]}
            }
        """.trimIndent()
        val result = PromptParser.parse(json, PromptSource.LIBRARY)
        assertTrue(result.isSuccess)
        val prompt = result.getOrThrow()
        assertEquals(null, prompt.constraint)
        assertEquals(PromptSource.LIBRARY, prompt.source)
    }

    @Test
    fun garbageInputs_fail() {
        for (garbage in listOf("hello", "[1,2", "")) {
            val result = PromptParser.parse(garbage, PromptSource.ON_DEVICE_AI)
            assertTrue("expected failure for [$garbage]", result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun parseLibrary_twoItemArray_succeeds() {
        val json = """
            [
              {
                "id": "lib-steam",
                "title": "Steam Maps",
                "oneLiner": "Turn kitchen steam into contour lines of light.",
                "details": "Wait for a kettle or hot tap. Side-light the plume.",
                "tips": ["Use a dark backdrop."],
                "constraint": "Stay in the kitchen",
                "theme": "Steam",
                "tags": ["indoor", "light"]
              },
              {
                "id": "lib-shadow",
                "title": "Chair Lattice",
                "oneLiner": "Let chair slats print a grid on the floor.",
                "details": "Find hard light through a window. Frame only the pattern, not the furniture.",
                "tips": ["Expose for the brightest bar.", "Crop tight."],
                "theme": "Shadow",
                "tags": []
              }
            ]
        """.trimIndent()
        val result = PromptParser.parseLibrary(json)
        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals(2, items.size)
        assertEquals("lib-steam", items[0].id)
        assertEquals(listOf("indoor", "light"), items[0].tags)
        assertEquals("Stay in the kitchen", items[0].constraint)
        assertEquals("lib-shadow", items[1].id)
        assertEquals(emptyList<String>(), items[1].tags)
        assertEquals(null, items[1].constraint)
        val generated = items[0].toGeneratedPrompt()
        assertEquals(PromptSource.LIBRARY, generated.source)
        assertEquals(items[0].title, generated.title)
    }

    @Test
    fun parseLibrary_badItem_messageContainsIndex() {
        val json = """
            [
              {
                "id": "lib-ok",
                "title": "Steam Maps",
                "oneLiner": "Turn kitchen steam into contour lines of light.",
                "details": "Wait for a kettle or hot tap. Side-light the plume.",
                "tips": ["Use a dark backdrop."],
                "theme": "Steam",
                "tags": ["indoor"]
              },
              {
                "id": "",
                "title": "Broken",
                "oneLiner": "This second item is missing a usable id.",
                "details": "Still otherwise shaped like a prompt. Tips exist.",
                "tips": ["A tip."],
                "theme": "Broken",
                "tags": []
              }
            ]
        """.trimIndent()
        val result = PromptParser.parseLibrary(json)
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue("message should name index 1: $message", message.contains("1"))
    }

    private fun assertFailureNames(result: Result<*>, token: String) {
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        val message = error?.message ?: ""
        assertTrue("message should mention $token: $message", message.contains(token))
    }
}
