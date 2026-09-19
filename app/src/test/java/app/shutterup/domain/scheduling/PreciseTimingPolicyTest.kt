package app.shutterup.domain.scheduling

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreciseTimingPolicyTest {
    @Test
    fun exactOnlyWhenEnabledAndPermitted() {
        assertTrue(PreciseTimingPolicy.useExactAlarm(true, true))
        assertFalse(PreciseTimingPolicy.useExactAlarm(true, false))
        assertFalse(PreciseTimingPolicy.useExactAlarm(false, true))
        assertFalse(PreciseTimingPolicy.useExactAlarm(false, false))
    }
}
