package com.dpinta.agenda.domain

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPlannerTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val day = LocalDate.of(2026, 9, 24)
    private val calculo = Occurrence(1, placeId = 10, start = day.atTime(8, 0), end = day.atTime(10, 0))
    private val fisica = Occurrence(2, placeId = 10, start = day.atTime(10, 0), end = day.atTime(12, 0))
    private val turno = Occurrence(3, placeId = 20, start = day.atTime(14, 0), end = day.atTime(18, 0))
    private val llamada = Occurrence(4, placeId = null, start = day.atTime(19, 0), end = day.atTime(19, 30))
    private val all = listOf(turno, calculo, llamada, fisica)

    private fun at(h: Int, m: Int = 0) = day.atTime(h, m).atZone(bogota).toInstant()
    private val settings = AlarmSettings(margin = Duration.ofMinutes(10), remindBefore = Duration.ofMinutes(15))
    private fun travel(min: Long) = TravelEstimate(Duration.ofMinutes(min), TransportMode.TRANSPORTE_PUBLICO, at(5), false)

    @Test fun `precalculo y salida solo donde hay traslado`() {
        val plan = AlarmPlanner.plan(all, bogota, at(5), { settings }, { travel(40) })
        val byKind = plan.groupBy { it.kind }.mapValues { (_, v) -> v.map { it.activityId } }
        // Física es en el mismo lugar que Cálculo y la llamada no tiene lugar: sin traslado.
        assertEquals(listOf(1L, 3L), byKind[AlarmKind.PRECALCULO])
        assertEquals(listOf(1L, 3L), byKind[AlarmKind.SALIDA])
        assertEquals(listOf(1L, 2L, 3L, 4L), byKind[AlarmKind.AVISO])
        assertEquals(listOf(1L, 3L), byKind[AlarmKind.SAL_YA])
        assertEquals(listOf(1L, 3L), byKind[AlarmKind.VAS_TARDE])
        // Cálculo: 8:00 − 10 min de margen − 40 de trayecto = 7:10; «Sal a las» 15 min antes y «Vas tarde» 2 min después.
        fun deCalculo(k: AlarmKind) = plan.single { it.kind == k && it.activityId == 1L }.at
        assertEquals(at(6, 55), deCalculo(AlarmKind.SALIDA))
        assertEquals(at(7, 10), deCalculo(AlarmKind.SAL_YA))
        assertEquals(at(7, 12), deCalculo(AlarmKind.VAS_TARDE))
        assertEquals(at(6), plan.single { it.kind == AlarmKind.PRECALCULO && it.activityId == 1L }.at)
        assertEquals(plan.sortedBy { it.at }, plan)
    }

    @Test fun `sin estimacion todavia no hay alarma de salida`() {
        val plan = AlarmPlanner.plan(all, bogota, at(5), { settings }, { null })
        assertTrue(plan.none { it.kind in setOf(AlarmKind.SALIDA, AlarmKind.SAL_YA, AlarmKind.VAS_TARDE) })
        assertEquals(2, plan.count { it.kind == AlarmKind.PRECALCULO })
    }

    @Test fun `no programa nada en el pasado`() {
        val plan = AlarmPlanner.plan(all, bogota, at(7, 30), { settings }, { travel(40) })
        // A las 7:30 ya pasó el precálculo (6:00) y la salida (7:10) de Cálculo; queda su aviso de 7:45.
        assertEquals(listOf(AlarmKind.AVISO), plan.filter { it.activityId == 1L }.map { it.kind })
        assertTrue(plan.all { it.at.isAfter(at(7, 30)) })
    }

    @Test fun `sin recordatorio propio no hay aviso y reprogramar da lo mismo`() {
        val quiet = AlarmSettings(Duration.ofMinutes(10), remindBefore = null)
        val a = AlarmPlanner.plan(all, bogota, at(5), { quiet }, { travel(40) })
        assertTrue(a.none { it.kind == AlarmKind.AVISO })
        assertEquals(a, AlarmPlanner.plan(all.reversed(), bogota, at(5), { quiet }, { travel(40) }))
    }
}
