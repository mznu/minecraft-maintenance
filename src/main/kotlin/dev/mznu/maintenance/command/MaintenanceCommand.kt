package dev.mznu.maintenance.command

import dev.mznu.maintenance.MaintenancePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executor
import java.util.logging.Level

class MaintenanceCommand(
    private val plugin: MaintenancePlugin,
) : TabExecutor {

    private val syncExecutor = Executor { task ->
        plugin.server.scheduler.runTask(plugin, task)
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        when (val input = parseMaintenanceCommandInput(args.asList())) {
            MaintenanceCommandInput.Root -> {
                send(sender, "command.root.version", "version" to plugin.pluginMeta.version)
                send(sender, "command.root.help-hint", "label" to label)
            }
            MaintenanceCommandInput.Help -> sendAll(sender, "command.help", "label" to label)
            MaintenanceCommandInput.Status -> handleStatus(sender)
            is MaintenanceCommandInput.Toggle -> handleToggle(sender, input.enabled)
            MaintenanceCommandInput.Reload -> handleReload(sender)
            is MaintenanceCommandInput.Unknown -> {
                send(sender, "command.unknown-subcommand", "input" to input.value)
                send(sender, "command.root.help-hint", "label" to label)
            }
            is MaintenanceCommandInput.Invalid -> handleInvalidInput(sender, label, input)
            MaintenanceCommandInput.ScheduleMissingTimes -> {
                send(sender, "command.schedule.missing-times")
                sendUsage(sender, label, CommandUsage.SCHEDULE.key)
            }
            is MaintenanceCommandInput.ScheduleStartOnly -> handleScheduleStartOnly(sender, label, input.value)
            MaintenanceCommandInput.ScheduleStatus -> handleScheduleStatus(sender)
            MaintenanceCommandInput.ScheduleCancel -> handleScheduleCancel(sender)
            is MaintenanceCommandInput.ScheduleCreate ->
                handleScheduleCreate(sender, label, input.start, input.end)
            MaintenanceCommandInput.WhitelistMissingSubcommand -> {
                send(sender, "command.whitelist.missing-subcommand")
                sendUsage(sender, label, CommandUsage.WHITELIST.key)
            }
            is MaintenanceCommandInput.WhitelistUnknownSubcommand -> {
                send(sender, "command.whitelist.unknown-subcommand", "input" to input.value)
                sendUsage(sender, label, CommandUsage.WHITELIST.key)
            }
            is MaintenanceCommandInput.WhitelistAdd -> handleWhitelistAdd(sender, input.player)
            is MaintenanceCommandInput.WhitelistRemove -> handleWhitelistRemove(sender, input.player)
            MaintenanceCommandInput.WhitelistList -> handleWhitelistList(sender)
        }
        return true
    }

    private fun handleInvalidInput(
        sender: CommandSender,
        label: String,
        input: MaintenanceCommandInput.Invalid,
    ) {
        send(sender, input.error.messagePath)
        sendUsage(sender, label, input.usage.key)
    }

    private fun handleReload(sender: CommandSender) {
        try {
            plugin.reloadMaintenanceConfiguration()
            send(sender, "command.reload.success")
        } catch (exception: Exception) {
            plugin.logger.log(
                Level.SEVERE,
                plugin.internalMessages.format("command.reload-failed"),
                exception,
            )
            send(sender, "command.reload.failure")
        }
    }

    private fun handleStatus(sender: CommandSender) {
        if (plugin.maintenanceSchedule.currentWindow != null) {
            handleScheduleStatus(sender)
            return
        }

        val messagePath = if (plugin.isMaintenanceEnabled) {
            "command.status.enabled"
        } else {
            "command.status.disabled"
        }
        send(sender, messagePath)
    }

    private fun handleToggle(
        sender: CommandSender,
        enabled: Boolean,
    ) {
        val scheduleCancelled = plugin.maintenanceSchedule.clear()
        if (scheduleCancelled) {
            send(sender, "command.toggle.schedule-cancelled")
        }

        val maintenanceChanged = plugin.setMaintenanceEnabled(enabled)
        send(sender, toggleResultMessagePath(enabled, maintenanceChanged, scheduleCancelled))
    }

    private fun handleScheduleStartOnly(sender: CommandSender, label: String, value: String) {
        if (plugin.maintenanceSchedule.parseStartInput(value) == null) {
            send(sender, "command.schedule.invalid-start")
        } else {
            send(sender, "command.schedule.missing-end")
        }
        sendUsage(sender, label, CommandUsage.SCHEDULE.key)
    }

    private fun handleScheduleCreate(
        sender: CommandSender,
        label: String,
        startInput: String,
        endInput: String,
    ) {
        val now = Instant.now()
        val startsAt = plugin.maintenanceSchedule.parseStartInput(startInput, now)
        if (startsAt == null) {
            send(sender, "command.schedule.invalid-start")
            sendUsage(sender, label, CommandUsage.SCHEDULE.key)
            return
        }

        val endsAt = plugin.maintenanceSchedule.parseEndInput(endInput, startsAt)
        if (endsAt == null) {
            send(sender, "command.schedule.invalid-end")
            sendUsage(sender, label, CommandUsage.SCHEDULE.key)
            return
        }
        if (!endsAt.isAfter(startsAt)) {
            send(sender, "command.schedule.end-before-start")
            return
        }
        if (!endsAt.isAfter(now)) {
            send(sender, "command.schedule.end-not-future")
            return
        }

        plugin.maintenanceSchedule.setWindow(startsAt, endsAt)
        send(
            sender,
            "command.schedule.created",
            "schedule_start" to formatScheduleTime(sender, startsAt),
            "schedule_end" to formatScheduleTime(sender, endsAt),
        )
    }

    private fun handleScheduleStatus(sender: CommandSender) {
        val window = plugin.maintenanceSchedule.currentWindow
        if (window == null) {
            send(sender, "command.schedule.none")
            return
        }

        val messagePath = if (Instant.now().isBefore(window.startsAt)) {
            "command.schedule.status-pending"
        } else {
            "command.schedule.status-active"
        }
        send(
            sender,
            messagePath,
            "schedule_start" to formatScheduleTime(sender, window.startsAt),
            "schedule_end" to formatScheduleTime(sender, window.endsAt),
        )
    }

    private fun handleScheduleCancel(sender: CommandSender) {
        if (!plugin.maintenanceSchedule.clear()) {
            send(sender, "command.schedule.none")
            return
        }

        plugin.setMaintenanceEnabled(false)
        send(sender, "command.schedule.cancelled-and-disabled")
    }

    private fun handleWhitelistAdd(sender: CommandSender, requestedName: String) {
        val onlinePlayer = plugin.server.getPlayerExact(requestedName)
        if (onlinePlayer != null) {
            addToWhitelist(sender, onlinePlayer.uniqueId, onlinePlayer.name)
            return
        }

        val profile = try {
            plugin.server.createProfile(requestedName)
        } catch (_: IllegalArgumentException) {
            send(sender, "command.whitelist.invalid-player", "player" to requestedName)
            return
        }

        send(sender, "command.whitelist.looking-up", "player" to requestedName)
        profile.update().whenCompleteAsync({ updatedProfile, throwable ->
            if (throwable != null) {
                plugin.logger.warning(
                    plugin.internalMessages.format(
                        "command.player-lookup-failed",
                        "player" to requestedName,
                        "error" to throwable.message,
                    ),
                )
                send(sender, "command.whitelist.lookup-failed", "player" to requestedName)
                return@whenCompleteAsync
            }

            val playerId = updatedProfile.id
            val playerName = updatedProfile.name
            if (playerId == null || playerName == null) {
                send(sender, "command.whitelist.not-found", "player" to requestedName)
                return@whenCompleteAsync
            }

            addToWhitelist(sender, playerId, playerName)
        }, syncExecutor)
    }

    private fun addToWhitelist(sender: CommandSender, playerId: UUID, playerName: String) {
        val added = try {
            plugin.maintenanceWhitelist.add(playerId, playerName)
        } catch (exception: IOException) {
            logWhitelistSaveFailure(exception)
            send(sender, "command.whitelist.save-failed")
            return
        }
        val messagePath = if (added) "command.whitelist.added" else "command.whitelist.already-added"
        send(sender, messagePath, "player" to playerName)
    }

    private fun handleWhitelistRemove(sender: CommandSender, player: String) {
        val removed = try {
            plugin.maintenanceWhitelist.remove(player)
        } catch (exception: IOException) {
            logWhitelistSaveFailure(exception)
            send(sender, "command.whitelist.save-failed")
            return
        }
        if (removed == null) {
            send(sender, "command.whitelist.not-listed", "player" to player)
        } else {
            send(sender, "command.whitelist.removed", "player" to removed.name)
        }
    }

    private fun logWhitelistSaveFailure(exception: IOException) {
        plugin.logger.log(
            Level.SEVERE,
            plugin.internalMessages.format("command.whitelist-save-failed"),
            exception,
        )
    }

    private fun handleWhitelistList(sender: CommandSender) {
        val players = plugin.maintenanceWhitelist.getPlayers()
        if (players.isEmpty()) {
            send(sender, "command.whitelist.empty")
            return
        }
        send(sender, "command.whitelist.list", "players" to players.joinToString { it.name })
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        val suggestions = when {
            args.size == 1 -> SUBCOMMANDS
            args.size == 2 && args[0].equals("schedule", ignoreCase = true) -> SCHEDULE_SUBCOMMANDS
            args.size == 3 && args[0].equals("schedule", ignoreCase = true) &&
                SCHEDULE_SUBCOMMANDS.none { it.equals(args[1], ignoreCase = true) } -> SCHEDULE_END_EXAMPLES
            args.size == 2 && args[0].equals("whitelist", ignoreCase = true) -> WHITELIST_SUBCOMMANDS
            args.size == 3 && args[0].equals("whitelist", ignoreCase = true) &&
                args[1].equals("add", ignoreCase = true) -> plugin.server.onlinePlayers.map { it.name }
            args.size == 3 && args[0].equals("whitelist", ignoreCase = true) &&
                args[1].equals("remove", ignoreCase = true) -> plugin.maintenanceWhitelist.getPlayers().map { it.name }
            else -> emptyList()
        }

        return suggestions.filter { it.startsWith(args.last(), ignoreCase = true) }
    }

    private fun send(sender: CommandSender, path: String, vararg placeholders: Pair<String, String>) {
        sender.sendMessage(plugin.commandMessages.render(path, mapOf(*placeholders)))
    }

    private fun sendAll(sender: CommandSender, path: String, vararg placeholders: Pair<String, String>) {
        plugin.commandMessages.renderList(path, mapOf(*placeholders)).forEach(sender::sendMessage)
    }

    private fun sendUsage(sender: CommandSender, label: String, usage: String) {
        send(sender, "command.usage.$usage", "label" to label)
    }

    private fun formatScheduleTime(sender: CommandSender, instant: Instant): String =
        if (sender is Player) {
            plugin.maintenanceSchedule.format(instant)
        } else {
            plugin.maintenanceSchedule.formatForConfiguration(instant)
        }

    private companion object {
        val SUBCOMMANDS = listOf("help", "status", "on", "off", "reload", "schedule", "whitelist")
        val SCHEDULE_SUBCOMMANDS = listOf("status", "cancel")
        val SCHEDULE_END_EXAMPLES = listOf("1h", "2h", "1h30m")
        val WHITELIST_SUBCOMMANDS = listOf("add", "remove", "list")
    }
}

internal fun toggleResultMessagePath(
    enabled: Boolean,
    maintenanceChanged: Boolean,
    scheduleCancelled: Boolean,
): String {
    val stateChanged = maintenanceChanged || scheduleCancelled
    return when {
        stateChanged && enabled -> "command.toggle.enabled"
        stateChanged -> "command.toggle.disabled"
        enabled -> "command.toggle.already-enabled"
        else -> "command.toggle.already-disabled"
    }
}
