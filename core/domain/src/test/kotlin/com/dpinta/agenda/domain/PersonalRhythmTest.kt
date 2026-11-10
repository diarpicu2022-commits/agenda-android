package com.dpinta.agenda.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant

class PersonalRhythmTest {
    private val base = Instant.parse("2026-11-02T12:00:00Z")
    private fun s(late: Long, lugar: Long? = 1, dia: DayOfWeek = DayOfWeek.MONDAY, n: Int = 0) =
        DepartureSample(1, lugar, dia, base.plusSeconds(n * 86_400L), base.plusSeconds(n * 86_400L + late * 60))

    @Test fun `sin suficientes salidas no aconseja`() {
        assertNull(PersonalRhythm.advice(listOf(s(8), s(9), s(10)), 1, DayOfWeek.MONDAY))
    }

    @Test fun `mediana del retraso al mismo lugar`() {
        val h = listOf(s(4, n = 1), s(6, n = 2), s(7, n = 3), s(30, n = 4), s(5, n = 5))
        assertEquals(RhythmAdvice(6, 5, RhythmAdvice.Scope.LUGAR), PersonalRhythm.advice(h, 1, DayOfWeek.MONDAY))
    }

    @Test fun `los olvidos de más de una hora no cuentan`() {
        val h = listOf(s(3, n = 1), s(4, n = 2), s(5, n = 3), s(4, n = 4), s(180, n = 5), s(-300, n = 6))
        assertEquals(RhythmAdvice(4, 4, RhythmAdvice.Scope.LUGAR), PersonalRhythm.advice(h, 1, DayOfWeek.MONDAY))
    }

    @Test fun `quien sale a tiempo no recibe consejo`() {
        assertNull(PersonalRhythm.advice(listOf(s(0, n = 1), s(1, n = 2), s(-2, n = 3), s(1, n = 4)), 1, DayOfWeek.MONDAY))
    }

    @Test fun `sin datos del lugar usa el día y luego todas`() {
        val otroLugar = listOf(s(9, 2, DayOfWeek.TUESDAY, 1), s(9, 2, DayOfWeek.TUESDAY, 2), s(10, 2, DayOfWeek.TUESDAY, 3), s(8, 2, DayOfWeek.TUESDAY, 4))
        assertEquals(RhythmAdvice.Scope.DIA, PersonalRhythm.advice(otroLugar, 1, DayOfWeek.TUESDAY)!!.scope)
        assertEquals(RhythmAdvice.Scope.GENERAL, PersonalRhythm.advice(otroLugar, 1, DayOfWeek.FRIDAY)!!.scope)
    }

    @Test fun `el consejo tiene tope`() {
        val h = (1..5).map { s(45, n = it) }
        assertEquals(PersonalRhythm.MAX_LATE, PersonalRhythm.advice(h, 1, DayOfWeek.MONDAY)!!.lateMinutes)
    }
}
