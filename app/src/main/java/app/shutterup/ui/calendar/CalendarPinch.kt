package app.shutterup.ui.calendar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlin.math.abs
import kotlin.math.ln

/**
 * 0 = month grid, 1 = year view. Pinch-out (zoom > 1) moves toward the year.
 */
fun yearProgressAfterPinch(current: Float, zoom: Float): Float {
    if (zoom <= 0f) return current.coerceIn(0f, 1f)
    val delta = ln(zoom.toDouble()).toFloat()
    return (current + delta).coerceIn(0f, 1f)
}

fun snapYearProgress(progress: Float): Float = if (progress >= 0.5f) 1f else 0f

/**
 * Two fingers start a pinch. Once a one-finger horizontal swipe has won,
 * the rest of this pointer sequence stays a swipe — even if a second finger
 * lands. Swipe wins at month (and year-pager) level.
 */
fun pinchWinsOverSwipe(pointerCount: Int, horizontalSwipeWon: Boolean): Boolean =
    pointerCount >= 2 && !horizontalSwipeWon

fun horizontalSwipeWon(delta: Offset, touchSlop: Float): Boolean =
    abs(delta.x) > touchSlop && abs(delta.x) > abs(delta.y)

/**
 * Watches pointers in the Initial pass so a two-finger pinch can be consumed
 * before [androidx.compose.foundation.pager.HorizontalPager] treats it as a
 * swipe. A one-finger horizontal drag is never consumed here.
 */
suspend fun PointerInputScope.detectCalendarPinch(
    onPinchDelta: (zoom: Float) -> Unit,
    onPinchEnd: () -> Unit,
) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var swipeWon = false
        var pinching = false
        var lastDistance = 0f
        val origin = down.position
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break
            if (pinchWinsOverSwipe(pressed.size, swipeWon)) {
                val distance = (pressed[0].position - pressed[1].position).getDistance()
                if (!pinching) {
                    pinching = true
                    lastDistance = distance
                } else if (lastDistance > 0f) {
                    onPinchDelta(distance / lastDistance)
                    lastDistance = distance
                }
                event.changes.forEach { it.consume() }
            } else if (!pinching && pressed.size == 1) {
                if (horizontalSwipeWon(pressed[0].position - origin, slop)) {
                    swipeWon = true
                }
            } else if (pinching && pressed.size < 2) {
                break
            }
        }
        if (pinching) onPinchEnd()
    }
}
