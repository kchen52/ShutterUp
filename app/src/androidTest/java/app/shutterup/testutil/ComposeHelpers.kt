package app.shutterup.testutil

import android.content.Intent
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.shutterup.MainActivity
import app.shutterup.ui.onboarding.OnboardingCopy

const val UI_TIMEOUT_MS = 15_000L

fun ComposeTestRule.waitUntilText(
    text: String,
    timeoutMillis: Long = UI_TIMEOUT_MS,
    substring: Boolean = false,
    useUnmergedTree: Boolean = false,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text, substring = substring, useUnmergedTree = useUnmergedTree)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

fun ComposeTestRule.waitUntilContentDescription(
    label: String,
    timeoutMillis: Long = UI_TIMEOUT_MS,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeTestRule.onFirstNodeWithText(
    text: String,
    substring: Boolean = false,
): SemanticsNodeInteraction = onAllNodesWithText(text, substring = substring).onFirst()

fun ComposeTestRule.clickFirstText(text: String, substring: Boolean = false) {
    waitUntilText(text, substring = substring)
    onFirstNodeWithText(text, substring = substring).performClick()
}

fun ComposeTestRule.openPromptDetail() {
    waitUntilText(SeedData.TODAY_TITLE)
    val details = onAllNodesWithText("Details →").fetchSemanticsNodes()
    if (details.isNotEmpty()) {
        onFirstNodeWithText("Details →").performClick()
        waitUntilText("Reroll")
    }
}

fun ComposeTestRule.clickShoot() {
    waitUntilText("Shoot")
    onFirstNodeWithText("Shoot").performClick()
}

fun launchMainActivity(intent: Intent? = null): ActivityScenario<MainActivity> {
    val launch = intent ?: Intent(
        ApplicationProvider.getApplicationContext(),
        MainActivity::class.java,
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launch.setClass(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
    return ActivityScenario.launch(launch)
}

fun ComposeTestRule.completeOnboarding() {
    waitUntilText(OnboardingCopy.WORDMARK)
    clickFirstText(OnboardingCopy.NEXT)
    waitUntilText(OnboardingCopy.NOTIFICATIONS_HEADLINE)
    clickFirstText(OnboardingCopy.NEXT)
    waitUntilText(OnboardingCopy.PREFS_HEADLINE)
    waitUntilText(OnboardingCopy.GET_FIRST_PROMPT)
    clickFirstText(OnboardingCopy.GET_FIRST_PROMPT)
}
