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
) : ViewModel() {

    val estado: StateFlow<ActividadesUiState> = repositorio.agenda()
        .map { ActividadesMapeador.estado(it, LocalDate.now(reloj)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActividadesUiState.Cargando)
}
