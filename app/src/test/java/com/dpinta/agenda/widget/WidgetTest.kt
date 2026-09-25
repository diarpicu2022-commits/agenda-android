package com.dpinta.agenda.widget

import com.dpinta.agenda.data.agenda.Actividad
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.Lugar
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class WidgetTest {

    private val zona = ZoneId.of("America/Bogota")
    private val hoy = LocalDate.of(2026, 9, 25)
    private val agenda = Agenda(
        actividades = listOf(
            Actividad(1, "Cálculo diferencial", ActivityKind.CLASE, "B-204", 10, Duration.ofMinutes(7), TransportMode.TRANSPORTE_PUBLICO),
            Actividad(2, "Física mecánica", ActivityKind.CLASE, "L-3", 10, Duration.ofMinutes(7), TransportMode.TRANSPORTE_PUBLICO),
        ).associateBy { it.id },
        lugares = mapOf(10L to Lugar(10, "Campus")),
        reglas = emptyList(),
        puntuales = emptyList(),
    )
    private val calculo = Occurrence(1, 10, hoy.atTime(8, 0), hoy.atTime(10, 0))
    private val fisica = Occurrence(2, 10, hoy.atTime(10, 30), hoy.atTime(12, 0))
    private val bus23 = TravelEstimate(Duration.ofMinutes(23), TransportMode.TRANSPORTE_PUBLICO, hoy.atTime(6, 0).atZone(zona).toInstant(), false, manual = true)

    @Test
    fun `sal a las con trayecto y lo siguiente para 4x2`() {
        val m = WidgetMapeador.modelo(agenda, listOf(fisica, calculo), hoy.atTime(6, 30), zona) { bus23 } as WidgetModelo.Siguiente
        val t = WidgetTextos.de(m, hoy, es24h = false)
        assertEquals("SAL A LAS", t.rotulo)
        assertEquals("7:30", t.hora)
        assertEquals("a. m.", t.sufijo)
        assertEquals("B-204", t.salon)
        assertNull(t.nota)
        assertEquals("Después: 10:30 a. m. · Física mecánica · L-3", t.despues)
        assertEquals("Sal a las 7:30 a. m., Cálculo diferencial, salón B-204, Campus", t.hablado)
    }

    @Test
    fun `misma sede empieza y sin trayecto no hay salida`() {
        // A las 9:00 lo siguiente es Física, en el mismo Campus que Cálculo: solo «empieza».
        val m = WidgetMapeador.modelo(agenda, listOf(calculo, fisica), hoy.atTime(9, 0), zona) { bus23 } as WidgetModelo.Siguiente
        val t = WidgetTextos.de(m, hoy, es24h = true)
        assertEquals("EMPIEZA", t.rotulo)
        assertEquals("10:30", t.hora)
        assertNull(t.sufijo)
    }

    @Test
    fun `manana y estados vacios`() {
        val manana = calculo.copy(start = hoy.plusDays(1).atTime(8, 0), end = hoy.plusDays(1).atTime(10, 0))
        val m = WidgetMapeador.modelo(agenda, listOf(manana), hoy.atTime(20, 0), zona) { bus23 }
        assertEquals("MAÑANA, SAL A LAS", WidgetTextos.de(m, hoy, true).rotulo)
        assertEquals(WidgetModelo.SinNada, WidgetMapeador.modelo(agenda, emptyList(), hoy.atTime(20, 0), zona) { null })
        assertEquals(WidgetModelo.PrimerUso, WidgetMapeador.modelo(Agenda.VACIA, emptyList(), hoy.atTime(20, 0), zona) { null })
        assertEquals("Empieza por tu horario de clases", WidgetTextos.de(WidgetModelo.PrimerUso, hoy, true).nota)
    }
}
