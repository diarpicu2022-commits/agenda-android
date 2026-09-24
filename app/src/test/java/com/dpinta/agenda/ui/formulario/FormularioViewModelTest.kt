package com.dpinta.agenda.ui.formulario

import androidx.lifecycle.SavedStateHandle
import com.dpinta.agenda.data.agenda.ActividadAGuardar
import com.dpinta.agenda.data.agenda.AgendaEnMemoria
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.AvisosGuardado
import com.dpinta.agenda.data.agenda.Guardado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FormularioViewModelTest {

    private val zona = ZoneId.of("America/Bogota")
    private val hoy = LocalDate.of(2026, 9, 24)
    private val reloj = Clock.fixed(hoy.atTime(7, 0).atZone(zona).toInstant(), zona)

    private class FallaAlGuardar(private val real: AgendaRepository) : AgendaRepository by real {
        override suspend fun guardar(actividad: ActividadAGuardar): Long = error("disco lleno")
    }

    @After fun limpiar() = Dispatchers.resetMain()

    private data class Montaje(val vm: FormularioViewModel, val repo: AgendaEnMemoria, val avisos: MutableList<Guardado>)

    private fun TestScope.montar(id: Long = -1, escenario: String = "", falla: Boolean = false): Montaje {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = AgendaEnMemoria(reloj)
        val bus = AvisosGuardado()
        val recibidos = mutableListOf<Guardado>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { bus.avisos.collect { recibidos += it } }
        val vm = FormularioViewModel(
            if (falla) FallaAlGuardar(repo) else repo,
            bus,
            reloj,
            SavedStateHandle(mapOf("id" to id, "escenario" to escenario)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        runCurrent()
        return Montaje(vm, repo, recibidos)
    }

    private fun FormularioViewModel.rellenar() {
        onCambio { it.copy(titulo = "Álgebra lineal", inicio = "14:00", fin = "16:00", lugar = "Campus", salon = "C-101") }
        onDia(DayOfWeek.TUESDAY)
        onDia(DayOfWeek.THURSDAY)
    }

    @Test fun nuevoSinErroresVisiblesHastaIntentarGuardar() = runTest {
        val (vm) = montar()
        val e = vm.estado.value
        assertTrue(e.esNueva)
        assertFalse(e.cargando)
        assertTrue(e.errores.isEmpty())
        assertNull(e.borradorValido)
        vm.onGuardar(); runCurrent()
        assertEquals(Validador.MSG_TITULO, vm.estado.value.errores[Campo.TITULO])
        assertEquals(FaseGuardado.Editando, vm.estado.value.fase)
    }

    @Test fun guardarValidoLoAnadeYAvisa() = runTest {
        val (vm, repo, avisos) = montar()
        vm.rellenar(); runCurrent()
        assertTrue(vm.estado.value.borradorValido != null)
        vm.onGuardar(); runCurrent()
        assertEquals(FaseGuardado.Guardado, vm.estado.value.fase)
        val agenda = repo.agenda().first()
        val nueva = agenda.actividades.values.single { it.titulo == "Álgebra lineal" }
        assertEquals("C-101", nueva.salon)
        assertEquals(AgendaEnMemoria.CAMPUS, nueva.lugarId) // «Campus» ya existía: se reutiliza
        assertEquals(listOf(Guardado(nueva.id, "Álgebra lineal", nueva = true)), avisos)
    }

    @Test fun lugarNuevoSeCrea() = runTest {
        val (vm, repo) = montar()
        vm.rellenar(); vm.onCambio { it.copy(lugar = "Biblioteca central") }
        vm.onGuardar(); runCurrent()
        val agenda = repo.agenda().first()
        assertTrue(agenda.lugares.values.any { it.nombre == "Biblioteca central" })
    }

    @Test fun conflictosAparecenEnLaMismaPantallaYNoBloquean() = runTest {
        val (vm, repo) = montar(escenario = "conflictos")
        val conflictos = vm.estado.value.conflictos
        assertTrue(conflictos.isNotEmpty())
        vm.onGuardar(); runCurrent()
        assertEquals(FaseGuardado.Guardado, vm.estado.value.fase)
        assertTrue(repo.agenda().first().actividades.values.any { it.titulo == "Inventario" })
    }

    @Test fun errorAlGuardarConservaLosDatos() = runTest {
        val (vm) = montar(falla = true)
        vm.rellenar()
        vm.onGuardar(); runCurrent()
        val fase = vm.estado.value.fase
        assertTrue(fase is FaseGuardado.Error)
        assertEquals("No se pudo guardar. Tus datos siguen aquí.", (fase as FaseGuardado.Error).mensaje)
        assertEquals("Álgebra lineal", vm.estado.value.formulario.titulo)
        vm.onCambio { it.copy(salon = "C-102") }; runCurrent()
        assertEquals(FaseGuardado.Editando, vm.estado.value.fase)
    }

    @Test fun editarCargaYSustituye() = runTest {
        val (vm, repo, avisos) = montar(id = 1)
        val e = vm.estado.value
        assertFalse(e.esNueva)
        assertEquals("Cálculo diferencial", e.formulario.titulo)
        vm.onCambio { it.copy(salon = "B-210") }
        vm.onGuardar(); runCurrent()
        val agenda = repo.agenda().first()
        assertEquals("B-210", agenda.actividades.getValue(1).salon)
        assertEquals(1, agenda.reglas.count { it.activityId == 1L })
        assertEquals(false, avisos.single().nueva)
    }

    @Test fun editarUnaQueNoExiste() = runTest {
        val (vm) = montar(id = 999)
        assertTrue(vm.estado.value.noEncontrada)
    }
}
