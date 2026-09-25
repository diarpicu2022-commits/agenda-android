package com.dpinta.agenda.data.agenda

import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.domain.WeeklyRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * Repositorio TEMPORAL en memoria con un día de ejemplo (el de los bocetos del anexo).
 * Las series valen todos los días para que el ejemplo se vea cualquier día que se abra la app.
 * Se sustituye por Room en el paso de persistencia.
 */
/** Doble de pruebas en memoria (antes era la fuente de la app; ahora la app usa AgendaRoom). */
class AgendaEnMemoria(private val reloj: Clock) : AgendaRepository {

    private val estado = MutableStateFlow(ejemplo(LocalDate.now(reloj)))

    override fun agenda(): Flow<Agenda> = estado

    override suspend fun estimacion(lugarId: Long, modo: TransportMode): TravelEstimate? {
        val minutos = DESDE_CASA[lugarId]?.get(modo) ?: return null
        // Estimación reciente: calculada hace 3 minutos.
        return TravelEstimate(Duration.ofMinutes(minutos), modo, reloj.instant() - Duration.ofMinutes(3), fromCache = false)
    }

    override suspend fun guardarExcepcion(excepcion: com.dpinta.agenda.domain.SessionException) {
        estado.update { it.copy(excepciones = it.excepciones.filterNot { e -> e.activityId == excepcion.activityId && e.date == excepcion.date } + excepcion) }
    }

    override suspend fun quitarExcepcion(actividadId: Long, fecha: LocalDate) {
        estado.update { it.copy(excepciones = it.excepciones.filterNot { e -> e.activityId == actividadId && e.date == fecha }) }
    }

    private val guardados = MutableStateFlow<List<SemestreGuardado>>(emptyList())

    override fun semestres(): Flow<List<SemestreGuardado>> = guardados

    override suspend fun guardarSemestre(
        nombre: String,
        inicio: LocalDate,
        fin: LocalDate,
        diasSinClase: Map<LocalDate, String>,
        id: Long?,
    ): Long {
        val nuevoId = id ?: ((guardados.value.maxOfOrNull { it.id } ?: 0) + 1)
        guardados.update { lista -> lista.filter { it.id != nuevoId } + SemestreGuardado(nuevoId, nombre, inicio, fin, diasSinClase) }
        sincronizar()
        return nuevoId
    }

    override suspend fun eliminarSemestre(id: Long) {
        guardados.update { lista -> lista.filter { it.id != id } }
        sincronizar()
    }

    private fun sincronizar() = estado.update { a ->
        a.copy(semestres = guardados.value.sortedBy { it.inicio }.map { com.dpinta.agenda.domain.Semester(it.inicio, it.fin, it.diasSinClase.keys) })
    }

    override suspend fun guardarEstimacion(lugarId: Long, estimacion: TravelEstimate) = Unit

    override suspend fun guardarTraslado(desde: Long, hasta: Long, minutos: Long) {
        estado.update { it.copy(traslados = it.traslados + ((desde to hasta) to minutos)) }
    }

    override suspend fun guardar(actividad: ActividadAGuardar): Long {
        var id = 0L
        estado.update { agenda ->
            id = actividad.id ?: ((agenda.actividades.keys.maxOrNull() ?: 0L) + 1)
            val lugar = agenda.lugarLlamado(actividad.lugar)
                ?: actividad.lugar.trim().takeIf { it.isNotEmpty() }
                    ?.let { Lugar((agenda.lugares.keys.maxOrNull() ?: 100L) + 1, it) }
            val nueva = Actividad(
                id = id,
                titulo = actividad.titulo.trim(),
                tipo = actividad.tipo,
                salon = actividad.salon.trim(),
                lugarId = lugar?.id,
                margen = actividad.margen,
                modo = actividad.modo,
                aviso = actividad.aviso,
            )
            val sinEsta = agenda.copy(
                reglas = agenda.reglas.filterNot { it.activityId == id },
                puntuales = agenda.puntuales.filterNot { it.activityId == id },
            )
            when (val c = actividad.cuando) {
                is Cuando.Semanal -> sinEsta.copy(reglas = sinEsta.reglas + WeeklyRule(id, c.dias, c.inicio, c.fin, c.desde, c.hasta))
                is Cuando.Puntual -> sinEsta.copy(puntuales = sinEsta.puntuales + Occurrence(id, lugar?.id, c.fecha.atTime(c.inicio), c.fecha.atTime(c.fin)))
            }.copy(
                actividades = agenda.actividades + (id to nueva),
                lugares = if (lugar != null) agenda.lugares + (lugar.id to lugar) else agenda.lugares,
            )
        }
        return id
    }

