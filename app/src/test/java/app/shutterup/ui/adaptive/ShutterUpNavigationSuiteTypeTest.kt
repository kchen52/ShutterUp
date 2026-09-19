package app.shutterup.ui.adaptive

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class ShutterUpNavigationSuiteTypeTest {

    @Test
    fun compactWidthUsesBottomBar() {
        assertEquals(
            NavigationSuiteType.NavigationBar,
            shutterUpNavigationSuiteType(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND - 1),
        )
    }

    @Test
    fun mediumWidthUsesRail() {
        assertEquals(
            NavigationSuiteType.NavigationRail,
            shutterUpNavigationSuiteType(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),
        )
        assertEquals(
            NavigationSuiteType.NavigationRail,
            shutterUpNavigationSuiteType(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND - 1),
        )
    }

    @Test
    fun expandedWidthUsesDrawer() {
        assertEquals(
            NavigationSuiteType.NavigationDrawer,
            shutterUpNavigationSuiteType(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND),
        )
    }
}
