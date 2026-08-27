package dev.mznu.maintenance.command

import kotlin.test.Test
import kotlin.test.assertEquals

class MaintenanceToggleResultTest {

    @Test
    fun `reports the requested state when a schedule was cancelled`() {
        assertEquals(
            "command.toggle.disabled",
            toggleResultMessagePath(enabled = false, maintenanceChanged = false, scheduleCancelled = true),
        )
        assertEquals(
            "command.toggle.enabled",
            toggleResultMessagePath(enabled = true, maintenanceChanged = false, scheduleCancelled = true),
        )
    }

    @Test
    fun `reports an unchanged state only when neither maintenance nor schedule changed`() {
        assertEquals(
            "command.toggle.already-disabled",
            toggleResultMessagePath(enabled = false, maintenanceChanged = false, scheduleCancelled = false),
        )
        assertEquals(
            "command.toggle.already-enabled",
            toggleResultMessagePath(enabled = true, maintenanceChanged = false, scheduleCancelled = false),
        )
    }

    @Test
    fun `reports the requested state when maintenance mode changed`() {
        assertEquals(
            "command.toggle.disabled",
            toggleResultMessagePath(enabled = false, maintenanceChanged = true, scheduleCancelled = false),
        )
        assertEquals(
            "command.toggle.enabled",
            toggleResultMessagePath(enabled = true, maintenanceChanged = true, scheduleCancelled = false),
        )
    }
}
