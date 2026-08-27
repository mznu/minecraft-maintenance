package dev.mznu.maintenance.schedule

import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleConfigurationParserTest {

    @Test
    fun `accepts an empty or valid schedule configuration`() {
        assertEquals(ScheduleConfigurationResult.Empty, parse("", ""))
        assertEquals(ScheduleConfigurationResult.Empty, parse("  ", "\t"))
        assertEquals(
            ScheduleConfigurationResult.Valid(
                MaintenanceWindow(
                    Instant.parse("2026-08-27T23:00:00Z"),
                    Instant.parse("2026-08-28T01:00:00Z"),
                ),
            ),
            parse("2026-08-27T23:00Z", "2026-08-28T01:00Z"),
        )
    }

    @Test
    fun `rejects an incomplete schedule configuration`() {
        assertInvalid(ScheduleConfigurationError.INCOMPLETE, "2026-08-28T01:00Z", "")
        assertInvalid(ScheduleConfigurationError.INCOMPLETE, "", "2026-08-28T01:00Z")
    }

    @Test
    fun `rejects invalid date-time formats`() {
        assertInvalid(ScheduleConfigurationError.INVALID_DATE_TIME, "now", "2026-08-28T01:00Z")
        assertInvalid(ScheduleConfigurationError.INVALID_DATE_TIME, "2026-08-27T23:00Z", "1h")
        assertInvalid(ScheduleConfigurationError.INVALID_DATE_TIME, "invalid", "invalid")
        assertInvalid(ScheduleConfigurationError.INVALID_DATE_TIME, 202608272300, 202608280100)
    }

    @Test
    fun `rejects an end that is equal to or before the start`() {
        assertInvalid(
            ScheduleConfigurationError.INVALID_ORDER,
            "2026-08-28T01:00Z",
            "2026-08-28T01:00Z",
        )
        assertInvalid(
            ScheduleConfigurationError.INVALID_ORDER,
            "2026-08-28T02:00Z",
            "2026-08-28T01:00Z",
        )
    }

    @Test
    fun `rejects an end that is not in the future`() {
        assertInvalid(
            ScheduleConfigurationError.END_NOT_FUTURE,
            "2026-08-27T22:00Z",
            "2026-08-28T00:00Z",
        )
        assertInvalid(
            ScheduleConfigurationError.END_NOT_FUTURE,
            "2026-08-27T21:00Z",
            "2026-08-27T23:00Z",
        )
    }

    private fun parse(startsAt: Any?, endsAt: Any?): ScheduleConfigurationResult =
        ScheduleConfigurationParser.parse(startsAt, endsAt, ZoneOffset.UTC, NOW)

    private fun assertInvalid(error: ScheduleConfigurationError, startsAt: Any?, endsAt: Any?) {
        assertEquals(ScheduleConfigurationResult.Invalid(error), parse(startsAt, endsAt))
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-28T00:00:00Z")
    }
}
