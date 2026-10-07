package com.dpinta.agenda.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DayFocusTest {
    private val d = LocalDateTime.of(2026, 10, 7, 0, 0)
    private fun h(hora: Int, min: Int = 0) = d.withHour(hora).withMinute(min)

    @Test fun `al empezar la clase se queda en ella, no salta a la siguiente`() {
        // Cálculo 8:00–10:00, Física 10:30 en el mismo campus (sin trayecto).
        assertTrue(DayFocus.showCurrent(h(8, 0), h(10), h(10, 30), null))
        assertTrue(DayFocus.showCurrent(h(9, 59), h(10), h(10, 30), null))
    }

    @Test fun `termina la clase y pasa a la siguiente`() {
        assertFalse(DayFocus.showCurrent(h(10, 0), h(10), h(10, 30), null))
    }

    @Test fun `con trayecto cambia 15 min antes de la hora de salida aunque la clase siga`() {
        // Turno a las 12:25 saliendo a las 11:30: a las 11:15 ya muestra la salida.
        assertTrue(DayFocus.showCurrent(h(11, 14), h(12), h(12, 25), h(11, 30)))
        assertFalse(DayFocus.showCurrent(h(11, 15), h(12), h(12, 25), h(11, 30)))
    }

    @Test fun `seguidas sin trayecto cambia 15 min antes de que empiece la otra`() {
        assertTrue(DayFocus.showCurrent(h(9, 44), h(10), h(10), null))
        assertFalse(DayFocus.showCurrent(h(9, 45), h(10), h(10), null))
    }

    @Test fun `sin siguiente se queda en la en curso hasta que termine`() {
        assertTrue(DayFocus.showCurrent(h(17), h(18), null, null))
        assertFalse(DayFocus.showCurrent(h(18), h(18), null, null))
        assertFalse(DayFocus.showCurrent(h(9), null, h(10), null))
    }

    @Test fun `la en curso es la que empezó y no ha terminado`() {
        val s = listOf(h(8) to h(10), h(10, 30) to h(12))
        assertEquals(h(8) to h(10), DayFocus.current(s, h(9), { it.first }, { it.second }))
        assertNull(DayFocus.current(s, h(10, 10), { it.first }, { it.second }))
    }
}
