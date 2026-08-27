package dev.mznu.maintenance.schedule

import java.time.Duration
import java.time.Instant

data class MaintenanceWindow(
    val startsAt: Instant,
    val endsAt: Instant,
)

internal fun MaintenanceWindow.isActiveAt(now: Instant): Boolean =
    !now.isBefore(startsAt) && now.isBefore(endsAt)

internal fun MaintenanceWindow.isUpcoming(now: Instant, noticePeriod: Duration): Boolean =
    now.isBefore(startsAt) && Duration.between(now, startsAt) <= noticePeriod
