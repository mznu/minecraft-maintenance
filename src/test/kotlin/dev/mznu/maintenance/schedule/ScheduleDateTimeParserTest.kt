package dev.mznu.maintenance.schedule

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduleDateTimeParserTest {

    @Test
    fun `formats configuration times as ISO date-times with the configured offset`() {
        assertEquals(
            "2026-08-27T10:02:03+09:00",
            formatScheduleDateTimeForConfiguration(SEOUL_NOW, SEOUL),
        )
    }

    @Test
    fun `parses local and offset ISO start times`() {
        assertEquals(
            Instant.parse("2026-09-01T00:00:00Z"),
            parseStart("2026-09-01T09:00", now = SEOUL_NOW),
        )
        assertEquals(
            Instant.parse("2026-09-01T09:00:00Z"),
            parseStart("2026-09-01T02:00-07:00", now = SEOUL_NOW),
        )
    }

    @Test
    fun `keeps relative and time-only values out of absolute configuration parsing`() {
        assertNull(ScheduleDateTimeParser.parseAbsoluteDateTime("now", SEOUL))
        assertNull(ScheduleDateTimeParser.parseAbsoluteDateTime("now+5m", SEOUL))
        assertNull(ScheduleDateTimeParser.parseAbsoluteDateTime("14", SEOUL))
        assertNull(ScheduleDateTimeParser.parseAbsoluteDateTime("14:30", SEOUL))
    }

    @Test
    fun `parses now and positive relative command durations without seconds`() {
        assertEquals(SEOUL_NOW, parseStart("now", now = SEOUL_NOW))
        assertEquals(SEOUL_NOW, parseStart("NOW", now = SEOUL_NOW))
        assertEquals(
            SEOUL_NOW.plus(Duration.ofDays(1).plusHours(2).plusMinutes(30)),
            parseStart("now+1d2h30m", now = SEOUL_NOW),
        )
        assertEquals(SEOUL_NOW.plus(Duration.ofMinutes(5)), parseStart("NOW+5M", now = SEOUL_NOW))

        assertNull(parseStart("now+", now = SEOUL_NOW))
        assertNull(parseStart("now+0m", now = SEOUL_NOW))
        assertNull(parseStart("now+5s", now = SEOUL_NOW))
        assertNull(parseStart("now-5m", now = SEOUL_NOW))
    }

    @Test
    fun `parses strict start time on the current date in the configured time zone`() {
        assertEquals(
            Instant.parse("2026-08-26T16:00:00Z"),
            parseStart("1", now = SEOUL_NOW),
        )
        assertEquals(
            Instant.parse("2026-08-26T16:00:00Z"),
            parseStart("01", now = SEOUL_NOW),
        )
        assertEquals(
            Instant.parse("2026-08-27T05:30:00Z"),
            parseStart("14:30", now = SEOUL_NOW),
        )
        assertEquals(
            Instant.parse("2026-08-26T15:15:00Z"),
            parseStart("00:15", now = Instant.parse("2026-08-26T23:30:00Z")),
        )

        assertNull(parseStart("1:30", now = SEOUL_NOW))
        assertNull(parseStart("000", now = SEOUL_NOW))
        assertNull(parseStart("24", now = SEOUL_NOW))
        assertNull(parseStart("24:00", now = SEOUL_NOW))
        assertNull(parseStart("14:30:00", now = SEOUL_NOW))
    }

    @Test
    fun `parses end time on the start date or the following date`() {
        val startsAt = Instant.parse("2026-08-27T13:00:00Z") // 22:00 in Asia/Seoul

        assertEquals(Instant.parse("2026-08-27T14:00:00Z"), parseEnd("23:00", startsAt))
        assertEquals(Instant.parse("2026-08-27T16:00:00Z"), parseEnd("01:00", startsAt))
        assertEquals(Instant.parse("2026-08-27T14:00:00Z"), parseEnd("23", startsAt))
        assertEquals(Instant.parse("2026-08-27T16:00:00Z"), parseEnd("1", startsAt))
        assertEquals(startsAt, parseEnd("22:00", startsAt))
        assertEquals(startsAt, parseEnd("22", startsAt))
        assertEquals(startsAt.plus(Duration.ofMinutes(90)), parseEnd("1h30m", startsAt))
        assertNull(parseEnd("5s", startsAt))
        assertNull(parseEnd("now", startsAt))
        assertNull(parseEnd("now+1h", startsAt))
    }

    @Test
    fun `moves an earlier end time to the next local date across daylight saving time`() {
        val newYork = ZoneId.of("America/New_York")
        val startsAt = Instant.parse("2026-03-08T04:00:00Z") // 23:00 before the spring-forward transition

        assertEquals(
            Instant.parse("2026-03-08T07:00:00Z"),
            ScheduleDateTimeParser.parseCommandEnd("03:00", newYork, startsAt),
        )
    }

    @Test
    fun `prefers the current or start offset during a repeated daylight saving hour`() {
        val newYork = ZoneId.of("America/New_York")
        val firstOccurrence = Instant.parse("2026-11-01T05:30:00Z") // 01:30 at UTC-04:00
        val secondOccurrence = Instant.parse("2026-11-01T06:30:00Z") // 01:30 at UTC-05:00

        assertEquals(
            Instant.parse("2026-11-01T05:45:00Z"),
            ScheduleDateTimeParser.parseCommandStart("01:45", newYork, firstOccurrence),
        )
        assertEquals(
            Instant.parse("2026-11-01T06:45:00Z"),
            ScheduleDateTimeParser.parseCommandStart("01:45", newYork, secondOccurrence),
        )
        assertEquals(
            Instant.parse("2026-11-01T06:45:00Z"),
            ScheduleDateTimeParser.parseCommandEnd("01:45", newYork, secondOccurrence),
        )
    }

    private fun parseStart(value: String, now: Instant): Instant? =
        ScheduleDateTimeParser.parseCommandStart(value, SEOUL, now)

    private fun parseEnd(value: String, startsAt: Instant): Instant? =
        ScheduleDateTimeParser.parseCommandEnd(value, SEOUL, startsAt)

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val SEOUL_NOW: Instant = Instant.parse("2026-08-27T01:02:03Z")
    }
}
