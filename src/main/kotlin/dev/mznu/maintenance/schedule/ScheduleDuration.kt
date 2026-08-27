package dev.mznu.maintenance.schedule

import java.time.Duration

internal object ScheduleDurationParser {

    private val configPattern = Regex(
        "^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$",
        RegexOption.IGNORE_CASE,
    )
    private val commandPattern = Regex(
        "^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?$",
        RegexOption.IGNORE_CASE,
    )

    fun parse(value: String): Duration? = parse(value, configPattern, requirePositiveComponents = false)

    fun parseCommandDuration(value: String): Duration? =
        parse(value, commandPattern, requirePositiveComponents = true)

    private fun parse(value: String, pattern: Regex, requirePositiveComponents: Boolean): Duration? {
        val match = pattern.matchEntire(value) ?: return null
        val rawComponents = match.groupValues.drop(1)
        if (rawComponents.all(String::isEmpty)) {
            return null
        }

        return try {
            val components = rawComponents.map { it.toLongOrZero() }
            if (requirePositiveComponents && rawComponents.indices.any {
                    rawComponents[it].isNotEmpty() && components[it] <= 0
                }
            ) {
                return null
            }
            Duration.ZERO
                .plusDays(components.getOrElse(0) { 0L })
                .plusHours(components.getOrElse(1) { 0L })
                .plusMinutes(components.getOrElse(2) { 0L })
                .plusSeconds(components.getOrElse(3) { 0L })
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun String.toLongOrZero(): Long = if (isEmpty()) 0L else toLong()
}

internal fun formatLocalizedDuration(duration: Duration, template: (String) -> String): String {
    require(!duration.isNegative)

    var seconds = duration.seconds
    val days = seconds / SECONDS_PER_DAY
    seconds %= SECONDS_PER_DAY
    val hours = seconds / SECONDS_PER_HOUR
    seconds %= SECONDS_PER_HOUR
    val minutes = seconds / SECONDS_PER_MINUTE
    seconds %= SECONDS_PER_MINUTE

    val parts = buildList {
        if (days > 0) add("day" to days)
        if (hours > 0) add("hour" to hours)
        if (minutes > 0) add("minute" to minutes)
        if (seconds > 0 || isEmpty()) add("second" to seconds)
    }
    return parts.joinToString(template("duration.separator")) { (unit, value) ->
        val quantity = if (value == 1L) "one" else "other"
        template("duration.$unit.$quantity").replace("<value>", value.toString())
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR
