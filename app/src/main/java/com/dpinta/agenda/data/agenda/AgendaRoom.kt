package com.dpinta.agenda.data.agenda

import com.dpinta.agenda.data.db.ActividadEntidad
import com.dpinta.agenda.data.db.AgendaDao
import com.dpinta.agenda.data.db.ExcepcionEntidad
import com.dpinta.agenda.data.db.SemestreEntidad
import com.dpinta.agenda.data.db.SerieEntidad
import com.dpinta.agenda.data.db.SesionPuntualEntidad
import com.dpinta.agenda.data.db.TrasladoEntidad
import com.dpinta.agenda.data.db.TrayectoEntidad
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.Semester
import com.dpinta.agenda.domain.SessionException
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.domain.WeeklyRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** La agenda en Room + SQLCipher. Traduce filas a los tipos del dominio y al revés. */
@Singleton
class AgendaRoom @Inject constructor(private val dao: AgendaDao, private val reloj: Clock) : AgendaRepository {

    private val estructura = combine(dao.actividades(), dao.lugares(), dao.series(), dao.puntuales()) { a, l, s, p ->
        Base(a, l, s, p)
    }
    private val calendario = combine(dao.excepciones(), dao.semestres(), dao.diasSinClase(), dao.traslados()) { e, sem, d, t ->
        Calendario(e, sem, d, t)
    }

    override fun agenda(): Flow<Agenda> = combine(estructura, calendario) { b, c ->
        val lugarDe = b.actividades.associate { it.id to it.lugarId }
        Agenda(
            actividades = b.actividades.associate {
                it.id to Actividad(it.id, it.titulo, it.tipo, it.salon, it.lugarId, Duration.ofMinutes(it.margenMin.toLong()), it.modo, Duration.ofMinutes(it.avisoMin.toLong()))
            },
            lugares = b.lugares.associate { it.id to Lugar(it.id, it.nombre) },
            reglas = b.series.map { WeeklyRule(it.actividadId, it.dias, it.inicio, it.fin, it.desde, it.hasta) },
            puntuales = b.puntuales.map { Occurrence(it.actividadId, lugarDe[it.actividadId], it.inicio, it.fin) },
            semestres = c.semestres.map { s -> Semester(s.inicio, s.fin, c.dias.filter { it.semestreId == s.id }.map { it.fecha }.toSet()) },
            excepciones = c.excepciones.mapNotNull { e ->
                when (e.tipo) {
                    ExcepcionEntidad.CANCELADA -> SessionException.Cancelled(e.actividadId, e.fecha)
                    ExcepcionEntidad.MOVIDA -> if (e.nuevoInicio != null && e.nuevoFin != null) SessionException.Moved(e.actividadId, e.fecha, e.nuevoInicio, e.nuevoFin) else null
                    else -> null
                }
            },
            traslados = c.traslados.associate { (it.desdeId to it.hastaId) to it.minutos.toLong() },
        )
    }

    override suspend fun estimacion(lugarId: Long, modo: TransportMode): TravelEstimate? =
        dao.trayecto(lugarId, modo)?.let { TravelEstimate(Duration.ofMinutes(it.minutos.toLong()), modo, it.calculadoEn, fromCache = false, manual = it.manual) }

    override suspend fun guardar(actividad: ActividadAGuardar): Long {
        val fila = ActividadEntidad(
            id = actividad.id ?: 0,
            titulo = actividad.titulo.trim(),
            tipo = actividad.tipo,
            salon = actividad.salon.trim(),
            lugarId = null,
            margenMin = actividad.margen.toMinutes().toInt(),
            modo = actividad.modo,
            avisoMin = actividad.aviso.toMinutes().toInt(),
        )
        val (serie, puntual) = when (val c = actividad.cuando) {
            is Cuando.Semanal -> SerieEntidad(actividadId = 0, dias = c.dias, inicio = c.inicio, fin = c.fin, desde = c.desde, hasta = c.hasta, zona = reloj.zone.id) to null
            is Cuando.Puntual -> null to SesionPuntualEntidad(actividadId = 0, inicio = c.fecha.atTime(c.inicio), fin = c.fecha.atTime(c.fin))
        }
        return dao.guardarActividad(fila, actividad.lugar, serie, puntual)
    }

    override suspend fun eliminar(id: Long) {
        dao.eliminarActividad(id)
    }

    override suspend fun guardarExcepcion(excepcion: SessionException) {
        dao.guardarExcepcion(
            when (excepcion) {
                is SessionException.Cancelled -> ExcepcionEntidad(actividadId = excepcion.activityId, fecha = excepcion.date, tipo = ExcepcionEntidad.CANCELADA)
                is SessionException.Moved -> ExcepcionEntidad(
                    actividadId = excepcion.activityId,
                    fecha = excepcion.date,
                    tipo = ExcepcionEntidad.MOVIDA,
                    nuevoInicio = excepcion.newStart,
                    nuevoFin = excepcion.newEnd,
                )
            },
        )
    }

    override suspend fun quitarExcepcion(actividadId: Long, fecha: LocalDate) {
        dao.quitarExcepcion(actividadId, fecha)
    }

    override fun semestres(): Flow<List<SemestreGuardado>> =
        combine(dao.semestres(), dao.diasSinClase()) { semestres, dias ->
            semestres.map { s ->
                SemestreGuardado(s.id, s.nombre, s.inicio, s.fin, dias.filter { it.semestreId == s.id }.associate { it.fecha to it.motivo })
            }
        }

    override suspend fun guardarSemestre(
        nombre: String,
        inicio: LocalDate,
        fin: LocalDate,
        diasSinClase: Map<LocalDate, String>,
        id: Long?,
    ): Long = dao.guardarSemestre(SemestreEntidad(id = id ?: 0, nombre = nombre, inicio = inicio, fin = fin), diasSinClase)

    override suspend fun eliminarSemestre(id: Long) = dao.eliminarSemestre(id)

    override suspend fun guardarEstimacion(lugarId: Long, estimacion: TravelEstimate) {
        dao.guardarTrayecto(TrayectoEntidad(lugarId, estimacion.mode, estimacion.duration.toMinutes().toInt(), estimacion.computedAt, estimacion.manual))
    }

    override suspend fun guardarTraslado(desde: Long, hasta: Long, minutos: Long) {
        dao.guardarTraslado(TrasladoEntidad(desde, hasta, minutos.toInt(), reloj.instant()))
    }

    private data class Base(
        val actividades: List<ActividadEntidad>,
        val lugares: List<com.dpinta.agenda.data.db.LugarEntidad>,
        val series: List<SerieEntidad>,
        val puntuales: List<SesionPuntualEntidad>,
    )

    private data class Calendario(
        val excepciones: List<ExcepcionEntidad>,
        val semestres: List<SemestreEntidad>,
        val dias: List<com.dpinta.agenda.data.db.DiaSinClaseEntidad>,
        val traslados: List<TrasladoEntidad>,
    )
}
