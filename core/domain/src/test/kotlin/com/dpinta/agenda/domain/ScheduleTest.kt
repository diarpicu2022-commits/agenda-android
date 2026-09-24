package com.dpinta.agenda.domain

import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {

    private val calculo = WeeklyRule(
        activityId = 1, days = setOf(MONDAY, WEDNESDAY),
        start = LocalTime.of(8, 0), end = LocalTime.of(10, 0),
        from = LocalDate.of(2026, 8, 3), until = LocalDate.of(2026, 11, 27),
    )
    private val turno = WeeklyRule(
        activityId = 2, days = setOf(WEDNESDAY),
        start = LocalTime.of(10, 15), end = LocalTime.of(16, 0),
        from = LocalDate.of(2026, 8, 3), until = LocalDate.of(2026, 12, 31),
    )
    private val places = mapOf(1L to 10L, 2L to 20L)
    private val week = LocalDate.of(2026, 9, 21) to LocalDate.of(2026, 9, 27)

    private fun expand(semester: Semester? = null, exceptions: List<SessionException> = emptyList()) =
        ScheduleExpander.expand(listOf(calculo, turno), emptyList(), places::get, week.first, week.second, semester, exceptions)

    @Test fun `la serie se repite los mismos dias de cada semana`() {
        val occ = expand()
        assertEquals(3, occ.size) // Cálculo lunes y miércoles, turno miércoles
        assertEquals(LocalDate.of(2026, 9, 21).atTime(8, 0), occ[0].start)
    }

    @Test fun `los festivos del semestre quitan las clases`() {
        val semester = Semester(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 11, 27), setOf(LocalDate.of(2026, 9, 21)))
        val occ = expand(semester)
        assertTrue(occ.none { it.start.toLocalDate() == LocalDate.of(2026, 9, 21) })
    }

    @Test fun `cancelar o mover una sesion no toca el resto de la serie`() {
        val wed = LocalDate.of(2026, 9, 23)
        val cancelled = expand(exceptions = listOf(SessionException.Cancelled(1, wed)))
        assertEquals(2, cancelled.size)

        val moved = expand(exceptions = listOf(SessionException.Moved(1, wed, wed.atTime(14, 0), wed.atTime(16, 0))))
        assertEquals(wed.atTime(14, 0), moved.first { it.activityId == 1L && it.start.toLocalDate() == wed }.start)
    }

    @Test fun `detecta traslado insuficiente entre lugares distintos`() {
        val conflicts = ConflictDetector.detect(expand()) { _, _ -> 35L }
        val c = conflicts.single() as Conflict.NotEnoughTravel
        assertEquals(15L, c.availableMinutes)
        assertEquals(35L, c.neededMinutes)
    }

    @Test fun `sin estimacion de trayecto no se inventa un conflicto`() {
        assertTrue(ConflictDetector.detect(expand()) { _, _ -> null }.isEmpty())
    }

    @Test fun `detecta solapes`() {
        val wed = LocalDate.of(2026, 9, 23)
        val moved = expand(exceptions = listOf(SessionException.Moved(2, wed, wed.atTime(9, 0), wed.atTime(12, 0))))
        assertTrue(ConflictDetector.detect(moved) { _, _ -> 0L }.any { it is Conflict.Overlap })
    }
}
