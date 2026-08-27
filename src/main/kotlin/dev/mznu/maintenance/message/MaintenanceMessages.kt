package dev.mznu.maintenance.message

import dev.mznu.maintenance.MaintenancePlugin
import dev.mznu.maintenance.localization.DEFAULT_LOCALE_TAG
import dev.mznu.maintenance.localization.localeFallbackTags
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale

class MaintenanceMessages private constructor(
    private val configurations: List<YamlConfiguration>,
    private val miniMessage: MiniMessage,
    private val internalMessages: InternalMessages,
) {

    fun render(path: String, placeholders: Map<String, String> = emptyMap()): Component = miniMessage.deserialize(
        template(path),
        *placeholders.map { (name, value) -> Placeholder.unparsed(name, value) }.toTypedArray(),
    )

    fun renderOptional(path: String, placeholders: Map<String, String> = emptyMap()): Component? =
        template(path).takeIf(String::isNotBlank)?.let {
            miniMessage.deserialize(
                it,
                *placeholders.map { (name, value) -> Placeholder.unparsed(name, value) }.toTypedArray(),
            )
        }

    fun renderList(path: String, placeholders: Map<String, String> = emptyMap()): List<Component> {
        val resolvers = placeholders
            .map { (name, value) -> Placeholder.unparsed(name, value) }
            .toTypedArray()
        return templateList(path).map { miniMessage.deserialize(it, *resolvers) }
    }

    fun template(path: String): String = configurations
        .firstNotNullOfOrNull { it.getString(path) }
        ?: error(internalMessages.format("language.missing-message", "path" to path))

    private fun templateList(path: String): List<String> = configurations
        .firstOrNull { it.isList(path) }
        ?.getStringList(path)
        ?: error(internalMessages.format("language.missing-message-list", "path" to path))

    companion object {
        private const val LANGUAGE_DIRECTORY = "lang"

        fun load(plugin: MaintenancePlugin, miniMessage: MiniMessage, locale: Locale): MaintenanceMessages {
            val requestedTag = locale.toLanguageTag()
            val loadedConfigurations = localeFallbackTags(locale).mapNotNull { tag ->
                loadLanguageFile(plugin, tag)?.let { tag to it }
            }
            check(loadedConfigurations.isNotEmpty()) {
                plugin.internalMessages.format("language.unavailable")
            }

            val resolvedTag = loadedConfigurations.first().first
            if (resolvedTag == DEFAULT_LOCALE_TAG && locale.language != DEFAULT_LOCALE_TAG) {
                plugin.logger.warning(
                    plugin.internalMessages.format(
                        "language.fallback",
                        "locale" to requestedTag,
                        "fallback" to DEFAULT_LOCALE_TAG,
                    ),
                )
            }
            val bundledEnglish = loadBundledLanguageFile(plugin, DEFAULT_LOCALE_TAG)
            return MaintenanceMessages(
                loadedConfigurations.map { it.second } + bundledEnglish,
                miniMessage,
                plugin.internalMessages,
            )
        }

        private fun loadLanguageFile(plugin: MaintenancePlugin, languageTag: String): YamlConfiguration? {
            val resourcePath = "$LANGUAGE_DIRECTORY/$languageTag.yml"
            val languageFile = File(plugin.dataFolder, resourcePath)
            if (!languageFile.exists()) {
                val bundledResource = plugin.getResource(resourcePath) ?: return null
                bundledResource.close()
                plugin.saveResource(resourcePath, false)
            }

            val configuration = YamlConfiguration()
            configuration.load(languageFile)
            return configuration
        }

        private fun loadBundledLanguageFile(plugin: MaintenancePlugin, languageTag: String): YamlConfiguration {
            val resourcePath = "$LANGUAGE_DIRECTORY/$languageTag.yml"
            val resource = checkNotNull(plugin.getResource(resourcePath)) {
                plugin.internalMessages.format("language.bundled-file-missing", "path" to resourcePath)
            }
            return YamlConfiguration().also { configuration ->
                InputStreamReader(resource, StandardCharsets.UTF_8).use(configuration::load)
            }
        }
    }
}
