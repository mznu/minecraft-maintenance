package dev.mznu.maintenance.serverlist

import dev.mznu.maintenance.MaintenancePlugin
import dev.mznu.maintenance.message.MaintenanceMessages
import dev.mznu.maintenance.schedule.ScheduleDurationParser
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.util.CachedServerIcon
import java.io.File
import java.time.Duration

data class ServerListSettings(
    val motd: ServerListMotdSettings,
    val playerCountText: String?,
    val playerListHoverLines: List<String>?,
    val icon: CachedServerIcon?,
) {

    companion object {
        private const val MOTD_PATH = "server-list.motd"
        private const val PLAYER_COUNT_PATH = "server-list.player-count"
        private const val PLAYER_LIST_HOVER_PATH = "server-list.player-list-hover"
        private const val ICON_PATH = "server-list.icon"

        fun load(plugin: MaintenancePlugin, messages: MaintenanceMessages): ServerListSettings {
            val legacySerializer = LegacyComponentSerializer.legacySection()
            val manualMotd = messages.template("$MOTD_PATH.manual").takeIf(String::isNotBlank)
            val scheduledMotd = messages.template("$MOTD_PATH.scheduled").takeIf(String::isNotBlank)
            val upcomingMotd = messages.template("$MOTD_PATH.upcoming").takeIf(String::isNotBlank)
            val upcomingMotdShowBefore = plugin.config.getString("$MOTD_PATH.upcoming.show-before")
                ?.trim()
                .orEmpty()
            val upcomingMotdNoticePeriod = upcomingMotdShowBefore
                .takeIf(String::isNotEmpty)
                ?.let(ScheduleDurationParser::parse)
                ?.takeUnless(Duration::isZero)
            if (upcomingMotdShowBefore.isNotEmpty() && upcomingMotdNoticePeriod == null) {
                plugin.logger.warning(plugin.internalMessages.format("server-list.invalid-upcoming-period"))
            }

            val playerCountText = messages.renderOptional(PLAYER_COUNT_PATH)
                ?.let(legacySerializer::serialize)

            val playerListHoverLines = messages.renderList(PLAYER_LIST_HOVER_PATH)
                .map(legacySerializer::serialize)
                .takeIf(List<String>::isNotEmpty)

            val icon = loadIcon(plugin)
            return ServerListSettings(
                ServerListMotdSettings(
                    manualMotd,
                    scheduledMotd,
                    upcomingMotd,
                    upcomingMotdNoticePeriod,
                ),
                playerCountText,
                playerListHoverLines,
                icon,
            )
        }

        private fun loadIcon(plugin: MaintenancePlugin): CachedServerIcon? {
            val fileName = plugin.config.getString("$ICON_PATH.file")?.trim().orEmpty()
            if (fileName.isEmpty()) {
                return null
            }

            val iconFile = File(plugin.dataFolder, fileName)
            if (!iconFile.isFile) {
                plugin.logger.warning(
                    plugin.internalMessages.format("server-list.icon-missing", "path" to iconFile.path),
                )
                return null
            }

            return try {
                plugin.server.loadServerIcon(iconFile)
            } catch (exception: Exception) {
                plugin.logger.warning(
                    plugin.internalMessages.format(
                        "server-list.icon-load-failed",
                        "path" to iconFile.path,
                        "error" to exception.message,
                    ),
                )
                null
            }
        }
    }
}
