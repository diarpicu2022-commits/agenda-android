package com.dpinta.agenda.ui.formulario

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.data.agenda.ActividadAGuardar
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.AvisosGuardado
import com.dpinta.agenda.data.agenda.Guardado
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.TransportMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/** Fase del guardado (C10: estados de la pantalla). */
sealed interface FaseGuardado {
    data object Editando : FaseGuardado
    data object Guardando : FaseGuardado
    data class Error(val mensaje: String) : FaseGuardado
    data object Guardado : FaseGuardado
}

data class FormularioUiState(
    val cargando: Boolean,
    val esNueva: Boolean,
    /** La actividad a editar ya no existe. */
    val noEncontrada: Boolean,
    val formulario: Formulario,
    /** Errores visibles: solo tras el primer intento de guardar (no se riñe mientras se escribe). */
    val errores: Map<Campo, String>,
    /** Borrador ya válido: para la frase-resumen. */
    val borradorValido: ActividadAGuardar?,
    val conflictos: List<AvisoConflicto>,
    val fase: FaseGuardado,
)

/**
 * Crear o editar una actividad (anexo §7, flujo 1). La validación y los conflictos son puros
 * (Formulario.kt) y los conflictos usan el ConflictDetector del dominio.
 * Argumentos de la ruta: `id` (−1 = nueva) y `escenario` (solo depuración, para capturas).
 */
@HiltViewModel
class FormularioViewModel @Inject constructor(
    private val repositorio: AgendaRepository,
    private val avisos: AvisosGuardado,
    private val reloj: Clock,
    estadoGuardado: SavedStateHandle,
) : ViewModel() {

    private val id: Long? = estadoGuardado.get<Long>("id")?.takeIf { it >= 0 }
    private val hoy: LocalDate get() = LocalDate.now(reloj)

    private val formulario = MutableStateFlow(Formulario.nuevo(hoy))
    private val intentado = MutableStateFlow(false)
    private val fase = MutableStateFlow<FaseGuardado>(FaseGuardado.Editando)
    private val cargando = MutableStateFlow(id != null)
    private val noEncontrada = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val agenda = repositorio.agenda().first()
            if (id != null) {
                val f = formularioDe(agenda, id, hoy)
                if (f == null) noEncontrada.value = true else formulario.value = f
            } else {
                formulario.value = Formulario.nuevo(hoy, agenda.semestre?.end)
                escenario(estadoGuardado.get<String>("escenario").orEmpty())
            }
            cargando.value = false
        }
    }

    val estado: StateFlow<FormularioUiState> =
        combine(formulario, intentado, fase, repositorio.agenda(), combine(cargando, noEncontrada) { c, n -> c to n }) { f, intento, fase, agenda, (carga, falta) ->
            val validacion = Validador.validar(f, hoy, id)
            val conflictos = validacion.aGuardar?.let { conflictosDelBorrador(agenda, it, repositorio::minutosEntre) }.orEmpty()
            FormularioUiState(
                cargando = carga,
                esNueva = id == null,
                noEncontrada = falta,
                formulario = f,
                errores = if (intento) validacion.errores else emptyMap(),
                borradorValido = validacion.aGuardar,
                conflictos = conflictos,
                fase = fase,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FormularioUiState(true, id == null, false, formulario.value, emptyMap(), null, emptyList(), FaseGuardado.Editando),
        )

    fun onCambio(cambio: (Formulario) -> Formulario) {
        formulario.update(cambio)
        if (fase.value is FaseGuardado.Error) fase.value = FaseGuardado.Editando
    }

    fun onTipo(t: ActivityKind) = onCambio { it.copy(tipo = t) }
    fun onSemanal(s: Boolean) = onCambio { it.copy(semanal = s) }
    fun onDia(d: DayOfWeek) = onCambio { it.copy(dias = if (d in it.dias) it.dias - d else it.dias + d) }
    fun onModo(m: TransportMode) = onCambio { it.copy(modo = m) }

    /** Guarda aunque haya conflictos: avisan, no bloquean (anexo §7, flujo 1). */
    fun onGuardar() {
        if (fase.value == FaseGuardado.Guardando) return
        intentado.value = true
        val borrador = Validador.validar(formulario.value, hoy, id).aGuardar ?: return
        fase.value = FaseGuardado.Guardando
        viewModelScope.launch {
            fase.value = try {
                val nuevoId = repositorio.guardar(borrador)
                avisos.publicar(Guardado(nuevoId, borrador.titulo, nueva = id == null))
                FaseGuardado.Guardado
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FaseGuardado.Error("No se pudo guardar. Tus datos siguen aquí.")
            }
        }
    }

    /** Solo depuración: estados reproducibles para las capturas del paso 5. */
    private fun escenario(nombre: String) {
        val base = formulario.value
        when (nombre) {
            "errores" -> {
                formulario.value = base.copy(titulo = "", inicio = "25:00", fin = "8")
                intentado.value = true
            }
            "completo" -> formulario.value = base.copy(
                titulo = "Álgebra lineal",
                dias = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                inicio = "14:00",
                fin = "16:00",
                lugar = "Campus",
                salon = "C-101",
            )
            "conflictos" -> formulario.value = base.copy(
                tipo = ActivityKind.TRABAJO,
                titulo = "Inventario",
                dias = setOf(DayOfWeek.MONDAY),
                inicio = "10:15",
                fin = "11:00",
                lugar = "Tienda centro",
                salon = "Bodega",
            )
            "puntual" -> formulario.value = base.copy(
                tipo = ActivityKind.EXAMEN,
                titulo = "Parcial de Cálculo",
                semanal = false,
                inicio = "18:00",
                fin = "20:00",
                lugar = "Campus",
                salon = "Auditorio 2",
            )
        }
    }
}
