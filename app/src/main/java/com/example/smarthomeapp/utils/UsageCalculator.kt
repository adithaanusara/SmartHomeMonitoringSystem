package com.example.smarthomeapp.utils

import com.example.smarthomeapp.data.model.DeviceEvent
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.EventSource

data class UsageSummary(
    val onDurationMs: Long = 0L,
    val timesSwitchedOn: Int = 0,
    /** Switch-offs driven by the worker or the schedule rather than by a person. */
    val automaticOffCount: Int = 0,
)

/**
 * Derives usage from the `/events` transition log.
 *
 * Folding transitions rather than keeping a running counter is what makes the report trustworthy:
 * a counter drifts every time a client dies mid-session, whereas the log can be replayed for any
 * window after the fact.
 *
 * Pure and clock-free so it can be unit tested on the JVM — see UsageCalculatorTest.
 */
object UsageCalculator {

    /**
     * On-duration inside `[windowStart, windowEnd]`, in milliseconds.
     *
     * An ON interval that began before the window contributes only its overlapping part, and a
     * device still on at `windowEnd` is counted up to that point. Intervals are only counted when
     * their opening ON transition was actually recorded — a device that the seed data set to ON
     * without an event has no start time to measure from, so it contributes nothing until the next
     * time it is switched.
     */
    fun summarise(
        events: List<DeviceEvent>,
        windowStart: Long,
        windowEnd: Long,
    ): UsageSummary {
        if (windowEnd <= windowStart) return UsageSummary()

        var onDuration = 0L
        var openedAt: Long? = null
        var timesSwitchedOn = 0
        var automaticOffCount = 0

        for (event in events.sortedBy { it.ts }) {
            val inWindow = event.ts in windowStart..windowEnd

            if (event.toStatus == DeviceStatus.ON) {
                // Consecutive ON transitions keep the earliest opening rather than restarting it.
                if (openedAt == null) openedAt = event.ts
                if (inWindow) timesSwitchedOn++
            } else {
                openedAt?.let { start ->
                    onDuration += overlap(start, event.ts, windowStart, windowEnd)
                }
                openedAt = null
                if (inWindow && event.eventSource.isAutomatic()) automaticOffCount++
            }
        }

        // Still on at the end of the window.
        openedAt?.let { start -> onDuration += overlap(start, windowEnd, windowStart, windowEnd) }

        return UsageSummary(
            onDurationMs = onDuration,
            timesSwitchedOn = timesSwitchedOn,
            automaticOffCount = automaticOffCount,
        )
    }

    private fun overlap(start: Long, end: Long, windowStart: Long, windowEnd: Long): Long =
        (minOf(end, windowEnd) - maxOf(start, windowStart)).coerceAtLeast(0L)

    private fun EventSource.isAutomatic(): Boolean =
        this == EventSource.WORKER || this == EventSource.SCHEDULE
}

/**
 * Compact duration for the report: "0m", "45m", "2h 05m", "1d 3h".
 *
 * Rounds to the nearest minute rather than truncating. Truncating makes the per-device rows fail to
 * add up to the total — 17m43s and 29s render as "17m" and "0m" beside a total of "18m" — because
 * the total is summed in milliseconds before formatting. Rounding each part keeps them consistent.
 */
fun formatOnDuration(millis: Long): String {
    val totalMinutes = Math.round(millis / 60_000.0)
    val days = totalMinutes / 1_440
    val hours = (totalMinutes % 1_440) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
        else -> "${minutes}m"
    }
}
