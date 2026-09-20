package app.shutterup.data.ai

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Opens [T] on first hold and closes it when the last hold is released.
 * [use] is a hold for the duration of [block].
 */
internal class RefCountedLease<T>(
    private val open: () -> T,
    private val onClose: (T) -> Unit,
) {
    private val lock = Mutex()
    private var value: T? = null
    private var holds: Int = 0

    suspend fun hold(): T = lock.withLock {
        val current = value ?: open().also { value = it }
        holds += 1
        current
    }

    suspend fun release() {
        lock.withLock {
            if (holds > 0) holds -= 1
            if (holds == 0) {
                val closing = value
                value = null
                closing?.let { runCatching { onClose(it) } }
            }
        }
    }

    suspend fun <R> use(block: suspend (T) -> R): R {
        val leased = hold()
        try {
            return block(leased)
        } finally {
            release()
        }
    }
}
