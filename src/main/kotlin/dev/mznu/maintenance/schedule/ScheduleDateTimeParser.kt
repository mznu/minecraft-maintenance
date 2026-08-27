package dev.mznu.maintenance.schedule

import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.util.Locale

internal object ScheduleDateTimeParser {

    private val hourPattern = Regex("[0-9]{1,2}")
    private val timeFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .toFormatter(Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    fun parseAbsoluteDateTime(value: String, timeZone: ZoneId): Instant? {
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        } catch (_: DateTimeParseException) {
            // Try a local date-time in the configured time zone below.
        }

        return try {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(timeZone)
                .toInstant()
        } catch (_: DateTimeException) {
            null
        }
    }

    fun parseCommandStart(value: String, timeZone: ZoneId, now: Instant): Instant? {
        parseAbsoluteDateTime(value, timeZone)?.let { return it }
        if (value.equals("now", ignoreCase = true)) {
            return now
        }
        if (value.startsWith(NOW_PLUS_PREFIX, ignoreCase = true)) {
            val duration = ScheduleDurationParser.parseCommandDuration(value.drop(NOW_PLUS_PREFIX.length))
                ?: return null
            return addDuration(now, duration)
        }

        val time = parseTimeOnly(value) ?: return null
        val currentDateTime = atZone(now, timeZone) ?: return null
        return atTime(currentDateTime.toLocalDate(), time, timeZone, currentDateTime.offset)
    }

    fun parseCommandEnd(value: String, timeZone: ZoneId, startsAt: Instant): Instant? {
        parseAbsoluteDateTime(value, timeZone)?.let { return it }
        parseTimeOnly(value)?.let { time ->
            val startDateTime = atZone(startsAt, timeZone) ?: return null
            val startDate = startDateTime.toLocalDate()
            val sameDay = atTime(startDate, time, timeZone, startDateTime.offset) ?: return null
            if (!sameDay.isBefore(startsAt)) {
                return sameDay
            }
            return try {
                atTime(startDate.plusDays(1), time, timeZone, startDateTime.offset)
            } catch (_: DateTimeException) {
                null
            }
        }

        val duration = ScheduleDurationParser.parseCommandDuration(value) ?: return null
        return addDuration(startsAt, duration)
    }

    private fun parseTimeOnly(value: String): LocalTime? {
        if (hourPattern.matches(value)) {
            return try {
                LocalTime.of(value.toInt(), 0)
            } catch (_: DateTimeException) {
                null
            }
        }

        return try {
            LocalTime.parse(value, timeFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun atTime(
        date: LocalDate,
        time: LocalTime,
        timeZone: ZoneId,
        preferredOffset: ZoneOffset,
    ): Instant? = try {
        ZonedDateTime.ofLocal(date.atTime(time), timeZone, preferredOffset).toInstant()
    } catch (_: DateTimeException) {
        null
    }

    private fun atZone(instant: Instant, timeZone: ZoneId): ZonedDateTime? = try {
        instant.atZone(timeZone)
    } catch (_: DateTimeException) {
        null
    }

    private fun addDuration(instant: Instant, duration: Duration): Instant? = try {
        instant.plus(duration)
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    private const val NOW_PLUS_PREFIX = "now+"
}

internal fun formatScheduleDateTimeForConfiguration(instant: Instant, timeZone: ZoneId): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(timeZone))
