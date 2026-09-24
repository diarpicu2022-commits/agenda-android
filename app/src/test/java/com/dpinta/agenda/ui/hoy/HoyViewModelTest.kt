package com.dpinta.agenda.ui.hoy

import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.AgendaEnMemoria
import com.dpinta.agenda.ui.components.banda.AccionBanda
import com.dpinta.agenda.ui.components.banda.EstadoBanda
import com.dpinta.agenda.ui.components.banda.ModoTransporte
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HoyViewModelTest {

    private val zona = ZoneId.of("America/Bogota")
    private val dia = LocalDate.of(2026, 9, 23)

    /** Reloj que avanza con el tiempo virtual de las pruebas. */
    private class RelojVirtual(private val base: Instant, private val zona: ZoneId, private val alcance: TestScope) : Clock() {
        override fun getZone(): ZoneId = zona
        override fun withZone(zone: ZoneId): Clock = RelojVirtual(base, zone, alcance)
        override fun instant(): Instant = base.plusMillis(alcance.testScheduler.currentTime)
    }

    @After fun limpiar() = Dispatchers.resetMain()

    private fun TestScope.preparar(h: Int, m: Int, agenda: ((LocalDate) -> Agenda)? = null): Pair<HoyViewModel, () -> HoyUiState> {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val reloj = RelojVirtual(dia.atTime(h, m).atZone(zona).toInstant(), zona, this)
        val repo = AgendaEnMemoria(reloj)
        agenda?.let { repo.reemplazar(it(dia)) }
        val vm = HoyViewModel(repo, reloj)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        runCurrent()
        return vm to { vm.estado.value }
    }

    private fun HoyUiState.dia() = this as HoyUiState.Dia

    @Test fun primerUsoSiNoHayActividades() = runTest {
        val (_, estado) = preparar(7, 0) { Agenda.VACIA }
        assertEquals(HoyUiState.PrimerUso, estado())
    }

    @Test fun alAmanecerLaBandaApuntaACalculoEnEspera() = runTest {
        val (_, estado) = preparar(6, 51)
        val d = estado().dia()
        assertEquals(4, d.filas.size)
        assertEquals(0, d.indiceAhora)
        val banda = d.banda!!
        assertEquals("Cálculo diferencial", banda.actividad)
        assertEquals(EstadoBanda.Espera, banda.estado)
        assertEquals(LocalTime.of(7, 30), banda.horaSalida)
        assertEquals(ConflictoFila.Traslado(25, 35), d.filas.first { it.titulo == "Turno" }.conflicto)
    }

    @Test fun elPulsoDeMinutoCambiaElEstado() = runTest {
        val (_, estado) = preparar(7, 14)
        assertEquals(EstadoBanda.Espera, estado().dia().banda!!.estado)
        advanceTimeBy(60_000L * 2); runCurrent()
        assertEquals(EstadoBanda.Preparate, estado().dia().banda!!.estado)
    }

    @Test fun voySaliendoPasaAEnCaminoYYaLlegueASinTraslado() = runTest {
        val (vm, estado) = preparar(7, 31)
        assertEquals(EstadoBanda.SalYa, estado().dia().banda!!.estado)
        vm.onAccion(AccionBanda.VoySaliendo); runCurrent()
        val enCamino = estado().dia().banda!!
        assertEquals(EstadoBanda.EnCamino, enCamino.estado)
        assertEquals(LocalTime.of(7, 54), enCamino.horaLlegada)
        vm.onAccion(AccionBanda.YaLlegue); runCurrent()
        assertEquals(EstadoBanda.SinTraslado, estado().dia().banda!!.estado)
    }

    @Test fun masCincoPosponeElAviso() = runTest {
        val (vm, _) = preparar(7, 20)
        vm.onAccion(AccionBanda.MasCinco)
        assertEquals(dia.atTime(7, 25).atZone(zona).toInstant(), vm.avisoPospuestoHasta.value)
    }

    @Test fun cambiarModoRecalculaConElDominio() = runTest {
        val (vm, estado) = preparar(6, 51)
        vm.onCambiarModo(); runCurrent()
        val banda = estado().dia().banda!!
        assertEquals(ModoTransporte.APie, banda.modo)
        // A pie 48 min + 7 de margen: salir 7:05.
        assertEquals(LocalTime.of(7, 5), banda.horaSalida)
    }

    @Test fun mismaSedeQueLaAnteriorEsSinTraslado() = runTest {
        val (_, estado) = preparar(10, 5)
        val d = estado().dia()
        assertEquals("Física mecánica", d.banda!!.actividad)
        assertEquals(EstadoBanda.SinTraslado, d.banda!!.estado)
        assertTrue(d.filas.first().pasada)
        assertEquals(1, d.indiceAhora)
    }

    @Test fun despuesDeLaUltimaNoHayBanda() = runTest {
        val (_, estado) = preparar(21, 40)
        val d = estado().dia()
        assertNull(d.banda)
        assertEquals(4, d.indiceAhora)
    }

    @Test fun diaSinNadaMuestraLaSiguiente() = runTest {
        val (_, estado) = preparar(9, 0) { hoy ->
            val base = AgendaEnMemoria.ejemplo(hoy)
            // Solo la puntual, movida a mañana.
            base.copy(reglas = emptyList(), puntuales = base.puntuales.map { it.copy(start = it.start.plusDays(1), end = it.end.plusDays(1)) })
        }
        val d = estado().dia()
        assertTrue(d.filas.isEmpty())
        assertEquals("Entrega informe", d.siguiente!!.titulo)
        assertEquals(dia.plusDays(1), d.siguiente!!.fecha)
    }

    @Test fun unSolapeSeAnotaEnLaSegundaYNombraALaPrimera() = runTest {
        val (_, estado) = preparar(7, 0) { AgendaEnMemoria.ejemploConCruce(it) }
        val d = estado().dia()
        assertEquals(ConflictoFila.Cruce("Cálculo diferencial", LocalTime.of(8, 0)), d.filas.first { it.titulo == "Física mecánica" }.conflicto)
        assertNull(d.filas.first { it.titulo == "Cálculo diferencial" }.conflicto)
    }
}
