package com.dpinta.agenda.data.agenda

import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.DayPlanner
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.ColombianHolidays
import com.dpinta.agenda.domain.Semester
import com.dpinta.agenda.domain.SessionException
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.domain.WeeklyRule
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Lo que el usuario ve de una actividad. El «dónde» es Lugar (mapa) + salón (texto libre). */
data class Actividad(
    val id: Long,
    val titulo: String,
    val tipo: ActivityKind,
    val salon: String,
    val lugarId: Long?,
    val margen: Duration,
    val modo: TransportMode,
    /** Cuánto antes de empezar se avisa «empieza X» (C10). */
    val aviso: Duration = Duration.ofMinutes(15),
)

data class Lugar(val id: Long, val nombre: String)

/** Cuándo ocurre una actividad: serie semanal o una sola vez. */
sealed interface Cuando {
    data class Semanal(
        val dias: Set<DayOfWeek>,
        val inicio: LocalTime,
        val fin: LocalTime,
        val desde: LocalDate,
        val hasta: LocalDate,
    ) : Cuando

    data class Puntual(val fecha: LocalDate, val inicio: LocalTime, val fin: LocalTime) : Cuando
}

/** Lo que el formulario entrega para guardar. [id] nulo = nueva. */
data class ActividadAGuardar(
    val id: Long?,
    val titulo: String,
    val tipo: ActivityKind,
    val cuando: Cuando,
    /** Nombre del lugar; vacío = sin lugar. Si coincide con uno guardado, se reutiliza. */
    val lugar: String,
    val salon: String,
    val modo: TransportMode,
    val margen: Duration,
    val aviso: Duration,
)

/** Todo lo que hace falta para construir el día: series, puntuales, excepciones, semestres y traslados. */
data class Agenda(
    val actividades: Map<Long, Actividad>,
    val lugares: Map<Long, Lugar>,
    val reglas: List<WeeklyRule>,
    val puntuales: List<Occurrence>,
    val semestres: List<Semester> = emptyList(),
    val excepciones: List<SessionException> = emptyList(),
    /** Minutos de traslado entre lugares (desde, hasta). */
    val traslados: Map<Pair<Long, Long>, Long> = emptyMap(),
) {
    val vacia: Boolean get() = actividades.isEmpty()

    /** El semestre que contiene [fecha], si lo hay. */
    fun semestreEn(fecha: LocalDate): Semester? = semestres.firstOrNull { !fecha.isBefore(it.start) && !fecha.isAfter(it.end) }

    fun minutosEntre(desde: Long, hasta: Long): Long? = traslados[desde to hasta]

    companion object {
        val VACIA = Agenda(emptyMap(), emptyMap(), emptyList(), emptyList())
    }
}

/**
 * Fuente de la agenda. Hoy la implementa un repositorio en memoria con datos de ejemplo;
 * Room (cifrado) y la ubicación + Routes API la sustituyen en pasos posteriores.
 */
interface AgendaRepository {
    fun agenda(): Flow<Agenda>

    /** Última estimación de trayecto hasta [lugarId] en [modo], o null si no se sabe. */
    suspend fun estimacion(lugarId: Long, modo: TransportMode): TravelEstimate?

    /** Las estimaciones conocidas de un lugar, por modo. */
    suspend fun estimaciones(lugarId: Long): Map<TransportMode, TravelEstimate> =
        TransportMode.entries.mapNotNull { m -> estimacion(lugarId, m)?.let { m to it } }.toMap()

    /** Crea o sustituye una actividad con su serie o su fecha. Devuelve su id. */
    suspend fun guardar(actividad: ActividadAGuardar): Long

    /** Borra una actividad y todas sus sesiones. */
    suspend fun eliminar(id: Long)

    /** Cancela o mueve una sola sesión de una serie. */
    suspend fun guardarExcepcion(excepcion: SessionException)

    suspend fun quitarExcepcion(actividadId: Long, fecha: LocalDate)

    /** Semestres guardados, con su nombre y el motivo de cada día sin clase (para Ajustes). */
    fun semestres(): Flow<List<SemestreGuardado>>

    /** Crea ([id] nulo) o sustituye un semestre con sus días sin clase (fecha → motivo). Devuelve su id. */
    suspend fun guardarSemestre(
        nombre: String,
        inicio: LocalDate,
        fin: LocalDate,
        diasSinClase: Map<LocalDate, String>,
        id: Long? = null,
    ): Long

    suspend fun eliminarSemestre(id: Long)

    /** Última duración conocida hasta un lugar (la escribirá la capa de rutas). */
    suspend fun guardarEstimacion(lugarId: Long, estimacion: TravelEstimate)

    suspend fun guardarTraslado(desde: Long, hasta: Long, minutos: Long)
}

data class SemestreGuardado(
    val id: Long,
    val nombre: String,
    val inicio: LocalDate,
    val fin: LocalDate,
    val diasSinClase: Map<LocalDate, String>,
)

/** Festivos de Colombia entre dos fechas, para proponerlos como días sin clase de un semestre. */
fun festivosPropuestos(inicio: LocalDate, fin: LocalDate): Map<LocalDate, String> =
    (inicio.year..fin.year).flatMap { ColombianHolidays.of(it) }
        .filter { !it.date.isBefore(inicio) && !it.date.isAfter(fin) }
        .associate { it.date to it.name }

/**
 * Tiempo hasta [sesion]: si se llega desde otro lugar del mismo día y hay traslado guardado entre
 * los dos, ese (Campus → Tienda); si no, el tiempo desde casa. Null si no se sabe o no hay lugar.
 */
suspend fun AgendaRepository.trayectoHacia(
    agenda: Agenda,
    dia: List<Occurrence>,
    sesion: Occurrence,
    modo: TransportMode,
    ahora: Instant,
): TravelEstimate? {
    val destino = sesion.placeId ?: return null
    DayPlanner.origin(dia, sesion)?.let { origen ->
        agenda.minutosEntre(origen, destino)?.let { return TravelEstimate(Duration.ofMinutes(it), modo, ahora, fromCache = false, manual = true) }
    }
    return estimacion(destino, modo)
}

/** El lugar guardado con ese nombre (sin distinguir mayúsculas ni espacios de más), si existe. */
fun Agenda.lugarLlamado(nombre: String): Lugar? {
    val buscado = nombre.trim().lowercase()
    if (buscado.isEmpty()) return null
    return lugares.values.firstOrNull { it.nombre.trim().lowercase() == buscado }
}
