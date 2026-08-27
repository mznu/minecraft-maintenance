package dev.mznu.maintenance.schedule

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduleDurationTest {

    @Test
    fun `parses duration units and combinations`() {
        assertEquals(Duration.ofHours(1), ScheduleDurationParser.parse("1h"))
        assertEquals(Duration.ofMinutes(90), ScheduleDurationParser.parse("90m"))
        assertEquals(Duration.ofMinutes(90), ScheduleDurationParser.parse("1h30m"))
        assertEquals(
            Duration.ofHours(26).plusMinutes(30).plusSeconds(15),
            ScheduleDurationParser.parse("1d2h30m15s"),
        )
        assertEquals(Duration.ofHours(2), ScheduleDurationParser.parse("2H"))
    }

    @Test
    fun `rejects malformed durations`() {
        assertNull(ScheduleDurationParser.parse(""))
        assertNull(ScheduleDurationParser.parse("1"))
        assertNull(ScheduleDurationParser.parse("1hour"))
        assertNull(ScheduleDurationParser.parse("1m1h"))
        assertNull(ScheduleDurationParser.parse("-1h"))
    }

    @Test
    fun `parses positive whole-number command durations without seconds`() {
        assertEquals(Duration.ofMinutes(1), ScheduleDurationParser.parseCommandDuration("1m"))
        assertEquals(Duration.ofMinutes(90), ScheduleDurationParser.parseCommandDuration("90m"))
        assertEquals(Duration.ofMinutes(90), ScheduleDurationParser.parseCommandDuration("1h30m"))
        assertEquals(Duration.ofHours(26), ScheduleDurationParser.parseCommandDuration("1d2h"))
        assertEquals(Duration.ofHours(2), ScheduleDurationParser.parseCommandDuration("2H"))
    }

    @Test
    fun `rejects zero fractional negative and second command durations`() {
        assertNull(ScheduleDurationParser.parseCommandDuration("0m"))
        assertNull(ScheduleDurationParser.parseCommandDuration("1h0m"))
        assertNull(ScheduleDurationParser.parseCommandDuration("1.5h"))
        assertNull(ScheduleDurationParser.parseCommandDuration("-1h"))
        assertNull(ScheduleDurationParser.parseCommandDuration("10s"))
        assertNull(ScheduleDurationParser.parseCommandDuration("1h10s"))
    }

    @Test
    fun `formats durations with localized unit templates`() {
        val english = durationTemplates(
            day = "<value> day" to "<value> days",
            hour = "<value> hour" to "<value> hours",
            minute = "<value> minute" to "<value> minutes",
            second = "<value> second" to "<value> seconds",
        )
        val korean = durationTemplates(
            day = "<value>일" to "<value>일",
            hour = "<value>시간" to "<value>시간",
            minute = "<value>분" to "<value>분",
            second = "<value>초" to "<value>초",
        )

        assertEquals("1 hour", formatLocalizedDuration(Duration.ofHours(1), english::getValue))
        assertEquals("30 minutes", formatLocalizedDuration(Duration.ofMinutes(30), english::getValue))
        assertEquals("1 hour 30 minutes", formatLocalizedDuration(Duration.ofMinutes(90), english::getValue))
        assertEquals(
            "1 day 2 hours 30 minutes 10 seconds",
            formatLocalizedDuration(
                Duration.ofDays(1).plusHours(2).plusMinutes(30).plusSeconds(10),
                english::getValue,
            ),
        )
        assertEquals("1시간 30분", formatLocalizedDuration(Duration.ofMinutes(90), korean::getValue))
        assertEquals("0 seconds", formatLocalizedDuration(Duration.ZERO, english::getValue))
    }

    private fun durationTemplates(
        day: Pair<String, String>,
        hour: Pair<String, String>,
        minute: Pair<String, String>,
        second: Pair<String, String>,
    ): Map<String, String> = mapOf(
        "duration.separator" to " ",
        "duration.day.one" to day.first,
        "duration.day.other" to day.second,
        "duration.hour.one" to hour.first,
        "duration.hour.other" to hour.second,
        "duration.minute.one" to minute.first,
        "duration.minute.other" to minute.second,
        "duration.second.one" to second.first,
        "duration.second.other" to second.second,
    )
}
