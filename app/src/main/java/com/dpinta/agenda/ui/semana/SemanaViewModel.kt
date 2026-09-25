package com.dpinta.agenda.ui.semana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.data.agenda.AgendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SemanaViewModel @Inject constructor(
    repositorio: AgendaRepository,
    private val reloj: Clock,
) : ViewModel() {

    /** Semanas desde la actual: −1 la anterior, +1 la siguiente. */
    private val desplazamiento = MutableStateFlow(0L)

    /** Un pulso por minuto para la línea de «ahora». */
    private val minuto: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now(reloj))
            delay(MINUTO_MS - Math.floorMod(reloj.millis(), MINUTO_MS))
        }
    }

    val estado: StateFlow<SemanaUiState> =
        combine(repositorio.agenda(), desplazamiento, minuto) { agenda, semanas, ahora ->
            SemanaMapeador.estado(agenda, SemanaMapeador.lunesDe(ahora.toLocalDate()).plusWeeks(semanas), ahora)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SemanaUiState.Cargando)

    fun anterior() = desplazamiento.update { it - 1 }

    fun siguiente() = desplazamiento.update { it + 1 }

    fun estaSemana() {
        desplazamiento.value = 0
    }

    private companion object {
        const val MINUTO_MS = 60_000L
    }
}
