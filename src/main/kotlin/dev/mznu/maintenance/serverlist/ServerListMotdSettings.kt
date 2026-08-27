package dev.mznu.maintenance.serverlist

import dev.mznu.maintenance.schedule.MaintenanceWindow
import dev.mznu.maintenance.schedule.isUpcoming
import java.time.Duration
import java.time.Instant

data class ServerListMotdSettings(
    val manual: String?,
    val scheduled: String?,
    val upcoming: String?,
    val upcomingNoticePeriod: Duration?,
) {

    internal fun select(
        maintenanceEnabled: Boolean,
        window: MaintenanceWindow?,
        now: Instant,
    ): String? = when {
        maintenanceEnabled && window != null -> scheduled ?: manual
        maintenanceEnabled -> manual
        window != null && upcomingNoticePeriod != null && window.isUpcoming(now, upcomingNoticePeriod) -> upcoming
        else -> null
    }
}
