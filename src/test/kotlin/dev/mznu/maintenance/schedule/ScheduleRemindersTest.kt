package dev.mznu.maintenance.schedule

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleRemindersTest {

    @Test
    fun `returns only newly reached reminder periods`() {
        val oneHour = Duration.ofHours(1)
        val thirtyMinutes = Duration.ofMinutes(30)
        val tenMinutes = Duration.ofMinutes(10)
        val periods = listOf(oneHour, thirtyMinutes, tenMinutes)

        assertEquals(
            listOf(thirtyMinutes),
            dueReminderPeriods(periods, setOf(oneHour), Duration.ofMinutes(30)),
        )
        assertEquals(
            emptyList(),
            dueReminderPeriods(periods, setOf(oneHour, thirtyMinutes), Duration.ofMinutes(29)),
        )
        assertEquals(
            listOf(thirtyMinutes, tenMinutes),
            dueReminderPeriods(periods, setOf(oneHour), Duration.ofMinutes(9)),
        )
    }
}
