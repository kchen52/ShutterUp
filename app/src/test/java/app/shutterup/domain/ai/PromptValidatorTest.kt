package app.shutterup.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptValidatorTest {

    private val blockedTerms = setOf(
        "weapon",
        "gun",
        "rifle",
        "knife",
        "nudity",
        "nude",
        "naked",
        "trespass",
        "stranger",
        "child",
        "children",
        "kid",
        "baby",
        "roof",
        "rooftop",
        "cliff",
        "driving",
        "drugged",
        "alcohol",
    )

    private val validator = PromptValidator(blockedTerms, DEFAULT_GEAR_TERMS)

    @Test
    fun rejectsBlocklist() {
        val result = validator.validate(
            goodPrompt(details = "Do not photograph a stranger on the street. Stay inside instead."),
            recentTitles = emptyList(),
            recentThemeLedes = emptyList(),
        )
        assertInvalid(result, "blocklist")
    }

    @Test
    fun rejectsGear() {
        val result = validator.validate(
            goodPrompt(details = "Borrow a DSLR if you can. Then frame the same scene with your phone."),
            recentTitles = emptyList(),
            recentThemeLedes = emptyList(),
        )
        assertInvalid(result, "gear")
    }

    @Test
    fun rejectsTitleDuplicate_caseAndPunctuationInsensitive() {
        val result = validator.validate(
            goodPrompt(title = "Window-Light!"),
            recentTitles = listOf("window light"),
            recentThemeLedes = emptyList(),
        )
        assertInvalid(result, "title duplicate")
    }

    @Test
    fun rejectsThemeLedeDuplicate() {
        val result = validator.validate(
            goodPrompt(
                theme = "Reflections",
                oneLiner = "Find a puddle after the storm passes.",
            ),
            recentTitles = emptyList(),
            recentThemeLedes = listOf(
                ThemeLede("reflections!", "Find a puddle after lunch today"),
            ),
        )
        assertInvalid(result, "theme lede duplicate")
    }

    @Test
    fun rejectsUrl() {
        val result = validator.validate(
            goodPrompt(details = "Look up examples at https://example.com later. Then shoot from memory."),
            recentTitles = emptyList(),
            recentThemeLedes = emptyList(),
        )
        assertInvalid(result, "url")
    }

    @Test
    fun rejectsHashtag() {
        val result = validator.validate(
            goodPrompt(oneLiner = "Hunt for #goldenhour patches on the wall."),
            recentTitles = emptyList(),
            recentThemeLedes = emptyList(),
        )
        assertInvalid(result, "hashtag")
    }

    @Test
    fun rejectsEmoji() {
        val smile = "\u263A"
        val result = validator.validate(
            goodPrompt(title = "Warm $smile Light"),
            recentTitles = emptyList(),
            recentThemeLedes = emptyList(),
        )
        assertInvalid(result, "emoji")
    }

    @Test
    fun rejectsOneSentenceDetails() {
        val result = validator.validate(
            goodPrompt(details = "Only one sentence lives here."),
            recentTitles = emptyList(),
            recentThemeLedes = emptyList(),
        )
        assertInvalid(result, "details sentence count")
    }

    @Test
    fun rejectsFiveSentenceDetails() {
        val result = validator.validate(
            goodPrompt(details = "One. Two. Three. Four. Five."),
            recentTitles = emptyList(),
            recentThemeLedes = emptyList(),
        )
        assertInvalid(result, "details sentence count")
    }

    @Test
    fun corpusOfGoodPrompts_allValid() {
        val corpus = listOf(
            goodPrompt(
                title = "Steam Maps",
                oneLiner = "Turn kitchen steam into contour lines of light.",
                details = "Wait for a kettle or hot tap to throw a plume. Side-light it and expose for the brightest edge.",
                tips = listOf("Use a dark backdrop.", "Shoot bursts as the plume shifts."),
                constraint = "Stay in the kitchen",
                theme = "Steam",
            ),
            goodPrompt(
                title = "Chair Lattice",
                oneLiner = "Let chair slats print a grid on the floor.",
                details = "Find hard light through a window. Frame only the pattern, not the furniture.",
                tips = listOf("Expose for the brightest bar."),
                constraint = null,
                theme = "Shadow",
            ),
            goodPrompt(
                title = "Sink Geometry",
                oneLiner = "Make the kitchen sink a study in ellipses.",
                details = "Shoot from directly above. Let the basin, drain, and tap become nested shapes.",
                tips = listOf("Wipe water spots first.", "Keep the frame square to the rim."),
                constraint = "No people in frame",
                theme = "Still life",
            ),
            goodPrompt(
                title = "Book Stack Dawn",
                oneLiner = "Use a stack of books as a cheap phone tripod.",
                details = "Brace the phone so a long indoor exposure stays sharp. Let window light skim the page edges.",
                tips = listOf("Timer release avoids shake."),
                constraint = "No zoom",
                theme = "Texture",
            ),
            goodPrompt(
                title = "Soap Film Sky",
                oneLiner = "Catch colour bands on a soap film by the window.",
                details = "Stretch a film across a jar mouth. Tilt until the bands run corner to corner.",
                tips = listOf("Work quickly before it pops.", "Underexpose a touch."),
                constraint = null,
                theme = "Colour",
            ),
            goodPrompt(
                title = "Negative Mug",
                oneLiner = "Photograph the space around a mug, not the mug.",
                details = "Place it against a plain wall. Fill the frame with the leftover shape of air.",
                tips = listOf("Step left until the handle reads as a silhouette."),
                constraint = "Shoot from knee height",
                theme = "Negative space",
            ),
            goodPrompt(
                title = "Fridge Glow",
                oneLiner = "Open the fridge and paint the room with its light.",
                details = "Turn other lamps off. Let labels and bottles become a small night scene.",
                tips = listOf("White-balance for the cool glow.", "Hold still on a counter edge."),
                constraint = null,
                theme = "Night indoor",
            ),
            goodPrompt(
                title = "Leading Crumbs",
                oneLiner = "Build a leading line from crumbs across a table.",
                details = "Arrange a trail toward a cup. Keep the far end slightly out of focus.",
                tips = listOf("Get low along the table plane.", "One catchlight on the cup is enough."),
                constraint = "Use only tabletop items",
                theme = "Story",
            ),
            goodPrompt(
                title = "Fogged Glass",
                oneLiner = "Draw with a fingertip on a fogged bathroom mirror.",
                details = "Let the shower steam the glass. Photograph the mark as a window onto the room behind.",
                tips = listOf("Wipe a small clear patch for the eye."),
                constraint = "No faces",
                theme = "Layers",
            ),
            goodPrompt(
                title = "Sock Landscape",
                oneLiner = "Turn a rumpled sock into rolling hills of fabric.",
                details = "Rake a desk lamp across the folds. Crop so scale becomes ambiguous.",
                tips = listOf("Move the lamp, not the sock.", "Expose for the brightest ridge."),
                constraint = null,
                theme = "Abstract",
            ),
            goodPrompt(
                title = "Tap Motion",
                oneLiner = "Freeze or smear a thin stream from the tap.",
                details = "Choose a shutter you can hold. Try one frame crisp and one that turns water into thread.",
                tips = listOf("Darken the sink so the stream pops."),
                constraint = "Stay at the sink",
                theme = "Motion",
            ),
            goodPrompt(
                title = "Spice Dust",
                oneLiner = "Tap cinnamon so it hangs in a shaft of light.",
                details = "Close blinds to a slit. Time the tap so motes cross the beam.",
                tips = listOf("A dark backdrop helps.", "Clean the counter after."),
                constraint = null,
                theme = "Light",
            ),
        )
        assertTrue("corpus must have at least 10 prompts", corpus.size >= 10)
        for (prompt in corpus) {
            val result = validator.validate(prompt, emptyList(), emptyList())
            assertEquals("expected Valid for '${prompt.title}' but was $result", ValidationResult.Valid, result)
        }
    }

    private fun goodPrompt(
        title: String = "Steam Maps",
        oneLiner: String = "Turn kitchen steam into contour lines of light.",
        details: String = "Wait for a kettle or hot tap. Side-light the plume and expose for the brightest edge.",
        tips: List<String> = listOf("Use a dark backdrop."),
        constraint: String? = "No zoom",
        theme: String = "Steam",
    ): GeneratedPrompt = GeneratedPrompt(
        title = title,
        oneLiner = oneLiner,
        details = details,
        tips = tips,
        constraint = constraint,
        theme = theme,
        source = PromptSource.ON_DEVICE_AI,
    )

    private fun assertInvalid(result: ValidationResult, token: String) {
        assertTrue("expected Invalid, was $result", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue("reason should mention $token: $reason", reason.contains(token, ignoreCase = true))
    }
}
