package dev.mznu.maintenance

import dev.mznu.maintenance.command.MaintenanceCommand
import dev.mznu.maintenance.listener.PlayerLoginListener
import dev.mznu.maintenance.listener.ServerListPingListener
import dev.mznu.maintenance.localization.DEFAULT_LOCALE_TAG
import dev.mznu.maintenance.localization.parseLocaleTag
import dev.mznu.maintenance.message.CommandMessages
import dev.mznu.maintenance.message.InternalMessages
import dev.mznu.maintenance.message.MaintenanceMessages
import dev.mznu.maintenance.schedule.MaintenanceSchedule
import dev.mznu.maintenance.serverlist.ServerListSettings
import dev.mznu.maintenance.whitelist.MaintenanceWhitelist
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.time.Instant
import java.util.Locale

class MaintenancePlugin : JavaPlugin() {

    private val miniMessage = MiniMessage.miniMessage()

    @Volatile
    var isMaintenanceEnabled: Boolean = false
        private set

    @Volatile
    lateinit var maintenanceWhitelist: MaintenanceWhitelist
        private set

    @Volatile
    lateinit var serverListSettings: ServerListSettings
        private set

    @Volatile
    lateinit var maintenanceSchedule: MaintenanceSchedule
        private set

    @Volatile
    lateinit var messages: MaintenanceMessages
        private set

    lateinit var internalMessages: InternalMessages
        private set

    lateinit var commandMessages: CommandMessages
        private set

    @Volatile
    lateinit var locale: Locale
        private set

    override fun onEnable() {
        saveDefaultConfig()
        internalMessages = InternalMessages.load(this)
        commandMessages = CommandMessages.load(this, miniMessage)
        locale = loadLocale()
        messages = MaintenanceMessages.load(this, miniMessage, locale)
        maintenanceWhitelist = MaintenanceWhitelist(this)
        serverListSettings = ServerListSettings.load(this, messages)
        isMaintenanceEnabled = config.getBoolean("enabled")
        maintenanceSchedule = MaintenanceSchedule(this)
        maintenanceSchedule.start()

        val maintenanceCommand = checkNotNull(getCommand("maintenance")) {
            internalMessages.format("plugin.command-missing")
        }
        val commandHandler = MaintenanceCommand(this)
        maintenanceCommand.setExecutor(commandHandler)
        maintenanceCommand.tabCompleter = commandHandler
        server.pluginManager.registerEvents(PlayerLoginListener(this), this)
        server.pluginManager.registerEvents(ServerListPingListener(this), this)

        logger.info(internalMessages.format("plugin.enabled", "enabled" to isMaintenanceEnabled))
    }

    override fun onDisable() {
        if (::maintenanceSchedule.isInitialized) {
            maintenanceSchedule.stop()
        }
        if (::internalMessages.isInitialized) {
            logger.info(internalMessages.format("plugin.disabled"))
        }
    }

    fun setMaintenanceEnabled(enabled: Boolean): Boolean {
        if (isMaintenanceEnabled == enabled) {
            if (config.getBoolean("enabled") != enabled) {
                config.set("enabled", enabled)
                saveConfig()
            }
            return false
        }

        isMaintenanceEnabled = enabled
        config.set("enabled", enabled)
        saveConfig()

        if (enabled) {
            kickNonWhitelistedPlayers()
        }
        return true
    }

    fun reloadMaintenanceConfiguration() {
        val previousConfiguration = config.saveToString()
        val previousSchedule = maintenanceSchedule
        val previousWhitelist = maintenanceWhitelist
        val previousLocale = locale
        val previousMessages = messages
        val previousServerListSettings = serverListSettings
        val previousEnabled = isMaintenanceEnabled
        var reloadedSchedule: MaintenanceSchedule? = null
        var previousScheduleStopped = false

        try {
            validateConfigurationFile()
            reloadConfig()

            val reloadedLocale = loadLocale()
            val reloadedMessages = MaintenanceMessages.load(this, miniMessage, reloadedLocale)
            val reloadedServerListSettings = ServerListSettings.load(this, reloadedMessages)
            val reloadedWhitelist = MaintenanceWhitelist(this)
            val configuredEnabled = config.getBoolean("enabled")
            val nextSchedule = MaintenanceSchedule(this, reloadedLocale, reloadedMessages)
            reloadedSchedule = nextSchedule
            val reloadedEnabled = nextSchedule.maintenanceStateAt() ?: configuredEnabled

            previousSchedule.stop()
            previousScheduleStopped = true
            locale = reloadedLocale
            messages = reloadedMessages
            serverListSettings = reloadedServerListSettings
            maintenanceWhitelist = reloadedWhitelist
            maintenanceSchedule = nextSchedule
            setMaintenanceEnabled(reloadedEnabled)
            nextSchedule.start()
            if (isMaintenanceEnabled) {
                kickNonWhitelistedPlayers()
            }
        } catch (exception: Exception) {
            reloadedSchedule?.stop()
            try {
                config.loadFromString(previousConfiguration)
            } catch (rollbackException: Exception) {
                exception.addSuppressed(rollbackException)
            }
            locale = previousLocale
            messages = previousMessages
            serverListSettings = previousServerListSettings
            maintenanceWhitelist = previousWhitelist
            maintenanceSchedule = previousSchedule
            isMaintenanceEnabled = previousEnabled
            if (previousScheduleStopped) {
                try {
                    previousSchedule.start()
                } catch (rollbackException: Exception) {
                    exception.addSuppressed(rollbackException)
                }
            }
            throw exception
        }

        logger.info(internalMessages.format("plugin.reloaded", "enabled" to isMaintenanceEnabled))
    }

    private fun validateConfigurationFile() {
        YamlConfiguration().load(File(dataFolder, CONFIG_FILE_NAME))
    }

    fun maintenanceKickMessage(): Component = messages.render("maintenance.kick-message")

    fun serverListMotd(): Component? {
        val settings = serverListSettings
        val schedule = maintenanceSchedule
        val window = schedule.currentWindow
        val motd = settings.motd.select(isMaintenanceEnabled, window, Instant.now()) ?: return null

        if (window == null) {
            return miniMessage.deserialize(motd)
        }
        return miniMessage.deserialize(
            motd,
            Placeholder.unparsed("schedule_start", schedule.format(window.startsAt)),
            Placeholder.unparsed("schedule_end", schedule.format(window.endsAt)),
        )
    }

    private fun kickNonWhitelistedPlayers() {
        val kickMessage = maintenanceKickMessage()
        val playersToKick = server.onlinePlayers
            .filterNot { maintenanceWhitelist.contains(it.uniqueId) }
        if (playersToKick.isEmpty()) {
            return
        }

        playersToKick.forEach { it.kick(kickMessage) }
        logger.info(internalMessages.format("plugin.players-kicked", "count" to playersToKick.size))
    }

    private fun loadLocale(): Locale {
        val languageTag = config.getString(LOCALE_PATH) ?: DEFAULT_LOCALE_TAG
        return parseLocaleTag(languageTag) ?: run {
            logger.warning(
                internalMessages.format(
                    "plugin.invalid-locale",
                    "locale" to languageTag,
                    "fallback" to DEFAULT_LOCALE_TAG,
                ),
            )
            Locale.ENGLISH
        }
    }

    private companion object {
        const val CONFIG_FILE_NAME = "config.yml"
        const val LOCALE_PATH = "locale"
    }
}
