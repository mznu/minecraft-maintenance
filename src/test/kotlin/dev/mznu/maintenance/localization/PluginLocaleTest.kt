package dev.mznu.maintenance.localization

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PluginLocaleTest {

    @Test
    fun `parses BCP 47 plugin locales`() {
        assertEquals("en", parseLocaleTag("en")?.toLanguageTag())
        assertEquals("ko-KR", parseLocaleTag("ko-KR")?.toLanguageTag())
        assertNull(parseLocaleTag(""))
        assertNull(parseLocaleTag("ko_KR"))
    }

    @Test
    fun `falls back from region to language and then English`() {
        assertEquals(listOf("en"), localeFallbackTags(Locale.ENGLISH))
        assertEquals(listOf("en-US", "en"), localeFallbackTags(Locale.forLanguageTag("en-US")))
        assertEquals(listOf("ko-KR", "ko", "en"), localeFallbackTags(Locale.forLanguageTag("ko-KR")))
    }
}
