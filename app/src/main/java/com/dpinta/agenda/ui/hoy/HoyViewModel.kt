package com.dpinta.agenda.ui.hoy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.Conflict
import com.dpinta.agenda.domain.ConflictDetector
import com.dpinta.agenda.domain.DayPlanner
import com.dpinta.agenda.domain.DepartureCalculator
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.ScheduleExpander
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.ui.components.banda.AccionBanda
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import com.dpinta.agenda.data.agenda.SesionesEnCurso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Estado de la pantalla Hoy. Toda la aritmética de tiempos vive en core/domain
 * (ScheduleExpander, DayPlanner, DepartureCalculator, Trip, ConflictDetector); aquí solo se
 * orquesta y se traduce con [BandaMapeador]. El reloj es inyectable para las pruebas.
 */
private typealias Clave = SesionesEnCurso.Sesion

@HiltViewModel
class HoyViewModel @Inject constructor(
    private val repositorio: AgendaRepository,
    private val reloj: Clock,
    sesiones: SesionesEnCurso,
) : ViewModel() {

    private val salida = sesiones.salida
    private val llegadas = sesiones.llegadas
    private val modos = MutableStateFlow<Map<Long, TransportMode>>(emptyMap())
    private val pospuesto = sesiones.pospuesto

    /** Sesión a la que apunta la banda ahora mismo (destino de las acciones). */
    @Volatile private var claveBanda: Clave? = null

    /** Un pulso por minuto, alineado al cambio de minuto del reloj. */
    private val minuto: Flow<Instant> = flow {
        while (true) {
            val ahora = reloj.instant()
            emit(ahora)
            delay(MINUTO_MS - Math.floorMod(ahora.toEpochMilli(), MINUTO_MS))
        }
    }

    val estado: StateFlow<HoyUiState> =
        combine(repositorio.agenda(), minuto, salida, llegadas, modos) { agenda, ahora, salida, llegadas, modos ->
            construir(agenda, ahora, salida, llegadas, modos)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HoyUiState.Cargando)

    /** «+5 min» pospone el aviso; lo consumirá el paso de notificaciones. */
    val avisoPospuestoHasta: StateFlow<Instant?> = pospuesto

    fun onAccion(accion: AccionBanda) {
        val clave = claveBanda ?: return
        when (accion) {
            AccionBanda.VoySaliendo -> salida.value = clave to reloj.instant()
            AccionBanda.MasCinco -> pospuesto.value = reloj.instant() + Duration.ofMinutes(5)
            AccionBanda.YaLlegue -> {
                llegadas.update { it + clave }
                salida.value = null
            }
        }
    }

    /** C1.3: tocar el modo pasa al siguiente para esta actividad. */
    fun onCambiarModo() {
        val clave = claveBanda ?: return
        val actual = estado.value as? HoyUiState.Dia ?: return
        val modoUi = actual.banda?.modo ?: return
        modos.update { it + (clave.actividadId to BandaMapeador.aModoDominio(modoUi.siguiente())) }
    }

    private suspend fun construir(
        agenda: Agenda,
        ahora: Instant,
        salida: Pair<Clave, Instant>?,
        llegadas: Set<Clave>,
        modos: Map<Long, TransportMode>,
    ): HoyUiState {
        if (agenda.vacia) return HoyUiState.PrimerUso
        val zona = reloj.zone
        val ahoraLocal = LocalDateTime.ofInstant(ahora, zona)
        val hoy = ahoraLocal.toLocalDate()

        val sesiones = ScheduleExpander.expand(
            rules = agenda.reglas,
            oneOff = agenda.puntuales,
            placeOf = { agenda.actividades[it]?.lugarId },
            from = hoy,
            to = hoy.plusDays(DIAS_SIGUIENTE),
            semester = agenda.semestreEn(hoy),
            exceptions = agenda.excepciones,
        )
        val deHoy = sesiones.filter { it.start.toLocalDate() == hoy }
        val conflictos = ConflictDetector.detect(deHoy) { a, b -> agenda.minutosEntre(a, b) }

        val siguiente = DayPlanner.next(deHoy, ahoraLocal)
        claveBanda = siguiente?.let { Clave(it.activityId, it.start) }
        val banda = siguiente?.let { sesion ->
            val act = agenda.actividades.getValue(sesion.activityId)
            val clave = Clave(sesion.activityId, sesion.start)
            val modo = modos[sesion.activityId] ?: act.modo
            val conTraslado = DayPlanner.needsTravel(DayPlanner.previous(deHoy, sesion), sesion) && clave !in llegadas
            val estimacion = if (conTraslado) sesion.placeId?.let { repositorio.estimacion(it, modo) } else null
            val inicio = sesion.start.atZone(zona).toInstant()
            val plan = estimacion?.let { DepartureCalculator.plan(inicio, act.margen, it, ahora) }
            BandaMapeador.modelo(
                sesion = SesionBanda(
                    actividad = act.titulo,
                    salon = act.salon,
                    lugar = act.lugarId?.let { agenda.lugares[it]?.nombre }.orEmpty(),
                    inicio = inicio,
                    margen = act.margen,
                    modo = modo,
                ),
                plan = plan,
                estimacion = estimacion,
                salioEn = salida?.takeIf { it.first == clave }?.second,
                ahora = ahora,
                zona = zona,
            )
        }

        val filas = deHoy.map { s ->
            val act = agenda.actividades.getValue(s.activityId)
            FilaHoy(
                clave = "${s.activityId}@${s.start}",
                actividadId = s.activityId,
                inicio = s.start.toLocalTime(),
                fin = s.end.toLocalTime(),
                titulo = act.titulo,
                tipo = tipoFila(act.tipo),
                salon = act.salon,
                lugar = act.lugarId?.let { agenda.lugares[it]?.nombre }.orEmpty(),
                pasada = !s.end.isAfter(ahoraLocal),
                conflicto = conflictoDe(s, conflictos, agenda),
            )
        }
        val indiceAhora = deHoy.indexOfFirst { it.start.isAfter(ahoraLocal) }.let { if (it < 0) filas.size else it }
        val proximo = if (filas.isEmpty()) {
            sesiones.firstOrNull { it.start.toLocalDate().isAfter(hoy) }?.let { s ->
                val act = agenda.actividades.getValue(s.activityId)
                SiguienteDia(s.start.toLocalDate(), s.start.toLocalTime(), act.titulo, act.salon)
            }
        } else {
            null
        }
        return HoyUiState.Dia(hoy, ahoraLocal.toLocalTime(), banda, filas, indiceAhora, proximo)
    }

    private fun conflictoDe(s: Occurrence, conflictos: List<Conflict>, agenda: Agenda): ConflictoFila? =
        conflictos.firstNotNullOfOrNull { c ->
            when (c) {
                is Conflict.NotEnoughTravel ->
                    if (c.to == s) ConflictoFila.Traslado(c.availableMinutes, c.neededMinutes) else null
                // El solape se anota en la segunda sesión y nombra a la primera (C10).
                is Conflict.Overlap ->
                    if (c.second == s) {
                        ConflictoFila.Cruce(agenda.actividades[c.first.activityId]?.titulo.orEmpty(), c.first.start.toLocalTime())
                    } else {
                        null
                    }
            }
        }

    private fun tipoFila(tipo: ActivityKind) = when (tipo) {
        ActivityKind.CLASE, ActivityKind.EXAMEN -> TipoFila.Clase
        ActivityKind.TRABAJO -> TipoFila.Trabajo
        ActivityKind.PUNTUAL, ActivityKind.OTRO -> TipoFila.Puntual
    }

    private companion object {
        const val MINUTO_MS = 60_000L
        const val DIAS_SIGUIENTE = 7L
    }
}
