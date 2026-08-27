package dev.mznu.maintenance.listener

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import com.destroystokyo.paper.event.server.PaperServerListPingEvent.ListedPlayerInfo
import dev.mznu.maintenance.MaintenancePlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.nio.charset.StandardCharsets
import java.util.UUID

class ServerListPingListener(
    private val plugin: MaintenancePlugin,
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onServerListPing(event: PaperServerListPingEvent) {
        plugin.serverListMotd()?.let(event::motd)
        if (!plugin.isMaintenanceEnabled) {
            return
        }

        val settings = plugin.serverListSettings
        settings.playerCountText?.let {
            event.protocolVersion = MAINTENANCE_PROTOCOL_VERSION
            event.version = it
        }
        settings.playerListHoverLines?.let { lines ->
            event.listedPlayers.clear()
            lines.forEachIndexed { index, line ->
                event.listedPlayers += ListedPlayerInfo(line, hoverLineId(index))
            }
        }
        settings.icon?.let(event::setServerIcon)
    }

    private fun hoverLineId(index: Int): UUID = UUID.nameUUIDFromBytes(
        "maintenance-player-list-hover-$index".toByteArray(StandardCharsets.UTF_8),
    )

    private companion object {
        const val MAINTENANCE_PROTOCOL_VERSION = -1
    }
}
