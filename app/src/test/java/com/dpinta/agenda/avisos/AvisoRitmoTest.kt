package com.dpinta.agenda.avisos

import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.ui.components.Lengua
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/** «Tu ritmo» en el aviso de salida (anexo 2026-11-10-agenda-ia, dirección A en el teléfono). */
class AvisoRitmoTest {

    private val bus = TravelEstimate(Duration.ofMinutes(23), TransportMode.TRANSPORTE_PUBLICO, Instant.EPOCH, false)

    @Test fun conRitmoLoSumaAlTexto() {
        val t = TextosAviso.salida(LocalTime.of(7, 32), "Cálculo", "B-204", bus, Duration.ofMinutes(7), true, ritmoMinutos = 6)
        assertEquals("B-204 · 23 min en bus + 7 de margen · sueles salir 6 min tarde", t.texto)
    }

    @Test fun sinRitmoQuedaIgual() {
        val t = TextosAviso.salida(LocalTime.of(7, 32), "Cálculo", "B-204", bus, Duration.ofMinutes(7), true)
        assertEquals("B-204 · 23 min en bus + 7 de margen", t.texto)
    }

    @Test fun enIngles() {
        val t = TextosAviso.salida(LocalTime.of(7, 32), "Calculus", "B-204", bus, Duration.ofMinutes(7), true, Lengua.EN, 6)
        assertEquals("B-204 · 23 min by bus + 7 min buffer · you usually leave 6 min late", t.texto)
    }
}
