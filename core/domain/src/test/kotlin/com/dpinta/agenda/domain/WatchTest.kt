package com.dpinta.agenda.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class WatchTest {
    private val start = Instant.parse("2026-10-07T13:00:00Z")
    private val leave = Instant.parse("2026-10-07T12:30:00Z")
    private fun at(min: Long) = leave.plusSeconds(min * 60)

    @Test fun `niveles por tiempo hasta la salida`() {
        assertEquals(Urgency.CALM, UrgencyRules.level(at(-61), start, leave))
        assertEquals(Urgency.UPCOMING, UrgencyRules.level(at(-45), start, leave))
        assertEquals(Urgency.SOON, UrgencyRules.level(at(-30), start, leave))
        assertEquals(Urgency.SOON, UrgencyRules.level(at(-1), start, leave))
        assertEquals(Urgency.LEAVE_NOW, UrgencyRules.level(at(0), start, leave))
        assertEquals(Urgency.LEAVE_NOW, UrgencyRules.level(at(4), start, leave))
        assertEquals(Urgency.URGENT, UrgencyRules.level(at(5), start, leave))
        assertEquals(Urgency.MISSED, UrgencyRules.level(at(30), start, leave))
    }

    @Test fun `con «Ya voy» no sube a urgente y al empezar va en camino`() {
        assertEquals(Urgency.UPCOMING, UrgencyRules.level(at(10), start, leave, leftAt = at(1)))
        assertEquals(Urgency.UPCOMING, UrgencyRules.level(at(31), start, leave, leftAt = at(1)))
    }

    @Test fun `sin trayecto cuenta hasta el inicio`() {
        assertEquals(Urgency.SOON, UrgencyRules.level(start.minusSeconds(600), start, null))
        assertEquals(Urgency.MISSED, UrgencyRules.level(start, start, null))
    }

    @Test fun `examen sube un nivel como máximo y nunca desde Calma`() {
        assertEquals(Urgency.CALM, UrgencyRules.level(at(-90), start, leave, exam = true))
        assertEquals(Urgency.UPCOMING, UrgencyRules.level(at(-40), start, leave, exam = true))
        assertEquals(Urgency.SOON, UrgencyRules.level(at(-25), start, leave, exam = true)) // nunca «sal ya» antes de tiempo
        assertEquals(Urgency.URGENT, UrgencyRules.level(at(0), start, leave, exam = true))
    }

    @Test fun `ida y vuelta del paquete con tabuladores, saltos y barras en los textos`() {
        val d = LocalDateTime.of(2026, 10, 7, 7, 12)
        val day = WatchDay(d, listOf(
            WatchSession(3, d.withHour(8).withMinute(0), d.withHour(10).withMinute(0), "Cálculo\tdiferencial", "B-204", "Campus\\Norte",
                d.withHour(7).withMinute(30), WatchSession.Kind.CLASE, exam = false),
            WatchSession(9, d.withHour(11), d.withHour(12), "Parcial\nFísica", "", "", null, WatchSession.Kind.CLASE, exam = true),
        ))
        assertEquals(day, WatchDay.decode(day.encode()))
        assertNull(WatchDay.decode("otra cosa"))
    }

    @Test fun `la siguiente es la primera que no ha empezado y la actual la que está en curso`() {
        val d = LocalDateTime.of(2026, 10, 7, 0, 0)
        val a = WatchSession(1, d.withHour(8), d.withHour(10), "A", "", "", null, WatchSession.Kind.CLASE, false)
        val b = WatchSession(2, d.withHour(11), d.withHour(12), "B", "", "", null, WatchSession.Kind.TRABAJO, false)
        val day = WatchDay(d, listOf(b, a))
        assertEquals(a, day.next(d.withHour(7)))
        assertEquals(b, day.next(d.withHour(9)))
        assertEquals(a, day.current(d.withHour(9)))
        assertNull(day.current(d.withHour(10)))
        assertNull(day.next(d.withHour(13)))
    }

    @Test fun `el reloj se queda en la clase en curso y cambia cuando toca prepararse para la otra`() {
        val d = LocalDateTime.of(2026, 10, 7, 0, 0)
        val calculo = WatchSession(1, d.withHour(8), d.withHour(10), "Cálculo", "B-204", "Campus", null, WatchSession.Kind.CLASE, false)
        val turno = WatchSession(2, d.withHour(12).withMinute(25), d.withHour(18), "Turno", "Caja 2", "Tienda", d.withHour(11).withMinute(30), WatchSession.Kind.TRABAJO, false)
        val day = WatchDay(d, listOf(calculo, turno))
        assertEquals(WatchDay.Focus(calculo, true), day.focus(d.withHour(8)))
        assertEquals(WatchDay.Focus(calculo, true), day.focus(d.withHour(9).withMinute(59)))
        assertEquals(WatchDay.Focus(turno, false), day.focus(d.withHour(10)))
        assertNull(day.focus(d.withHour(19)))
    }
}
