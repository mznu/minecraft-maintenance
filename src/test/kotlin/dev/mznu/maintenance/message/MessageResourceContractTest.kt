package dev.mznu.maintenance.message

import dev.mznu.maintenance.command.CommandUsage
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageResourceContractTest {

    @Test
    fun `localized files have the expected matching schema without command messages`() {
        val english = loadYaml("lang/en.yml")
        val korean = loadYaml("lang/ko.yml")

        assertFalse(english.contains("command"))
        assertFalse(korean.contains("command"))
        assertEquals(LOCALIZED_MESSAGE_KEYS, leafKeys(english))
        assertEquals(LOCALIZED_MESSAGE_KEYS, leafKeys(korean))
    }

    @Test
    fun `all internal and command message keys match their code references`() {
        val source = readKotlinSource()
        val internalKeys = leafKeys(loadYaml("internal-messages.yml"))
        val internalReferences = INTERNAL_MESSAGE_REFERENCE.findAll(source)
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(internalKeys, internalReferences)

        val commandReferences = COMMAND_MESSAGE_REFERENCE.findAll(source)
            .map { it.groupValues[1] }
            .filterNot { it in internalKeys }
            .toMutableSet()
            .apply { addAll(CommandUsage.entries.map { "command.usage.${it.key}" }) }
        assertEquals(leafKeys(loadYaml("command-messages.yml")), commandReferences)
    }

    @Test
    fun `all player-facing message templates can be parsed by MiniMessage`() {
        validateMiniMessageTemplates(loadYaml("command-messages.yml"))
        validateMiniMessageTemplates(loadYaml("lang/en.yml"))
        validateMiniMessageTemplates(loadYaml("lang/ko.yml"))
    }

    private fun loadYaml(resourcePath: String): YamlConfiguration {
        val resource = checkNotNull(javaClass.classLoader.getResourceAsStream(resourcePath)) {
            "Missing test resource: $resourcePath"
        }
        return YamlConfiguration().also { configuration ->
            InputStreamReader(resource, StandardCharsets.UTF_8).use(configuration::load)
        }
    }

    private fun leafKeys(configuration: YamlConfiguration): Set<String> = configuration
        .getKeys(true)
        .filterNot(configuration::isConfigurationSection)
        .toSet()

    private fun readKotlinSource(): String {
        val sourceRoot = Path.of("src/main/kotlin")
        val sourceFiles = Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }.toList()
        }
        return sourceFiles.joinToString("\n") { Files.readString(it) }
    }

    private fun validateMiniMessageTemplates(configuration: YamlConfiguration) {
        for (path in leafKeys(configuration)) {
            val values = when (val value = configuration.get(path)) {
                is String -> listOf(value)
                is List<*> -> {
                    assertTrue(value.all { it is String }, "Message list '$path' must contain only strings")
                    value.filterIsInstance<String>()
                }
                else -> error("Message '$path' must be a string or list of strings")
            }
            values.forEach { template ->
                MINI_MESSAGE.deserialize(template, *PLACEHOLDER_RESOLVERS)
            }
        }
    }

    private companion object {
        val INTERNAL_MESSAGE_REFERENCE = Regex("internalMessages\\.format\\(\\s*\"([^\"]+)\"")
        val COMMAND_MESSAGE_REFERENCE = Regex("\"(command\\.[a-z0-9.-]+)\"")

        val LOCALIZED_MESSAGE_KEYS = setOf(
            "maintenance.kick-message",
            "duration.separator",
            "duration.day.one",
            "duration.day.other",
            "duration.hour.one",
            "duration.hour.other",
            "duration.minute.one",
            "duration.minute.other",
            "duration.second.one",
            "duration.second.other",
            "schedule.display-format",
            "schedule.notifications.scheduled-message",
            "schedule.notifications.reminder-message",
            "server-list.motd.manual",
            "server-list.motd.scheduled",
            "server-list.motd.upcoming",
            "server-list.player-count",
            "server-list.player-list-hover",
        )

        val PLACEHOLDER_RESOLVERS = setOf(
            "input",
            "label",
            "player",
            "players",
            "schedule_end",
            "schedule_start",
            "time_until_start",
            "value",
            "version",
        ).map { Placeholder.unparsed(it, "value") }.toTypedArray()

        val MINI_MESSAGE = MiniMessage.miniMessage()
    }
}