    override suspend fun eliminar(id: Long) {
        estado.update { a ->
            a.copy(
                actividades = a.actividades - id,
                reglas = a.reglas.filterNot { it.activityId == id },
                puntuales = a.puntuales.filterNot { it.activityId == id },
            )
        }
    }

    /** Sustituye la agenda. */
    internal fun reemplazar(agenda: Agenda) {
        estado.value = agenda
    }

    companion object {
        const val CAMPUS = 10L
        const val TIENDA = 20L

        private val DESDE_CASA = mapOf(
            CAMPUS to mapOf(
                TransportMode.TRANSPORTE_PUBLICO to 23L,
                TransportMode.A_PIE to 48L,
                TransportMode.CARRO to 14L,
                TransportMode.MOTO to 11L,
            ),
            TIENDA to mapOf(
                TransportMode.TRANSPORTE_PUBLICO to 31L,
                TransportMode.A_PIE to 62L,
                TransportMode.CARRO to 18L,
                TransportMode.MOTO to 15L,
            ),
        )

        private val ENTRE_LUGARES = mapOf((CAMPUS to TIENDA) to 35L, (TIENDA to CAMPUS) to 35L)

        /** Variante con un solape: Física pasa a las 9:00, dentro de Cálculo (8:00-10:00). */
        fun ejemploConCruce(hoy: LocalDate): Agenda {
            val base = ejemplo(hoy)
            return base.copy(
                reglas = base.reglas.map { if (it.activityId == 2L) it.copy(start = LocalTime.of(9, 0), end = LocalTime.of(10, 30)) else it },
            )
        }

        fun ejemplo(hoy: LocalDate): Agenda {
            val todos = DayOfWeek.entries.toSet()
            val desde = hoy.minusDays(120)
            val hasta = hoy.plusDays(120)
            val actividades = listOf(
                Actividad(1, "Cálculo diferencial", ActivityKind.CLASE, "B-204", CAMPUS, Duration.ofMinutes(7), TransportMode.TRANSPORTE_PUBLICO),
                Actividad(2, "Física mecánica", ActivityKind.CLASE, "L-3", CAMPUS, Duration.ofMinutes(7), TransportMode.TRANSPORTE_PUBLICO),
                Actividad(3, "Turno", ActivityKind.TRABAJO, "Caja 2", TIENDA, Duration.ofMinutes(10), TransportMode.TRANSPORTE_PUBLICO),
                Actividad(4, "Entrega informe", ActivityKind.PUNTUAL, "", null, Duration.ZERO, TransportMode.TRANSPORTE_PUBLICO),
            ).associateBy { it.id }
            val reglas = listOf(
                WeeklyRule(1, todos, LocalTime.of(8, 0), LocalTime.of(10, 0), desde, hasta),
                WeeklyRule(2, todos, LocalTime.of(10, 30), LocalTime.of(12, 0), desde, hasta),
                WeeklyRule(3, todos, LocalTime.of(12, 25), LocalTime.of(18, 0), desde, hasta),
            )
            val puntuales = listOf(Occurrence(4, null, hoy.atTime(21, 0), hoy.atTime(21, 30)))
            val lugares = mapOf(CAMPUS to Lugar(CAMPUS, "Campus"), TIENDA to Lugar(TIENDA, "Tienda centro"))
            return Agenda(actividades, lugares, reglas, puntuales, traslados = ENTRE_LUGARES)
        }
    }
}
