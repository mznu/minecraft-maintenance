package dev.mznu.maintenance.schedule

import java.time.Duration

internal fun dueReminderPeriods(
    reminderPeriods: List<Duration>,
    announcedPeriods: Set<Duration>,
    timeUntilStart: Duration,
): List<Duration> = reminderPeriods.filter { period ->
    period !in announcedPeriods && timeUntilStart <= period
}
