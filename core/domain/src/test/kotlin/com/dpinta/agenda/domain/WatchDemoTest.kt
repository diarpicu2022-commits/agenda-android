package com.dpinta.agenda.domain

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchDemoTest {
    private val ahora = LocalDateTime.of(2026, 11, 12, 9, 30, 41)

    @Test
    fun laProximaEsUnaClaseAPieConRitmo() {
        val dia = WatchDemo.day(ahora)
        val foco = dia.focus(ahora.withSecond(0))!!
        assertEquals("Programación", foco.session.title)
        assertEquals(TransportMode.A_PIE, foco.session.mode)
        assertNotNull(foco.session.leaveAt)
        assertEquals(6, dia.rhythmFor(foco.session)?.lateMinutes)
    }

    @Test
    fun traeFuisteMananaYAsistenciaEnElLimite() {
        val dia = WatchDemo.day(ahora)
        assertTrue(dia.attendance.any { it.pendingStart != null })
        assertTrue(dia.attendance.any { it.maxAbsences != null && it.absences >= it.maxAbsences!! })
        assertEquals(1, dia.on(ahora.toLocalDate().plusDays(1)).size)
    }

    @Test
    fun pasaPorElFormatoDelRelojSinPerderNada() {
        val dia = WatchDemo.day(ahora, english = true)
        assertEquals(dia, WatchDay.decode(dia.encode()))
    }
}
