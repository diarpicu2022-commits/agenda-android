package com.dpinta.agenda.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DepartureTest {

    // Cálculo a las 8:00; bus 23 min + 7 de margen → salir 7:30.
    private val start = Instant.parse("2026-09-23T13:00:00Z")
    private val margin = Duration.ofMinutes(7)
    private fun travel(computedAt: Instant, cached: Boolean = false) =
        TravelEstimate(Duration.ofMinutes(23), TransportMode.TRANSPORTE_PUBLICO, computedAt, cached)

    private fun stateAt(minutesBeforeLeave: Long): DepartureState {
        val leave = start - margin - Duration.ofMinutes(23)
        val now = leave - Duration.ofMinutes(minutesBeforeLeave)
        return DepartureCalculator.plan(start, margin, travel(now), now).state
    }

    @Test fun `hora de salida es inicio menos margen menos trayecto`() {
        val now = Instant.parse("2026-09-23T11:00:00Z")
        val plan = DepartureCalculator.plan(start, margin, travel(now), now)
        assertEquals(Instant.parse("2026-09-23T12:30:00Z"), plan.leaveAt)
        assertEquals(90L, plan.minutesToLeave)
    }

    @Test fun `estados de la banda segun el contrato`() {
        assertEquals(DepartureState.ESPERA, stateAt(16))
        assertEquals(DepartureState.PREPARATE, stateAt(15))
        assertEquals(DepartureState.PREPARATE, stateAt(1))
        assertEquals(DepartureState.SAL_YA, stateAt(0))
        assertEquals(DepartureState.SAL_YA, stateAt(-1))
        assertEquals(DepartureState.VAS_TARDE, stateAt(-2))
    }

    @Test fun `vas tarde dice a que hora llegas si sales ya`() {
        val now = Instant.parse("2026-09-23T12:41:00Z")
        val plan = DepartureCalculator.plan(start, margin, travel(now), now)
        assertEquals(Instant.parse("2026-09-23T13:04:00Z"), plan.arriveIfLeavingNow)
    }

    @Test fun `dato viejo si viene de cache o tiene mas de 15 minutos`() {
        val now = Instant.parse("2026-09-23T11:00:00Z")
        assertFalse(DepartureCalculator.plan(start, margin, travel(now), now).stale)
        assertTrue(DepartureCalculator.plan(start, margin, travel(now, cached = true), now).stale)
        assertTrue(DepartureCalculator.plan(start, margin, travel(now - Duration.ofMinutes(16)), now).stale)
    }

    @Test fun `recalcula a mitad de camino y no en los ultimos 15 minutos`() {
        val leave = Instant.parse("2026-09-23T12:30:00Z")
        assertEquals(Instant.parse("2026-09-23T11:30:00Z"), DepartureCalculator.nextRecalculation(leave, Instant.parse("2026-09-23T10:30:00Z")))
        assertNull(DepartureCalculator.nextRecalculation(leave, Instant.parse("2026-09-23T12:16:00Z")))
    }
}
