package com.dpinta.agenda.domain

import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkedHoursTest {

    @Test fun `semana de lunes a domingo y quincenas`() {
        assertEquals(DateRange(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 27)), PayPeriods.week(LocalDate.of(2026, 9, 24)))
        assertEquals(DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15)), PayPeriods.fortnight(LocalDate.of(2026, 9, 15)))
        assertEquals(DateRange(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 28)), PayPeriods.fortnight(LocalDate.of(2026, 2, 16)))
    }

    @Test fun `suma solo los turnos y recorta los que cruzan el borde`() {
        val sat = LocalDate.of(2026, 9, 26)
        val occ = listOf(
            Occurrence(2, 20, LocalDate.of(2026, 9, 23).atTime(10, 15), LocalDate.of(2026, 9, 23).atTime(16, 0)), // 5 h 45
            Occurrence(1, 10, LocalDate.of(2026, 9, 23).atTime(8, 0), LocalDate.of(2026, 9, 23).atTime(10, 0)), // clase
            Occurrence(2, 20, sat.plusDays(1).atTime(20, 0), sat.plusDays(2).atTime(2, 0)), // domingo 20:00 → lunes 2:00
        )
        val total = WorkedHours.total(occ, { it == 2L }, PayPeriods.week(sat))
        assertEquals(Duration.ofHours(5).plusMinutes(45).plusHours(4), total)
    }
}
