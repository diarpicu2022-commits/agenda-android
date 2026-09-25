package com.dpinta.agenda.ui.semestre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.data.agenda.AgendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class SemestreUiState(
    val formulario: SemestreFormulario = SemestreFormulario(),
    val errores: ErroresSemestre = ErroresSemestre(),
    val editando: Boolean = false,
    val cargando: Boolean = true,
    /** Primer toque en «Eliminar semestre»: pide el segundo para confirmar. */
    val confirmarEliminar: Boolean = false,
    val terminado: Boolean = false,
)

@HiltViewModel
class SemestreViewModel @Inject constructor(
    private val repositorio: AgendaRepository,
    private val reloj: Clock,
    estadoGuardado: SavedStateHandle,
) : ViewModel() {

    private val id: Long? = estadoGuardado.get<Long>("id")?.takeIf { it >= 0 }
    private val hoy: LocalDate get() = LocalDate.now(reloj)
    private var intentado = false

    private val _estado = MutableStateFlow(SemestreUiState(editando = id != null))
    val estado: StateFlow<SemestreUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val guardado = id?.let { buscado -> repositorio.semestres().first().firstOrNull { it.id == buscado } }
            _estado.update { it.copy(formulario = guardado?.let(SemestreFormulario::de) ?: it.formulario, cargando = false) }
        }
    }

    private fun editar(cambio: (SemestreFormulario) -> SemestreFormulario) = _estado.update { e ->
        val f = cambio(e.formulario)
        e.copy(formulario = f, errores = if (intentado) SemestreLogica.validar(f, hoy) else e.errores, confirmarEliminar = false)
    }

    fun nombre(v: String) = editar { it.copy(nombre = v) }
    fun inicio(v: String) = editar { SemestreLogica.conFechas(it.copy(inicio = v), hoy) }
    fun fin(v: String) = editar { SemestreLogica.conFechas(it.copy(fin = v), hoy) }
    fun nuevoDia(v: String) = _estado.update { it.copy(formulario = it.formulario.copy(nuevoDia = v), errores = it.errores.copy(nuevoDia = null)) }
    fun nuevoMotivo(v: String) = editar { it.copy(nuevoMotivo = v) }
    fun quitar(fecha: LocalDate) = editar { SemestreLogica.quitar(it, fecha) }

    fun anadir() = _estado.update { e ->
        val (f, error) = SemestreLogica.anadir(e.formulario, hoy)
        e.copy(formulario = f, errores = e.errores.copy(nuevoDia = error))
    }

    fun guardar() {
        intentado = true
        val f = _estado.value.formulario
        val errores = SemestreLogica.validar(f, hoy)
        _estado.update { it.copy(errores = errores) }
        if (!errores.ninguno) return
        val inicio = SemestreLogica.inicio(f, hoy) ?: return
        val fin = SemestreLogica.fin(f, hoy) ?: return
        viewModelScope.launch {
            // Solo los días dentro del semestre: si se acortó, los de fuera sobran.
            val dias = f.dias.filterKeys { !it.isBefore(inicio) && !it.isAfter(fin) }
            repositorio.guardarSemestre(f.nombre.trim(), inicio, fin, dias, id)
            _estado.update { it.copy(terminado = true) }
        }
    }

    fun eliminar() {
        val borrar = id ?: return
        if (!_estado.value.confirmarEliminar) {
            _estado.update { it.copy(confirmarEliminar = true) }
            return
        }
        viewModelScope.launch {
            repositorio.eliminarSemestre(borrar)
            _estado.update { it.copy(terminado = true) }
        }
    }
}
