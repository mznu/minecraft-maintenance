package dev.mznu.maintenance.serverlist

import dev.mznu.maintenance.schedule.MaintenanceWindow
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerListMotdSettingsTest {

    private val window = MaintenanceWindow(
        startsAt = Instant.parse("2026-09-01T02:00:00Z"),
        endsAt = Instant.parse("2026-09-01T04:00:00Z"),
    )

    @Test
    fun `selects manual MOTD for maintenance without a schedule`() {
        val settings = settings()

        assertEquals("manual", settings.select(true, null, window.startsAt))
    }

    @Test
    fun `selects scheduled MOTD and falls back to manual when it is absent`() {
        assertEquals("scheduled", settings().select(true, window, window.startsAt))
        assertEquals("manual", settings(scheduled = null).select(true, window, window.startsAt))
    }

    @Test
    fun `selects upcoming MOTD independently of the manual MOTD`() {
        val settings = settings(manual = null)
        val withinNoticePeriod = Instant.parse("2026-09-01T01:30:00Z")

        assertEquals("upcoming", settings.select(false, window, withinNoticePeriod))
    }

    @Test
    fun `keeps the normal MOTD outside maintenance and the upcoming notice period`() {
        val beforeNoticePeriod = Instant.parse("2026-09-01T00:59:59Z")

        assertNull(settings().select(false, window, beforeNoticePeriod))
        assertNull(settings().select(false, null, window.startsAt))
    }

    private fun settings(
        manual: String? = "manual",
        scheduled: String? = "scheduled",
        upcoming: String? = "upcoming",
    ): ServerListMotdSettings = ServerListMotdSettings(
        manual = manual,
        scheduled = scheduled,
        upcoming = upcoming,
        upcomingNoticePeriod = Duration.ofHours(1),
    )
}
