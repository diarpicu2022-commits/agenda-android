package com.dpinta.agenda.ui.semana

import com.dpinta.agenda.data.agenda.Actividad
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.Semester
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.WeeklyRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class SemanaMapeadorTest {

    // Lunes 12 de octubre de 2026 es festivo (Día de la Raza, trasladado).
    private val lunes = LocalDate.of(2026, 10, 12)
    private val miercoles = lunes.plusDays(2)
    private fun act(id: Long, titulo: String, tipo: ActivityKind) =
        Actividad(id, titulo, tipo, "B-20$id", null, Duration.ofMinutes(10), TransportMode.TRANSPORTE_PUBLICO)
    private fun serie(id: Long, h: Int, fin: Int) =
        WeeklyRule(id, setOf(MONDAY, WEDNESDAY), LocalTime.of(h, 0), LocalTime.of(fin, 30), lunes.minusWeeks(8), lunes.plusWeeks(8))

    private val agenda = Agenda(
        actividades = listOf(act(1, "Cálculo diferencial", ActivityKind.CLASE), act(2, "Física", ActivityKind.CLASE), act(3, "Entrega", ActivityKind.PUNTUAL))
            .associateBy { it.id },
        lugares = emptyMap(),
        reglas = listOf(serie(1, 8, 9), serie(2, 9, 10)),
        puntuales = listOf(Occurrence(3, null, miercoles.atTime(21, 0), miercoles.atTime(21, 30))),
        semestres = listOf(Semester(lunes.minusWeeks(7), lunes.plusWeeks(8), setOf(lunes))),
    )

    @Test
    fun `semana del semestre con festivo, carriles y horas visibles`() {
        val s = SemanaMapeador.estado(agenda, lunes, miercoles.atTime(9, 15)) as SemanaUiState.Semana
        assertEquals(8, s.numero)
        assertEquals("Festivo: Día de la Raza", s.dias[0].sinClase)
        assertTrue(s.dias[2].esHoy)
        // El festivo quita las series del lunes: solo quedan las del miércoles y la puntual.
        assertTrue(s.bloques.none { it.dia == 0 })
        val delMiercoles = s.bloques.filter { it.dia == 2 }.sortedBy { it.inicio }
        assertEquals(listOf("Cálc", "Físi", "Entr"), delMiercoles.map { it.abreviado })
        // Cálculo 8:00–9:30 y Física 9:00–10:30 se cruzan: dos carriles; la entrega va sola.
        assertEquals(listOf(0 to 2, 1 to 2, 0 to 1), delMiercoles.map { it.carril to it.carriles })
        assertEquals(8, s.desde)
        assertEquals(22, s.hasta)
        assertEquals(LocalTime.of(9, 15), s.ahora)
    }

    @Test
    fun `otra semana no lleva ahora ni numero fuera del semestre`() {
        val lejos = lunes.plusWeeks(20)
        val s = SemanaMapeador.estado(agenda, lejos, miercoles.atTime(9, 15)) as SemanaUiState.Semana
        assertNull(s.ahora)
        assertNull(s.numero)
        assertTrue(s.bloques.isEmpty())
        assertEquals(7, s.desde)
    }

    @Test
    fun `lunes de cualquier dia`() {
        assertEquals(lunes, SemanaMapeador.lunesDe(lunes.plusDays(6)))
        assertEquals(lunes, SemanaMapeador.lunesDe(lunes))
    }
}
