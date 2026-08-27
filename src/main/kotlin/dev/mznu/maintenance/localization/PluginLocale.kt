package dev.mznu.maintenance.localization

import java.util.IllformedLocaleException
import java.util.Locale

internal const val DEFAULT_LOCALE_TAG = "en"

internal fun parseLocaleTag(value: String): Locale? {
    val languageTag = value.trim()
    if (languageTag.isEmpty()) {
        return null
    }
    return try {
        Locale.Builder().setLanguageTag(languageTag).build()
    } catch (_: IllformedLocaleException) {
        null
    }
}

internal fun localeFallbackTags(locale: Locale): List<String> =
    listOf(locale.toLanguageTag(), locale.language, DEFAULT_LOCALE_TAG)
        .filter(String::isNotBlank)
        .distinct()
