package app.shutterup.data.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefCountedLeaseTest {
    @Test
    fun use_opensThenCloses() = runBlocking {
        val resource = CountingResource()
        val lease = resource.lease()
        lease.use { leased ->
            assertEquals(resource, leased)
            assertEquals(1, resource.opens)
            assertEquals(0, resource.closes)
        }
        assertEquals(1, resource.opens)
        assertEquals(1, resource.closes)
    }

    @Test
    fun nestedHold_keepsResourceUntilOuterRelease() = runBlocking {
        val resource = CountingResource()
        val lease = resource.lease()
        lease.hold()
        lease.use {
            assertEquals(1, resource.opens)
            assertEquals(0, resource.closes)
        }
        assertEquals(0, resource.closes)
        lease.release()
        assertEquals(1, resource.closes)
    }

    @Test
    fun use_closesAfterException() = runBlocking {
        val resource = CountingResource()
        val lease = resource.lease()
        val thrown = runCatching {
            lease.use<Unit> { error("boom") }
        }
        assertTrue(thrown.isFailure)
        assertEquals(1, resource.opens)
        assertEquals(1, resource.closes)
    }

    @Test
    fun failedOpen_doesNotLeaveAHold() = runBlocking {
        var opens = 0
        var closes = 0
        val lease = RefCountedLease<String>(
            open = {
                opens += 1
                error("no client")
            },
            onClose = { closes += 1 },
        )
        val first = runCatching { lease.hold() }
        assertTrue(first.isFailure)
        assertEquals(1, opens)
        assertEquals(0, closes)
        val second = runCatching { lease.hold() }
        assertTrue(second.isFailure)
        assertEquals(2, opens)
        lease.release()
        assertEquals(0, closes)
    }

    @Test
    fun extraRelease_isSafe() = runBlocking {
        val resource = CountingResource()
        val lease = resource.lease()
        lease.release()
        lease.release()
        assertEquals(0, resource.opens)
        assertEquals(0, resource.closes)
    }

    @Test
    fun secondSession_opensANewResource() = runBlocking {
        val resource = CountingResource()
        val lease = resource.lease()
        lease.use { }
        lease.use { }
        assertEquals(2, resource.opens)
        assertEquals(2, resource.closes)
    }

    private class CountingResource {
        var opens: Int = 0
        var closes: Int = 0

        fun lease(): RefCountedLease<CountingResource> = RefCountedLease(
            open = {
                opens += 1
                this
            },
            onClose = { closes += 1 },
        )
    }
}
