package com.dpinta.agenda.ui.actividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.data.agenda.AgendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ActividadesViewModel @Inject constructor(
    repositorio: AgendaRepository,
    reloj: Clock,
    resumen: com.dpinta.agenda.data.agenda.ResumenAsistencia,
    programador: com.dpinta.agenda.avisos.ProgramadorAvisos,
) : ViewModel() {

    /** Cada clase con su asistencia (anexo 2026-11-10-agenda-ia, dirección A en el teléfono): se recalcula al marcar. */
    val estado: StateFlow<ActividadesUiState> = kotlinx.coroutines.flow.combine(repositorio.agenda(), repositorio.asistencias()) { agenda, _ -> agenda }
        .map { agenda ->
            val base = ActividadesMapeador.estado(agenda, LocalDate.now(reloj))
            if (base !is ActividadesUiState.Lista) return@map base
            val porId = resumen.porMateria(agenda, programador::sesiones, java.time.LocalDateTime.now(reloj)).associateBy { it.activityId }
            base.copy(secciones = base.secciones.map { sec -> sec.copy(filas = sec.filas.map { it.copy(asistencia = porId[it.id]) }) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActividadesUiState.Cargando)
}
