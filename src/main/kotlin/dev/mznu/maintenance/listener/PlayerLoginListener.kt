package dev.mznu.maintenance.listener

import dev.mznu.maintenance.MaintenancePlugin
import io.papermc.paper.connection.PlayerConfigurationConnection
import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.UUID

class PlayerLoginListener(
    private val plugin: MaintenancePlugin,
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerConnectionValidateLogin(event: PlayerConnectionValidateLoginEvent) {
        if (!plugin.isMaintenanceEnabled) {
            return
        }

        if (isMaintenanceAccessAllowed(event.authenticatedPlayerId(), plugin.maintenanceWhitelist::contains)) {
            return
        }

        event.kickMessage(plugin.maintenanceKickMessage())
    }

    private fun PlayerConnectionValidateLoginEvent.authenticatedPlayerId(): UUID? = when (val connection = connection) {
        is PlayerConfigurationConnection -> connection.profile.id
        is PlayerLoginConnection -> connection.authenticatedProfile?.id
        else -> null
    }
}

internal fun isMaintenanceAccessAllowed(playerId: UUID?, isWhitelisted: (UUID) -> Boolean): Boolean =
    playerId?.let(isWhitelisted) == true
