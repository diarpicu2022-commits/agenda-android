package com.dpinta.agenda.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayPlannerTest {

    private fun at(h: Int, m: Int = 0) = LocalDateTime.of(2026, 9, 23, h, m)
    private val calculo = Occurrence(1, placeId = 10, start = at(8), end = at(10))
    private val fisica = Occurrence(2, placeId = 10, start = at(10, 30), end = at(12))
    private val turno = Occurrence(3, placeId = 20, start = at(12, 25), end = at(18))
    private val entrega = Occurrence(4, placeId = null, start = at(21), end = at(21, 30))
    private val dia = listOf(turno, calculo, entrega, fisica)

    @Test fun `la siguiente es la primera que no ha empezado`() {
        assertEquals(calculo, DayPlanner.next(dia, at(6, 51)))
        assertEquals(fisica, DayPlanner.next(dia, at(9)))
        assertEquals(fisica, DayPlanner.next(dia, at(10)))
        assertNull(DayPlanner.next(dia, at(21, 5)))
    }

    @Test fun `la anterior es la ultima que termina antes el mismo dia`() {
        assertNull(DayPlanner.previous(dia, calculo))
        assertEquals(calculo, DayPlanner.previous(dia, fisica))
        assertEquals(fisica, DayPlanner.previous(dia, turno))
    }

    @Test fun `hay traslado si cambia el lugar y no si es el mismo o no hay lugar`() {
        assertTrue(DayPlanner.needsTravel(previous = null, next = calculo))
        assertFalse(DayPlanner.needsTravel(previous = calculo, next = fisica))
        assertTrue(DayPlanner.needsTravel(previous = fisica, next = turno))
        assertFalse(DayPlanner.needsTravel(previous = turno, next = entrega))
    }

    @Test fun `llegada de un trayecto ya iniciado`() {
        val salida = Instant.parse("2026-09-23T12:35:00Z")
        val bus = TravelEstimate(Duration.ofMinutes(23), TransportMode.TRANSPORTE_PUBLICO, salida, false)
        val p = Trip.progress(salida, bus, now = salida + Duration.ofMinutes(5))
        assertEquals(Instant.parse("2026-09-23T12:58:00Z"), p.arriveAt)
        assertEquals(18L, p.minutesRemaining)
        assertEquals(0L, Trip.progress(salida, bus, now = salida + Duration.ofMinutes(40)).minutesRemaining)
    }
}
