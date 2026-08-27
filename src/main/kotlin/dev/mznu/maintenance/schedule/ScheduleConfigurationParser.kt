package dev.mznu.maintenance.schedule

import java.time.Instant
import java.time.ZoneId

internal enum class ScheduleConfigurationError {
    INCOMPLETE,
    INVALID_DATE_TIME,
    INVALID_ORDER,
    END_NOT_FUTURE,
}

internal sealed interface ScheduleConfigurationResult {
    data object Empty : ScheduleConfigurationResult
    data class Valid(val window: MaintenanceWindow) : ScheduleConfigurationResult
    data class Invalid(val error: ScheduleConfigurationError) : ScheduleConfigurationResult
}

internal object ScheduleConfigurationParser {

    fun parse(
        startsAtValue: Any?,
        endsAtValue: Any?,
        timeZone: ZoneId,
        now: Instant,
    ): ScheduleConfigurationResult {
        if ((startsAtValue != null && startsAtValue !is String) ||
            (endsAtValue != null && endsAtValue !is String)
        ) {
            return ScheduleConfigurationResult.Invalid(ScheduleConfigurationError.INVALID_DATE_TIME)
        }

        val startsAtText = startsAtValue?.trim().orEmpty()
        val endsAtText = endsAtValue?.trim().orEmpty()
        if (startsAtText.isEmpty() && endsAtText.isEmpty()) {
            return ScheduleConfigurationResult.Empty
        }
        if (startsAtText.isEmpty() || endsAtText.isEmpty()) {
            return ScheduleConfigurationResult.Invalid(ScheduleConfigurationError.INCOMPLETE)
        }

        val startsAt = ScheduleDateTimeParser.parseAbsoluteDateTime(startsAtText, timeZone)
        val endsAt = ScheduleDateTimeParser.parseAbsoluteDateTime(endsAtText, timeZone)
        if (startsAt == null || endsAt == null) {
            return ScheduleConfigurationResult.Invalid(ScheduleConfigurationError.INVALID_DATE_TIME)
        }
        if (!endsAt.isAfter(startsAt)) {
            return ScheduleConfigurationResult.Invalid(ScheduleConfigurationError.INVALID_ORDER)
        }
        if (!endsAt.isAfter(now)) {
            return ScheduleConfigurationResult.Invalid(ScheduleConfigurationError.END_NOT_FUTURE)
        }

        return ScheduleConfigurationResult.Valid(MaintenanceWindow(startsAt, endsAt))
    }
}
