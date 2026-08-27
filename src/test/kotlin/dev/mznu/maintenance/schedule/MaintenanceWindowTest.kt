package dev.mznu.maintenance.schedule

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaintenanceWindowTest {

    private val window = MaintenanceWindow(
        Instant.parse("2026-09-01T02:00:00Z"),
        Instant.parse("2026-09-01T04:00:00Z"),
    )

    @Test
    fun `upcoming window includes notice boundary but excludes maintenance start`() {
        val noticePeriod = Duration.ofHours(1)

        assertFalse(window.isUpcoming(Instant.parse("2026-09-01T00:59:59Z"), noticePeriod))
        assertTrue(window.isUpcoming(Instant.parse("2026-09-01T01:00:00Z"), noticePeriod))
        assertTrue(window.isUpcoming(Instant.parse("2026-09-01T01:59:59Z"), noticePeriod))
        assertFalse(window.isUpcoming(Instant.parse("2026-09-01T02:00:00Z"), noticePeriod))
    }

    @Test
    fun `active window includes start but excludes end`() {
        assertFalse(window.isActiveAt(Instant.parse("2026-09-01T01:59:59Z")))
        assertTrue(window.isActiveAt(Instant.parse("2026-09-01T02:00:00Z")))
        assertTrue(window.isActiveAt(Instant.parse("2026-09-01T03:59:59Z")))
        assertFalse(window.isActiveAt(Instant.parse("2026-09-01T04:00:00Z")))
    }
}
