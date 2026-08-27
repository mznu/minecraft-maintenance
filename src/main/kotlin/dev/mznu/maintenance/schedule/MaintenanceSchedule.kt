package dev.mznu.maintenance.schedule

import dev.mznu.maintenance.MaintenancePlugin
import dev.mznu.maintenance.message.MaintenanceMessages
import org.bukkit.scheduler.BukkitTask
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class MaintenanceSchedule(
    private val plugin: MaintenancePlugin,
    private val displayLocale: Locale = plugin.locale,
    private val messages: MaintenanceMessages = plugin.messages,
) {

    val timeZone: ZoneId = loadTimeZone()
    private val displayFormatter = loadDisplayFormatter()
    private val reminderPeriods = loadReminderPeriods()

    @Volatile
    var currentWindow: MaintenanceWindow? = loadWindow()
        private set

    private val announcedReminderPeriods = mutableSetOf<Duration>()
    private var checkTask: BukkitTask? = null

    init {
        currentWindow?.let { markElapsedReminders(it, Instant.now()) }
    }

    fun start() {
        if (checkTask != null) {
            return
        }
        reconcile()
        checkTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable(::reconcile),
            CHECK_INTERVAL_TICKS,
            CHECK_INTERVAL_TICKS,
        )
    }

    fun stop() {
        checkTask?.cancel()
        checkTask = null
    }

    fun parseStartInput(value: String, now: Instant = Instant.now()): Instant? =
        ScheduleDateTimeParser.parseCommandStart(value, timeZone, now)

    fun parseEndInput(value: String, startsAt: Instant): Instant? =
        ScheduleDateTimeParser.parseCommandEnd(value, timeZone, startsAt)

    fun format(instant: Instant): String = displayFormatter.format(instant)

    fun formatForConfiguration(instant: Instant): String =
        formatScheduleDateTimeForConfiguration(instant, timeZone)

    fun maintenanceStateAt(now: Instant = Instant.now()): Boolean? = currentWindow?.isActiveAt(now)

    fun setWindow(startsAt: Instant, endsAt: Instant) {
        require(endsAt.isAfter(startsAt)) {
            plugin.internalMessages.format("schedule.end-before-start")
        }

        val window = MaintenanceWindow(startsAt, endsAt)
        currentWindow = window
        markElapsedReminders(window, Instant.now())
        saveWindow(currentWindow)
        broadcastScheduled(window)
        reconcile()
    }

    fun clear(): Boolean {
        if (currentWindow == null) {
            return false
        }

        currentWindow = null
        announcedReminderPeriods.clear()
        saveWindow(null)
        return true
    }

    private fun reconcile() {
        val window = currentWindow ?: return
        val now = Instant.now()

        when {
            now.isBefore(window.startsAt) -> {
                plugin.setMaintenanceEnabled(false)
                broadcastDueReminders(window, now)
            }
            now.isBefore(window.endsAt) -> {
                if (plugin.setMaintenanceEnabled(true)) {
                    plugin.logger.info(
                        plugin.internalMessages.format(
                            "schedule.started",
                            "end" to formatForConfiguration(window.endsAt),
                        ),
                    )
                }
            }
            else -> {
                plugin.setMaintenanceEnabled(false)
                currentWindow = null
                announcedReminderPeriods.clear()
                saveWindow(null)
                plugin.logger.info(plugin.internalMessages.format("schedule.completed"))
            }
        }
    }

    private fun loadWindow(): MaintenanceWindow? {
        val configuration = ScheduleConfigurationParser.parse(
            plugin.config.get(STARTS_AT_PATH),
            plugin.config.get(ENDS_AT_PATH),
            timeZone,
            Instant.now(),
        )
        return when (configuration) {
            ScheduleConfigurationResult.Empty -> null
            is ScheduleConfigurationResult.Valid -> configuration.window
            is ScheduleConfigurationResult.Invalid -> throw IllegalArgumentException(
                scheduleConfigurationErrorMessage(configuration.error),
            )
        }
    }

    private fun scheduleConfigurationErrorMessage(error: ScheduleConfigurationError): String = when (error) {
        ScheduleConfigurationError.INCOMPLETE -> plugin.internalMessages.format("schedule.incomplete")
        ScheduleConfigurationError.INVALID_DATE_TIME -> plugin.internalMessages.format("schedule.invalid-date-time")
        ScheduleConfigurationError.INVALID_ORDER -> plugin.internalMessages.format("schedule.invalid-order")
        ScheduleConfigurationError.END_NOT_FUTURE -> plugin.internalMessages.format("schedule.end-not-future")
    }

    private fun loadReminderPeriods(): List<Duration> {
        val periods = mutableListOf<Duration>()
        for (value in plugin.config.getStringList(REMIND_BEFORE_PATH)) {
            val period = ScheduleDurationParser.parse(value)
            if (period == null || period.isZero) {
                plugin.logger.warning(
                    plugin.internalMessages.format("schedule.invalid-reminder", "value" to value),
                )
                continue
            }
            periods += period
        }
        return periods.distinct().sortedDescending()
    }

    private fun markElapsedReminders(window: MaintenanceWindow, now: Instant) {
        announcedReminderPeriods.clear()
        if (!now.isBefore(window.startsAt)) {
            announcedReminderPeriods += reminderPeriods
            return
        }

        val timeUntilStart = Duration.between(now, window.startsAt)
        announcedReminderPeriods += reminderPeriods.filter { timeUntilStart <= it }
    }

    private fun broadcastDueReminders(window: MaintenanceWindow, now: Instant) {
        val timeUntilStart = Duration.between(now, window.startsAt)
        for (period in dueReminderPeriods(reminderPeriods, announcedReminderPeriods, timeUntilStart)) {
            announcedReminderPeriods += period
            broadcast(REMINDER_NOTIFICATION_PATH, window, formatLocalizedDuration(period, messages::template))
        }
    }

    private fun broadcastScheduled(window: MaintenanceWindow) {
        broadcast(SCHEDULED_NOTIFICATION_PATH, window)
    }

    private fun broadcast(messagePath: String, window: MaintenanceWindow, timeUntilStart: String? = null) {
        val placeholders = mutableMapOf(
            "schedule_start" to format(window.startsAt),
            "schedule_end" to format(window.endsAt),
        )
        timeUntilStart?.let { placeholders["time_until_start"] = it }
        val message = try {
            messages.renderOptional(messagePath, placeholders) ?: return
        } catch (exception: Exception) {
            plugin.logger.warning(
                plugin.internalMessages.format("schedule.notification-invalid", "error" to exception.message),
            )
            return
        }
        plugin.server.onlinePlayers.forEach { it.sendMessage(message) }
    }

    private fun saveWindow(window: MaintenanceWindow?) {
        plugin.config.set(STARTS_AT_PATH, window?.startsAt?.let(::formatForConfiguration).orEmpty())
        plugin.config.set(ENDS_AT_PATH, window?.endsAt?.let(::formatForConfiguration).orEmpty())
        plugin.saveConfig()
    }

    private fun loadTimeZone(): ZoneId {
        val configuredTimeZone = plugin.config.getString(TIME_ZONE_PATH) ?: DEFAULT_TIME_ZONE
        return try {
            ZoneId.of(configuredTimeZone)
        } catch (_: DateTimeException) {
            plugin.logger.warning(
                plugin.internalMessages.format("schedule.invalid-time-zone", "time_zone" to configuredTimeZone),
            )
            ZoneOffset.UTC
        }
    }

    private fun loadDisplayFormatter(): DateTimeFormatter {
        val pattern = messages.template(DISPLAY_FORMAT_PATH)
        return try {
            DateTimeFormatter.ofPattern(pattern, displayLocale).withZone(timeZone)
        } catch (_: IllegalArgumentException) {
            plugin.logger.warning(
                plugin.internalMessages.format(
                    "schedule.invalid-display-format",
                    "format" to pattern,
                    "fallback" to DEFAULT_DISPLAY_FORMAT,
                ),
            )
            DateTimeFormatter.ofPattern(DEFAULT_DISPLAY_FORMAT, displayLocale).withZone(timeZone)
        }
    }

    private companion object {
        const val CHECK_INTERVAL_TICKS = 20L
        const val TIME_ZONE_PATH = "schedule.time-zone"
        const val DISPLAY_FORMAT_PATH = "schedule.display-format"
        const val STARTS_AT_PATH = "schedule.starts-at"
        const val ENDS_AT_PATH = "schedule.ends-at"
        const val REMIND_BEFORE_PATH = "schedule.notifications.remind-before"
        const val SCHEDULED_NOTIFICATION_PATH = "schedule.notifications.scheduled-message"
        const val REMINDER_NOTIFICATION_PATH = "schedule.notifications.reminder-message"
        const val DEFAULT_TIME_ZONE = "UTC"
        const val DEFAULT_DISPLAY_FORMAT = "yyyy-MM-dd HH:mm z"
    }
}
