package dev.mznu.maintenance.command

import kotlin.test.Test
import kotlin.test.assertEquals

class MaintenanceCommandInputTest {

    @Test
    fun `parses the root and unknown subcommands`() {
        assertEquals(MaintenanceCommandInput.Root, parse())
        assertEquals(MaintenanceCommandInput.Unknown("unknown"), parse("unknown"))
    }

    @Test
    fun `parses simple subcommands case insensitively`() {
        assertEquals(MaintenanceCommandInput.Help, parse("HELP"))
        assertEquals(MaintenanceCommandInput.Reload, parse("reload"))
        assertEquals(MaintenanceCommandInput.Status, parse("Status"))
        assertEquals(MaintenanceCommandInput.Toggle(true), parse("On"))
        assertEquals(MaintenanceCommandInput.Toggle(false), parse("OFF"))
    }

    @Test
    fun `rejects additional arguments for simple subcommands`() {
        assertEquals(invalid(CommandInputError.ADDITIONAL_ARGUMENTS, CommandUsage.HELP), parse("help", "extra"))
        assertEquals(invalid(CommandInputError.ADDITIONAL_ARGUMENTS, CommandUsage.RELOAD), parse("reload", "extra"))
        assertEquals(invalid(CommandInputError.ADDITIONAL_ARGUMENTS, CommandUsage.STATUS), parse("status", "extra"))
        assertEquals(invalid(CommandInputError.ADDITIONAL_ARGUMENTS, CommandUsage.ON), parse("on", "extra"))
        assertEquals(invalid(CommandInputError.ADDITIONAL_ARGUMENTS, CommandUsage.OFF), parse("off", "extra"))
    }

    @Test
    fun `parses incomplete and complete schedule inputs`() {
        assertEquals(MaintenanceCommandInput.ScheduleMissingTimes, parse("schedule"))
        assertEquals(
            MaintenanceCommandInput.ScheduleStartOnly("2026-09-01T02:00"),
            parse("schedule", "2026-09-01T02:00"),
        )
        assertEquals(
            MaintenanceCommandInput.ScheduleCreate("2026-09-01T02:00", "1h"),
            parse("schedule", "2026-09-01T02:00", "1h"),
        )
    }

    @Test
    fun `parses schedule status and cancel case insensitively`() {
        assertEquals(MaintenanceCommandInput.ScheduleStatus, parse("schedule", "STATUS"))
        assertEquals(MaintenanceCommandInput.ScheduleCancel, parse("schedule", "Cancel"))
    }

    @Test
    fun `rejects excess schedule arguments with specific usage`() {
        assertEquals(
            invalid(CommandInputError.TOO_MANY_ARGUMENTS, CommandUsage.SCHEDULE_STATUS),
            parse("schedule", "status", "extra"),
        )
        assertEquals(
            invalid(CommandInputError.TOO_MANY_ARGUMENTS, CommandUsage.SCHEDULE_CANCEL),
            parse("schedule", "cancel", "extra"),
        )
        assertEquals(
            invalid(CommandInputError.TOO_MANY_ARGUMENTS, CommandUsage.SCHEDULE),
            parse("schedule", "2026-09-01T02:00", "1h", "extra"),
        )
    }

    @Test
    fun `parses missing and unknown whitelist subcommands`() {
        assertEquals(MaintenanceCommandInput.WhitelistMissingSubcommand, parse("whitelist"))
        assertEquals(
            MaintenanceCommandInput.WhitelistUnknownSubcommand("unknown"),
            parse("whitelist", "unknown"),
        )
    }

    @Test
    fun `parses whitelist operations`() {
        assertEquals(MaintenanceCommandInput.WhitelistAdd("Player"), parse("whitelist", "ADD", "Player"))
        assertEquals(MaintenanceCommandInput.WhitelistRemove("Player"), parse("whitelist", "remove", "Player"))
        assertEquals(MaintenanceCommandInput.WhitelistList, parse("whitelist", "list"))
    }

    @Test
    fun `rejects missing and excess whitelist player arguments`() {
        assertEquals(
            invalid(CommandInputError.MISSING_PLAYER, CommandUsage.WHITELIST_ADD),
            parse("whitelist", "add"),
        )
        assertEquals(
            invalid(CommandInputError.MISSING_PLAYER, CommandUsage.WHITELIST_REMOVE),
            parse("whitelist", "remove"),
        )
        assertEquals(
            invalid(CommandInputError.TOO_MANY_ARGUMENTS, CommandUsage.WHITELIST_ADD),
            parse("whitelist", "add", "Player", "extra"),
        )
        assertEquals(
            invalid(CommandInputError.TOO_MANY_ARGUMENTS, CommandUsage.WHITELIST_REMOVE),
            parse("whitelist", "remove", "Player", "extra"),
        )
    }

    @Test
    fun `rejects additional whitelist list arguments`() {
        assertEquals(
            invalid(CommandInputError.ADDITIONAL_ARGUMENTS, CommandUsage.WHITELIST_LIST),
            parse("whitelist", "list", "extra"),
        )
    }

    private fun parse(vararg args: String): MaintenanceCommandInput = parseMaintenanceCommandInput(args.toList())

    private fun invalid(error: CommandInputError, usage: CommandUsage): MaintenanceCommandInput =
        MaintenanceCommandInput.Invalid(error, usage)
}
