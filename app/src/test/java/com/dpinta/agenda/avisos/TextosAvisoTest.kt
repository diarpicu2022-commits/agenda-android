package com.dpinta.agenda.avisos

import com.dpinta.agenda.domain.AlarmKind
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

class TextosAvisoTest {

    private fun estimacion(guardada: Boolean = false) =
        TravelEstimate(Duration.ofMinutes(23), TransportMode.TRANSPORTE_PUBLICO, Instant.EPOCH, fromCache = guardada)

    @Test
    fun `salida sigue la copia del anexo`() {
        val t = TextosAviso.salida(LocalTime.of(7, 32), "Cálculo", "B-204", estimacion(), Duration.ofMinutes(7), es24h = true)
        assertEquals("Sal a las 7:32 · Cálculo", t.titulo)
        assertEquals("B-204 · 23 min en bus + 7 de margen", t.texto)
    }

    @Test
    fun `salida en 12 h y con dato guardado lleva sufijo y aprox`() {
        val t = TextosAviso.salida(LocalTime.of(19, 5), "Turno", "", estimacion(guardada = true), Duration.ofMinutes(10), es24h = false)
        assertEquals("Sal a las 7:05 p. m. aprox. · Turno", t.titulo)
        assertEquals("23 min en bus + 10 de margen", t.texto)
    }

    @Test
    fun `empieza con y sin salon`() {
        assertEquals("Empieza Cálculo · B-204", TextosAviso.empieza("Cálculo", " B-204 ").titulo)
        val sinSalon = TextosAviso.empieza("Cálculo", "")
        assertEquals("Empieza Cálculo", sinSalon.titulo)
        assertNull(sinSalon.texto)
    }

    @Test
    fun `sal ya y vas tarde siguen la copia del anexo`() {
        val ya = TextosAviso.salYa(LocalTime.of(8, 0), "Cálculo", "B-204", estimacion(), Duration.ofMinutes(7), es24h = true)
        assertEquals("Sal ya · Cálculo empieza 8:00", ya.titulo)
        assertEquals("B-204 · 23 min en bus + 7 de margen", ya.texto)
        val tarde = TextosAviso.vasTarde(4, LocalTime.of(8, 4), LocalTime.of(8, 0), "Cálculo", "B-204", es24h = true)
        assertEquals("Vas 4 min tarde · llegas 8:04 a B-204", tarde.titulo)
        assertEquals("Cálculo empieza 8:00", tarde.texto)
        assertEquals("Vas 1 min tarde · llegas 8:04", TextosAviso.vasTarde(0, LocalTime.of(8, 4), LocalTime.of(8, 0), "Cálculo", "", true).titulo)
    }

    @Test
    fun `resumen matutino`() {
        assertEquals("Hoy: 3 cosas · primera salida 7:32", TextosAviso.resumen(3, LocalTime.of(7, 32), es24h = true).titulo)
        assertEquals("Hoy: 1 cosa", TextosAviso.resumen(1, null, es24h = true).titulo)
    }

    @Test
    fun `el codigo de alarma es estable y distingue tipo y sesion`() {
        val inicio = LocalDateTime.of(2026, 9, 28, 8, 0)
        assertEquals(ProgramadorAvisos.codigo(AlarmKind.AVISO, 1, inicio), ProgramadorAvisos.codigo(AlarmKind.AVISO, 1, inicio))
        assertNotEquals(ProgramadorAvisos.codigo(AlarmKind.AVISO, 1, inicio), ProgramadorAvisos.codigo(AlarmKind.SALIDA, 1, inicio))
        assertNotEquals(ProgramadorAvisos.codigo(AlarmKind.AVISO, 1, inicio), ProgramadorAvisos.codigo(AlarmKind.AVISO, 1, inicio.plusWeeks(1)))
    }
}
