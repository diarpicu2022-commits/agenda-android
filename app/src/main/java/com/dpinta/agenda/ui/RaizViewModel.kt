package com.dpinta.agenda.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.AvisosGuardado
import com.dpinta.agenda.data.agenda.Guardado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Vive con la raíz: muestra «Guardado» y deshace una actividad recién creada. */
@HiltViewModel
class RaizViewModel @Inject constructor(
    avisos: AvisosGuardado,
    private val repositorio: AgendaRepository,
) : ViewModel() {
    val guardados: SharedFlow<Guardado> = avisos.avisos

    /** Solo las nuevas se pueden deshacer (borrarlas); una edición no guarda la versión anterior. */
    fun deshacer(g: Guardado) {
        if (!g.nueva) return
        viewModelScope.launch { repositorio.eliminar(g.id) }
    }
}
