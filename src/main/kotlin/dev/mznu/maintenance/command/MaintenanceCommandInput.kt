package dev.mznu.maintenance.command

internal sealed interface MaintenanceCommandInput {

    data object Root : MaintenanceCommandInput
    data object Help : MaintenanceCommandInput
    data object Status : MaintenanceCommandInput
    data class Toggle(val enabled: Boolean) : MaintenanceCommandInput
    data object Reload : MaintenanceCommandInput
    data class Unknown(val value: String) : MaintenanceCommandInput
    data class Invalid(
        val error: CommandInputError,
        val usage: CommandUsage,
    ) : MaintenanceCommandInput

    data object ScheduleMissingTimes : MaintenanceCommandInput
    data class ScheduleStartOnly(val value: String) : MaintenanceCommandInput
    data object ScheduleStatus : MaintenanceCommandInput
    data object ScheduleCancel : MaintenanceCommandInput
    data class ScheduleCreate(
        val start: String,
        val end: String,
    ) : MaintenanceCommandInput

    data object WhitelistMissingSubcommand : MaintenanceCommandInput
    data class WhitelistUnknownSubcommand(val value: String) : MaintenanceCommandInput
    data class WhitelistAdd(val player: String) : MaintenanceCommandInput
    data class WhitelistRemove(val player: String) : MaintenanceCommandInput
    data object WhitelistList : MaintenanceCommandInput
}

internal enum class CommandInputError(
    val messagePath: String,
) {
    ADDITIONAL_ARGUMENTS("command.additional-arguments"),
    TOO_MANY_ARGUMENTS("command.too-many-arguments"),
    MISSING_PLAYER("command.whitelist.missing-player"),
}

internal enum class CommandUsage(
    val key: String,
) {
    HELP("help"),
    STATUS("status"),
    ON("on"),
    OFF("off"),
    RELOAD("reload"),
    SCHEDULE("schedule"),
    SCHEDULE_STATUS("schedule-status"),
    SCHEDULE_CANCEL("schedule-cancel"),
    WHITELIST("whitelist"),
    WHITELIST_ADD("whitelist-add"),
    WHITELIST_REMOVE("whitelist-remove"),
    WHITELIST_LIST("whitelist-list"),
}

internal fun parseMaintenanceCommandInput(args: List<String>): MaintenanceCommandInput {
    if (args.isEmpty()) {
        return MaintenanceCommandInput.Root
    }

    return when (args[0].lowercase()) {
        "help" -> args.withoutAdditionalArguments(MaintenanceCommandInput.Help, CommandUsage.HELP)
        "status" -> args.withoutAdditionalArguments(MaintenanceCommandInput.Status, CommandUsage.STATUS)
        "on" -> args.withoutAdditionalArguments(MaintenanceCommandInput.Toggle(true), CommandUsage.ON)
        "off" -> args.withoutAdditionalArguments(MaintenanceCommandInput.Toggle(false), CommandUsage.OFF)
        "reload" -> args.withoutAdditionalArguments(MaintenanceCommandInput.Reload, CommandUsage.RELOAD)
        "schedule" -> parseScheduleInput(args)
        "whitelist" -> parseWhitelistInput(args)
        else -> MaintenanceCommandInput.Unknown(args[0])
    }
}

private fun List<String>.withoutAdditionalArguments(
    input: MaintenanceCommandInput,
    usage: CommandUsage,
): MaintenanceCommandInput = if (size == 1) {
    input
} else {
    MaintenanceCommandInput.Invalid(CommandInputError.ADDITIONAL_ARGUMENTS, usage)
}

private fun parseScheduleInput(args: List<String>): MaintenanceCommandInput = when {
    args.size == 1 -> MaintenanceCommandInput.ScheduleMissingTimes
    args.size == 2 && args[1].equals("status", ignoreCase = true) -> MaintenanceCommandInput.ScheduleStatus
    args.size == 2 && args[1].equals("cancel", ignoreCase = true) -> MaintenanceCommandInput.ScheduleCancel
    args.size == 2 -> MaintenanceCommandInput.ScheduleStartOnly(args[1])
    args[1].equals("status", ignoreCase = true) ->
        MaintenanceCommandInput.Invalid(CommandInputError.TOO_MANY_ARGUMENTS, CommandUsage.SCHEDULE_STATUS)
    args[1].equals("cancel", ignoreCase = true) ->
        MaintenanceCommandInput.Invalid(CommandInputError.TOO_MANY_ARGUMENTS, CommandUsage.SCHEDULE_CANCEL)
    args.size == 3 -> MaintenanceCommandInput.ScheduleCreate(args[1], args[2])
    else -> MaintenanceCommandInput.Invalid(CommandInputError.TOO_MANY_ARGUMENTS, CommandUsage.SCHEDULE)
}

private fun parseWhitelistInput(args: List<String>): MaintenanceCommandInput {
    if (args.size == 1) {
        return MaintenanceCommandInput.WhitelistMissingSubcommand
    }

    return when (args[1].lowercase()) {
        "add" -> parseWhitelistPlayerInput(args, CommandUsage.WHITELIST_ADD, MaintenanceCommandInput::WhitelistAdd)
        "remove" ->
            parseWhitelistPlayerInput(args, CommandUsage.WHITELIST_REMOVE, MaintenanceCommandInput::WhitelistRemove)
        "list" -> if (args.size == 2) {
            MaintenanceCommandInput.WhitelistList
        } else {
            MaintenanceCommandInput.Invalid(CommandInputError.ADDITIONAL_ARGUMENTS, CommandUsage.WHITELIST_LIST)
        }
        else -> MaintenanceCommandInput.WhitelistUnknownSubcommand(args[1])
    }
}

private fun parseWhitelistPlayerInput(
    args: List<String>,
    usage: CommandUsage,
    createInput: (String) -> MaintenanceCommandInput,
): MaintenanceCommandInput = when {
    args.size < 3 -> MaintenanceCommandInput.Invalid(CommandInputError.MISSING_PLAYER, usage)
    args.size > 3 -> MaintenanceCommandInput.Invalid(CommandInputError.TOO_MANY_ARGUMENTS, usage)
    else -> createInput(args[2])
}
