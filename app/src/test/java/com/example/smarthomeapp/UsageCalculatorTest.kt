package com.example.smarthomeapp

import com.example.smarthomeapp.data.model.DeviceEvent
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.EventSource
import com.example.smarthomeapp.utils.UsageCalculator
import com.example.smarthomeapp.utils.formatOnDuration
import org.junit.Assert.assertEquals
import org.junit.Test

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

/** Window is a clean 10-hour span so expected values stay readable. */
private const val START = 1_000_000L
private const val END = START + 10 * HOUR

private fun event(
    atMinutes: Long,
    to: DeviceStatus,
    from: DeviceStatus = if (to == DeviceStatus.ON) DeviceStatus.OFF else DeviceStatus.ON,
    source: EventSource = EventSource.APP,
) = DeviceEvent(
    ts = START + atMinutes * MINUTE,
    from = from.name,
    to = to.name,
    source = source.name,
)

class UsageCalculatorTest {

    @Test
    fun `no events means no usage`() {
        val result = UsageCalculator.summarise(emptyList(), START, END)
        assertEquals(0L, result.onDurationMs)
        assertEquals(0, result.timesSwitchedOn)
    }

    @Test
    fun `a completed on-off interval is counted`() {
        val result = UsageCalculator.summarise(
            listOf(event(60, DeviceStatus.ON), event(90, DeviceStatus.OFF)),
            START,
            END,
        )
        assertEquals(30 * MINUTE, result.onDurationMs)
        assertEquals(1, result.timesSwitchedOn)
    }

    @Test
    fun `multiple intervals accumulate`() {
        val result = UsageCalculator.summarise(
            listOf(
                event(10, DeviceStatus.ON),
                event(25, DeviceStatus.OFF),
                event(100, DeviceStatus.ON),
                event(160, DeviceStatus.OFF),
            ),
            START,
            END,
        )
        assertEquals(75 * MINUTE, result.onDurationMs)
        assertEquals(2, result.timesSwitchedOn)
    }

    @Test
    fun `a device still on at the window end is counted up to the end`() {
        val result = UsageCalculator.summarise(
            listOf(event(9 * 60, DeviceStatus.ON)),
            START,
            END,
        )
        assertEquals(1 * HOUR, result.onDurationMs)
    }

    @Test
    fun `an interval starting before the window only counts its overlap`() {
        val early = DeviceEvent(
            ts = START - 2 * HOUR,
            from = DeviceStatus.OFF.name,
            to = DeviceStatus.ON.name,
            source = EventSource.APP.name,
        )
        val result = UsageCalculator.summarise(
            listOf(early, event(30, DeviceStatus.OFF)),
            START,
            END,
        )
        assertEquals(30 * MINUTE, result.onDurationMs)
        // The switch-on happened before the window, so it is not counted as an in-window action.
        assertEquals(0, result.timesSwitchedOn)
    }

    @Test
    fun `an interval entirely before the window contributes nothing`() {
        val on = DeviceEvent(ts = START - 5 * HOUR, to = DeviceStatus.ON.name, from = DeviceStatus.OFF.name)
        val off = DeviceEvent(ts = START - 4 * HOUR, to = DeviceStatus.OFF.name, from = DeviceStatus.ON.name)
        val result = UsageCalculator.summarise(listOf(on, off), START, END)
        assertEquals(0L, result.onDurationMs)
    }

    @Test
    fun `events are sorted before folding so out-of-order input is safe`() {
        val result = UsageCalculator.summarise(
            listOf(event(90, DeviceStatus.OFF), event(60, DeviceStatus.ON)),
            START,
            END,
        )
        assertEquals(30 * MINUTE, result.onDurationMs)
    }

    @Test
    fun `consecutive ON transitions keep the earliest opening`() {
        val result = UsageCalculator.summarise(
            listOf(
                event(30, DeviceStatus.ON),
                event(45, DeviceStatus.ON),
                event(90, DeviceStatus.OFF),
            ),
            START,
            END,
        )
        assertEquals(60 * MINUTE, result.onDurationMs)
    }

    @Test
    fun `a worker cutoff closes the interval and counts as automatic`() {
        val result = UsageCalculator.summarise(
            listOf(
                event(10, DeviceStatus.ON),
                event(25, DeviceStatus.OFF, source = EventSource.WORKER),
            ),
            START,
            END,
        )
        assertEquals(15 * MINUTE, result.onDurationMs)
        assertEquals(1, result.automaticOffCount)
    }

    @Test
    fun `a manual switch-off is not counted as automatic`() {
        val result = UsageCalculator.summarise(
            listOf(event(10, DeviceStatus.ON), event(25, DeviceStatus.OFF, source = EventSource.APP)),
            START,
            END,
        )
        assertEquals(0, result.automaticOffCount)
    }

    @Test
    fun `a schedule switch-off counts as automatic`() {
        val result = UsageCalculator.summarise(
            listOf(event(10, DeviceStatus.ON), event(25, DeviceStatus.OFF, source = EventSource.SCHEDULE)),
            START,
            END,
        )
        assertEquals(1, result.automaticOffCount)
    }

    @Test
    fun `DISCONNECTED closes an open interval like any non-ON transition`() {
        val result = UsageCalculator.summarise(
            listOf(
                event(10, DeviceStatus.ON),
                event(40, DeviceStatus.DISCONNECTED, source = EventSource.WORKER),
            ),
            START,
            END,
        )
        assertEquals(30 * MINUTE, result.onDurationMs)
    }

    @Test
    fun `an inverted window yields nothing rather than a negative duration`() {
        val result = UsageCalculator.summarise(
            listOf(event(10, DeviceStatus.ON), event(20, DeviceStatus.OFF)),
            END,
            START,
        )
        assertEquals(0L, result.onDurationMs)
    }
}

class FormatOnDurationTest {

    @Test
    fun `rounds to the nearest minute so rows add up to their total`() {
        val a = 17 * MINUTE + 43_000L  // 17m43s
        val b = 29_000L                // 29s
        assertEquals("18m", formatOnDuration(a))
        assertEquals("0m", formatOnDuration(b))
        // The total is summed in millis before formatting; the parts must not contradict it.
        assertEquals("18m", formatOnDuration(a + b))
    }

    @Test
    fun `rounding carries into the next hour`() {
        assertEquals("1h 00m", formatOnDuration(59 * MINUTE + 45_000))
    }

    @Test
    fun `formats minutes, hours and days`() {
        assertEquals("0m", formatOnDuration(0))
        assertEquals("45m", formatOnDuration(45 * MINUTE))
        assertEquals("1h 00m", formatOnDuration(HOUR))
        assertEquals("2h 05m", formatOnDuration(2 * HOUR + 5 * MINUTE))
        assertEquals("1d 3h", formatOnDuration(27 * HOUR))
    }
}
