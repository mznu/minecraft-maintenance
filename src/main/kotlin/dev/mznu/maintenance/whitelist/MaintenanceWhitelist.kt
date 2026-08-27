package dev.mznu.maintenance.whitelist

import dev.mznu.maintenance.MaintenancePlugin
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class MaintenanceWhitelist(
    private val plugin: MaintenancePlugin,
) {

    private val whitelistFile = File(plugin.dataFolder, FILE_NAME)

    @Volatile
    private var players: Map<UUID, String> = loadPlayers()

    private fun loadPlayers(): Map<UUID, String> {
        if (!whitelistFile.exists()) {
            plugin.saveResource(FILE_NAME, false)
        }

        val reloadedConfig = YamlConfiguration()
        reloadedConfig.load(whitelistFile)
        val reloadedPlayers = linkedMapOf<UUID, String>()
        for (entry in reloadedConfig.getKeys(false)) {
            loadPlayer(reloadedPlayers, entry, reloadedConfig.getString(entry))
        }
        return reloadedPlayers.toMap()
    }

    fun contains(playerId: UUID): Boolean = playerId in players

    @Throws(IOException::class)
    fun add(playerId: UUID, playerName: String): Boolean {
        val updatedPlayers = LinkedHashMap(players)
        val previousName = updatedPlayers.put(playerId, playerName)
        if (previousName != playerName) {
            save(updatedPlayers)
            players = updatedPlayers.toMap()
        }
        return previousName == null
    }

    @Throws(IOException::class)
    fun remove(playerName: String): WhitelistedPlayer? {
        val entry = players.entries.firstOrNull {
            it.value.equals(playerName, ignoreCase = true) || it.key.toString().equals(playerName, ignoreCase = true)
        } ?: return null

        val updatedPlayers = LinkedHashMap(players)
        updatedPlayers.remove(entry.key)
        save(updatedPlayers)
        players = updatedPlayers.toMap()
        return WhitelistedPlayer(entry.key, entry.value)
    }

    fun getPlayers(): List<WhitelistedPlayer> = players
        .map { WhitelistedPlayer(it.key, it.value) }
        .sortedBy { it.name.lowercase() }

    private fun loadPlayer(players: MutableMap<UUID, String>, entry: String, playerName: String?) {
        val playerId = parseUuid(entry) ?: return
        if (playerName.isNullOrBlank()) {
            plugin.logger.warning(
                plugin.internalMessages.format("whitelist.missing-player-name", "entry" to entry),
            )
            return
        }
        players[playerId] = playerName
    }

    private fun parseUuid(value: String): UUID? = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        plugin.logger.warning(plugin.internalMessages.format("whitelist.invalid-uuid", "uuid" to value))
        null
    }

    private fun save(players: Map<UUID, String>) {
        val serializedPlayers = players.entries
            .sortedBy { it.value.lowercase() }
            .associateTo(linkedMapOf()) { it.key.toString() to it.value }
        val configuration = YamlConfiguration()
        for ((playerId, playerName) in serializedPlayers) {
            configuration.set(playerId, playerName)
        }

        val temporaryFile = Files.createTempFile(whitelistFile.parentFile.toPath(), "whitelist-", ".tmp")
        try {
            configuration.save(temporaryFile.toFile())
            try {
                Files.move(
                    temporaryFile,
                    whitelistFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporaryFile, whitelistFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporaryFile)
        }
    }

    data class WhitelistedPlayer(
        val id: UUID,
        val name: String,
    )

    private companion object {
        const val FILE_NAME = "whitelist.yml"
    }
}
