package dev.mznu.maintenance.message

import dev.mznu.maintenance.MaintenancePlugin
import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class InternalMessages private constructor(
    private val configuration: YamlConfiguration,
) {

    fun format(path: String, vararg placeholders: Pair<String, Any?>): String {
        val template = checkNotNull(configuration.getString(path)) { path }
        return placeholders.fold(template) { message, (name, value) ->
            message.replace("<$name>", value.toString())
        }
    }

    companion object {
        private const val RESOURCE_PATH = "internal-messages.yml"

        fun load(plugin: MaintenancePlugin): InternalMessages {
            val resource = checkNotNull(plugin.getResource(RESOURCE_PATH)) { RESOURCE_PATH }
            val configuration = YamlConfiguration()
            InputStreamReader(resource, StandardCharsets.UTF_8).use(configuration::load)
            return InternalMessages(configuration)
        }
    }
}
