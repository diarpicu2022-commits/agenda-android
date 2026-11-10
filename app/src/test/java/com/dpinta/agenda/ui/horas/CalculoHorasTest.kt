package com.dpinta.agenda.ui.horas

import com.dpinta.agenda.domain.Occurrence
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Horas trabajadas (04-patrones): solo lo ya trabajado, por semana, dos semanas y quincena. */
class CalculoHorasTest {

    private val lunes = LocalDate.of(2026, 11, 9)
    private fun turno(d: LocalDate, h1: Int, h2: Int, id: Long = 1) = Occurrence(id, null, d.atTime(h1, 0), d.atTime(h2, 0))
    private val esTrabajo = { id: Long -> id == 1L }

    @Test fun cuentaSoloLoYaTrabajado() {
        val sesiones = listOf(turno(lunes, 8, 12), turno(lunes.plusDays(1), 14, 18), turno(lunes.plusDays(2), 8, 12))
        val ahora = LocalDateTime.of(lunes.plusDays(1), java.time.LocalTime.of(16, 0)) // martes a media jornada
        val r = CalculoHoras.resumen(sesiones, esTrabajo, ahora, true)
        assertEquals(Duration.ofHours(6), r.semana) // 4 h del lunes + 2 h del martes
        assertEquals(1, r.turnosCompletados)
        assertEquals(240L, r.porDia[0].second)
        assertEquals(120L, r.porDia[1].second)
        assertEquals(0L, r.porDia[2].second)
    }

    @Test fun lasClasesNoSonHoras() {
        val sesiones = listOf(turno(lunes, 8, 12), turno(lunes, 13, 15, id = 2))
        val r = CalculoHoras.resumen(sesiones, esTrabajo, lunes.atTime(20, 0), true)
        assertEquals(Duration.ofHours(4), r.semana)
    }

    @Test fun dosSemanasYQuincena() {
        val sesiones = listOf(turno(lunes.minusDays(7), 8, 12), turno(lunes, 8, 12))
        val r = CalculoHoras.resumen(sesiones, esTrabajo, lunes.atTime(20, 0), true)
        assertEquals(Duration.ofHours(8), r.dosSemanas)
        assertEquals(Duration.ofHours(4), r.semana)
        assertEquals(Duration.ofHours(8), r.quincena) // quincena del 1 al 15 de noviembre: el turno del 2 también cuenta
    }
}
