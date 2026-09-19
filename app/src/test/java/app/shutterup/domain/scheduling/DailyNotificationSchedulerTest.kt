package app.shutterup.domain.scheduling

import app.shutterup.domain.model.DayStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyNotificationSchedulerTest {

    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 9, 19)
    private val notifyTime = LocalTime.of(9, 0)

    @Test
    fun beforeNotifyTime_returnsTodayTrigger() {
        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.PENDING, todayNotified = false, paused = false, zone,
        )
        assertEquals(today.atTime(notifyTime).atZone(zone).toInstant(), trigger)
    }

    @Test
    fun afterNotifyTime_pendingUnfired_returnsNowPlus60Seconds() {
        val now = today.atTime(10, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.PENDING, todayNotified = false, paused = false, zone,
        )
        assertEquals(now.plusSeconds(60), trigger)
    }

    @Test
    fun completedToday_returnsTomorrow() {
        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.COMPLETED, todayNotified = false, paused = false, zone,
        )
        assertEquals(today.plusDays(1).atTime(notifyTime).atZone(zone).toInstant(), trigger)
    }

    @Test
    fun completedNoPhotoToday_returnsTomorrow() {
        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.COMPLETED_NO_PHOTO, todayNotified = false, paused = false, zone,
        )
        assertEquals(today.plusDays(1).atTime(notifyTime).atZone(zone).toInstant(), trigger)
    }

    @Test
    fun skippedToday_returnsTomorrow() {
        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.SKIPPED, todayNotified = false, paused = false, zone,
        )
        assertEquals(today.plusDays(1).atTime(notifyTime).atZone(zone).toInstant(), trigger)
    }

    @Test
    fun alreadyFiredPending_returnsTomorrow_noDoubleFire() {
        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.PENDING, todayNotified = true, paused = false, zone,
        )
        assertEquals(today.plusDays(1).atTime(notifyTime).atZone(zone).toInstant(), trigger)
    }

    @Test
    fun alreadyFiredAfterNotifyTime_returnsTomorrow_noDoubleFire() {
        val now = today.atTime(10, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.PENDING, todayNotified = true, paused = false, zone,
        )
        assertEquals(today.plusDays(1).atTime(notifyTime).atZone(zone).toInstant(), trigger)
    }

    @Test
    fun paused_returnsNull() {
        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.PENDING, todayNotified = false, paused = true, zone,
        )
        assertNull(trigger)
    }

    @Test
    fun dstSpringForward_notifyTimeInGap_shiftsForward() {
        val dstZone = ZoneId.of("America/New_York")
        val dstDay = LocalDate.of(2026, 3, 8)
        val gapTime = LocalTime.of(2, 30)
        val now = dstDay.atTime(1, 0).atZone(dstZone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, gapTime, DayStatus.PENDING, todayNotified = false, paused = false, dstZone,
        )
        val local = trigger!!.atZone(dstZone)
        assertEquals(dstDay, local.toLocalDate())
        assertEquals(LocalTime.of(3, 30), local.toLocalTime())
        assertEquals(ZonedDateTime.of(dstDay, gapTime, dstZone).toInstant(), trigger)
        assertTrue(local.toLocalTime() >= LocalTime.of(3, 0))
    }

    @Test
    fun sameInstantDifferentZones_yieldDifferentLocalTriggers() {
        val now = today.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant()
        val notify = LocalTime.of(15, 0)
        val utcTrigger = DailyNotificationScheduler.nextTrigger(
            now, notify, DayStatus.PENDING, todayNotified = false, paused = false, ZoneId.of("UTC"),
        )
        val nyTrigger = DailyNotificationScheduler.nextTrigger(
            now, notify, DayStatus.PENDING, todayNotified = false, paused = false, zone,
        )
        assertEquals(today.atTime(15, 0).atZone(ZoneId.of("UTC")).toInstant(), utcTrigger)
        assertEquals(today.atTime(15, 0).atZone(zone).toInstant(), nyTrigger)
        assertTrue(utcTrigger != nyTrigger)
    }

    @Test
    fun missedYesterdayPendingToday_behavesLikePending() {
        val now = today.atTime(8, 0).atZone(zone).toInstant()
        val trigger = DailyNotificationScheduler.nextTrigger(
            now, notifyTime, DayStatus.PENDING, todayNotified = false, paused = false, zone,
        )
        assertEquals(today.atTime(notifyTime).atZone(zone).toInstant(), trigger)
    }
}
