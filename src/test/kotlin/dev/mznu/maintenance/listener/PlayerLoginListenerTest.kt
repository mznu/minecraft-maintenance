package dev.mznu.maintenance.listener

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerLoginListenerTest {

    private val playerId = UUID.fromString("12345678-1234-1234-1234-123456789abc")

    @Test
    fun `allows only connections with a whitelisted player id`() {
        assertTrue(isMaintenanceAccessAllowed(playerId) { it == playerId })
        assertFalse(isMaintenanceAccessAllowed(playerId) { false })
    }

    @Test
    fun `does not allow a connection without an authenticated player id`() {
        assertFalse(isMaintenanceAccessAllowed(null) { true })
    }
}
