package com.dpinta.agenda.ui.hoy

import com.dpinta.agenda.domain.DepartureCalculator
import com.dpinta.agenda.domain.DepartureState
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.ui.components.banda.EstadoBanda
import com.dpinta.agenda.ui.components.banda.ModoTransporte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** DeparturePlan + contexto → modelo de la banda. Los tiempos los pone el dominio. */
class BandaMapeadorTest {

    private val zona = ZoneId.of("America/Bogota")
    // Cálculo a las 8:00 (Bogotá = UTC-5); bus 23 min + 7 de margen → salir 7:30.
    private val inicio = Instant.parse("2026-09-23T13:00:00Z")
    private val margen = Duration.ofMinutes(7)
    private val sesion = SesionBanda("Cálculo diferencial", "B-204", "Campus", inicio, margen, TransportMode.TRANSPORTE_PUBLICO)

    private fun local(h: Int, m: Int) = LocalTime.of(h, m).atDate(java.time.LocalDate.of(2026, 9, 23)).atZone(zona).toInstant()
    private fun bus(calculado: Instant) = TravelEstimate(Duration.ofMinutes(23), TransportMode.TRANSPORTE_PUBLICO, calculado, false)

    private fun a(h: Int, m: Int, salioEn: Instant? = null, calculado: Instant? = null) = run {
        val ahora = local(h, m)
        val est = bus(calculado ?: (ahora - Duration.ofMinutes(3)))
        BandaMapeador.modelo(sesion, DepartureCalculator.plan(inicio, margen, est, ahora), est, salioEn, ahora, zona)
    }

    @Test fun esperaTomaLaHoraDelDominio() {
        val m = a(6, 51)
        assertEquals(EstadoBanda.Espera, m.estado)
        assertEquals(LocalTime.of(7, 30), m.horaSalida)
        assertEquals(LocalTime.of(8, 0), m.horaInicio)
        assertEquals(39L, m.minutosParaSalir)
        assertEquals(Duration.ofMinutes(3), m.calculadoHace)
        assertEquals(ModoTransporte.Bus, m.modo)
        assertFalse(m.datoViejo)
    }

    @Test fun losCuatroEstadosDelDominioSeTraducen() {
        assertEquals(EstadoBanda.Preparate, a(7, 20).estado)
        assertEquals(EstadoBanda.SalYa, a(7, 30).estado)
        val tarde = a(7, 41)
        assertEquals(EstadoBanda.VasTarde, tarde.estado)
        assertEquals(LocalTime.of(8, 4), tarde.horaLlegada)
        DepartureState.entries.forEach { BandaMapeador.aEstadoUi(it) }
    }

    @Test fun enCaminoUsaElTrayectoIniciado() {
        val m = a(7, 40, salioEn = local(7, 35))
        assertEquals(EstadoBanda.EnCamino, m.estado)
        assertEquals(LocalTime.of(7, 58), m.horaLlegada)
        assertEquals(Duration.ofMinutes(18), m.duracion)
    }

    @Test fun sinPlanEsSinTraslado() {
        val m = BandaMapeador.modelo(sesion, plan = null, estimacion = null, salioEn = null, ahora = local(7, 0), zona = zona)
        assertEquals(EstadoBanda.SinTraslado, m.estado)
        assertEquals(LocalTime.of(8, 0), m.horaInicio)
    }

    @Test fun datoViejoVieneDelDominio() {
        val m = a(7, 0, calculado = local(6, 20))
        assertTrue(m.datoViejo)
        assertEquals(Duration.ofMinutes(40), m.calculadoHace)
    }

    @Test fun modosIdaYVuelta() {
        TransportMode.entries.forEach { assertEquals(it, BandaMapeador.aModoDominio(BandaMapeador.aModoUi(it))) }
    }
}
