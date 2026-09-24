package com.dpinta.agenda.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidaysTest {

    @Test fun `domingo de pascua`() {
        assertEquals(LocalDate.of(2025, 4, 20), ColombianHolidays.easterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), ColombianHolidays.easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), ColombianHolidays.easterSunday(2027))
    }

    @Test fun `los 18 festivos de 2026`() {
        val expected = listOf(
            "2026-01-01", "2026-01-12", "2026-03-23", "2026-04-02", "2026-04-03", "2026-05-01",
            "2026-05-18", "2026-06-08", "2026-06-15", "2026-06-29", "2026-07-20", "2026-08-07",
            "2026-08-17", "2026-10-12", "2026-11-02", "2026-11-16", "2026-12-08", "2026-12-25",
        ).map(LocalDate::parse)
        assertEquals(expected, ColombianHolidays.of(2026).map { it.date })
    }

    @Test fun `en 2025 San Pedro y Sagrado Corazon caen el mismo lunes`() {
        val june30 = ColombianHolidays.of(2025).filter { it.date == LocalDate.of(2025, 6, 30) }.map { it.name }
        assertEquals(listOf("Sagrado Corazón", "San Pedro y San Pablo"), june30.sorted())
        assertEquals(17, ColombianHolidays.between(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)).size)
    }

    @Test fun `llena los dias sin clase del semestre y la serie se los salta`() {
        val start = LocalDate.of(2026, 8, 3)
        val end = LocalDate.of(2026, 11, 27)
        val off = ColombianHolidays.between(start, end)
        assertEquals(
            listOf("2026-08-07", "2026-08-17", "2026-10-12", "2026-11-02", "2026-11-16").map(LocalDate::parse),
            off.toList(),
        )
        val lunes = WeeklyRule(1, setOf(java.time.DayOfWeek.MONDAY), java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 0), start, end)
        val occ = ScheduleExpander.expand(listOf(lunes), emptyList(), { null }, start, end, Semester(start, end, off))
        assertTrue(occ.none { it.start.toLocalDate() in off })
        assertEquals(17 - 4, occ.size) // 17 lunes en el semestre, 4 festivos en lunes
    }
}
