package dev.mznu.maintenance.message

import dev.mznu.maintenance.MaintenancePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class CommandMessages private constructor(
    private val configuration: YamlConfiguration,
    private val miniMessage: MiniMessage,
    private val internalMessages: InternalMessages,
) {

    fun render(path: String, placeholders: Map<String, String> = emptyMap()): Component = miniMessage.deserialize(
        template(path),
        *placeholders.map { (name, value) -> Placeholder.unparsed(name, value) }.toTypedArray(),
    )

    fun renderList(path: String, placeholders: Map<String, String> = emptyMap()): List<Component> {
        val resolvers = placeholders
            .map { (name, value) -> Placeholder.unparsed(name, value) }
            .toTypedArray()
        return templateList(path).map { miniMessage.deserialize(it, *resolvers) }
    }

    private fun template(path: String): String = configuration.getString(path)
        ?: error(internalMessages.format("command-messages.missing-message", "path" to path))

    private fun templateList(path: String): List<String> = configuration
        .takeIf { it.isList(path) }
        ?.getStringList(path)
        ?: error(internalMessages.format("command-messages.missing-message-list", "path" to path))

    companion object {
        private const val RESOURCE_PATH = "command-messages.yml"

        fun load(plugin: MaintenancePlugin, miniMessage: MiniMessage): CommandMessages {
            val resource = checkNotNull(plugin.getResource(RESOURCE_PATH)) {
                plugin.internalMessages.format("command-messages.bundled-file-missing", "path" to RESOURCE_PATH)
            }
            val configuration = YamlConfiguration()
            InputStreamReader(resource, StandardCharsets.UTF_8).use(configuration::load)
            return CommandMessages(configuration, miniMessage, plugin.internalMessages)
        }
    }
}
