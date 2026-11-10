package com.dpinta.agenda.reloj

import com.dpinta.agenda.domain.PersonalRhythm
import com.dpinta.agenda.domain.WatchRhythm
import kotlinx.coroutines.flow.first
import com.dpinta.agenda.avisos.AccionesAsistencia
import com.dpinta.agenda.domain.SessionException
import com.dpinta.agenda.domain.AttendanceMark
import com.dpinta.agenda.domain.Attendance
import com.dpinta.agenda.domain.WatchAttendance
import android.content.Context
import android.content.Intent
import com.dpinta.agenda.avisos.AccionAviso
import com.dpinta.agenda.avisos.AccionAvisoReceiver
import com.dpinta.agenda.avisos.ProgramadorAvisos
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.trayectoHacia
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.DayPlanner
import com.dpinta.agenda.domain.DepartureCalculator
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.domain.WatchDay
import com.dpinta.agenda.domain.WatchSession
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Rutas de la capa de datos de Wear OS que comparten el teléfono y CampusWatch. */
object RutasReloj {
    const val DIA = "/campuswatch/dia"
    const val ACCION = "/campuswatch/accion"
    /** Modo oscuro del teléfono, para «Como el teléfono» en el reloj (anexo 2026-11-09-campuswatch-claro, W2). */
    const val TEMA = "/campuswatch/tema"
    const val CLAVE_OSCURO = "oscuro"
    const val CLAVE_DIA = "dia"
}

/** De la agenda a lo que ve el reloj (03-campuswatch.md): hoy y mañana, con la hora de salida si hay trayecto. Puro. */
object MapeadorReloj {
    fun dia(
        agenda: Agenda,
        sesiones: List<Occurrence>,
        ahora: LocalDateTime,
        zona: ZoneId,
        trayecto: (Occurrence) -> TravelEstimate?,
        asistencia: List<WatchAttendance> = emptyList(),
        ritmo: List<WatchRhythm> = emptyList(),
    ): WatchDay {
        val validas = sesiones.filter { it.activityId in agenda.actividades }.sortedBy { it.start }
        return WatchDay(ahora, attendance = asistencia, rhythm = ritmo, sessions = validas.map { s ->
            val act = agenda.actividades.getValue(s.activityId)
            val necesita = DayPlanner.needsTravel(DayPlanner.previous(validas, s), s)
            val salida = if (necesita) {
                trayecto(s)?.let { DepartureCalculator.plan(s.start.atZone(zona).toInstant(), act.margen, it, ahora.atZone(zona).toInstant()).leaveAt }
            } else {
                null
            }
            WatchSession(
                activityId = act.id,
                start = s.start,
                end = s.end,
                title = act.titulo,
                room = act.salon,
                place = act.lugarId?.let { agenda.lugares[it]?.nombre }.orEmpty(),
                leaveAt = salida?.atZone(zona)?.toLocalDateTime(),
                kind = when (act.tipo) {
                    ActivityKind.CLASE, ActivityKind.EXAMEN -> WatchSession.Kind.CLASE
                    ActivityKind.TRABAJO -> WatchSession.Kind.TRABAJO
                    ActivityKind.PUNTUAL -> WatchSession.Kind.ENTREGA
                    ActivityKind.OTRO -> WatchSession.Kind.PERSONAL
                },
                exam = act.tipo == ActivityKind.EXAMEN,
            )
        })
    }
}

/** Deja el día en la capa de datos de Wear OS; el reloj lo guarda y funciona con datos locales sin el teléfono. */
@Singleton
class PublicadorReloj @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
    private val repositorio: AgendaRepository,
    private val reloj: Clock,
) {
    /**
     * Asistencia por materia para el reloj (W6), con [Attendance.summary] desde el inicio del semestre (o 120 días) hasta
     * hoy. «Hoy no voy» cancela la sesión; en una clase con marca «falté» esa sesión sí cuenta como sesión que hubo.
     */
    private suspend fun asistencia(agenda: Agenda, sesiones: (Agenda, LocalDate, LocalDate) -> List<Occurrence>, ahora: LocalDateTime): List<WatchAttendance> {
        val hoy = ahora.toLocalDate()
        val marcas = repositorio.asistencias().first()
        val faltas = marcas.filter { it.mark == AttendanceMark.FALTE }.map { it.activityId to it.date }.toSet()
        val sinMisFaltas = agenda.copy(excepciones = agenda.excepciones.filterNot { it is SessionException.Cancelled && (it.activityId to it.date) in faltas })
        val desde = agenda.semestreEn(hoy)?.start ?: hoy.minusDays(120)
        val hubo = sesiones(sinMisFaltas, desde, hoy)
        return agenda.actividades.values.filter { it.tipo == ActivityKind.CLASE || it.tipo == ActivityKind.EXAMEN }.mapNotNull { act ->
            val propias = hubo.filter { it.activityId == act.id }
            if (propias.none { !it.start.isAfter(ahora) }) return@mapNotNull null
            val r = Attendance.summary(act.id, propias, marcas, hoy, null)
            val pendiente = propias.firstOrNull { it.start.toLocalDate() == hoy && !it.end.isAfter(ahora) &&
                marcas.none { m -> m.activityId == act.id && m.date == hoy } }
            WatchAttendance(act.id, act.titulo, r.attended, r.absences, r.maxAbsences, pendiente?.start)
        }.sortedBy { it.title }
    }

    suspend fun publicar(agenda: Agenda, sesiones: (Agenda, LocalDate, LocalDate) -> List<Occurrence>) {
        val ahora = LocalDateTime.now(reloj)
        val lista = sesiones(agenda, ahora.toLocalDate(), ahora.toLocalDate().plusDays(1))
        val trayectos = HashMap<Occurrence, TravelEstimate?>()
        for (s in lista) {
            val modo = agenda.actividades[s.activityId]?.modo ?: continue
            trayectos[s] = repositorio.trayectoHacia(agenda, lista, s, modo, reloj.instant())
        }
        // «Tu ritmo» (anexo 2026-11-10-agenda-ia): consejo por sesión con hora de salida, aprendido de las salidas reales.
        val historial = repositorio.salidasReales().first()
        val ritmo = lista.filter { trayectos[it] != null }.mapNotNull { s ->
            val act = agenda.actividades[s.activityId] ?: return@mapNotNull null
            PersonalRhythm.advice(historial, act.lugarId, s.start.dayOfWeek)?.let { WatchRhythm(s.activityId, s.start, it.lateMinutes, it.samples) }
        }
        val dia = MapeadorReloj.dia(agenda, lista, ahora, reloj.zone, { trayectos[it] }, asistencia(agenda, sesiones, ahora), ritmo)
        // Sin reloj emparejado o sin Play Services la app sigue igual: el reloj es un acompañante.
        runCatching {
            val pedido = PutDataMapRequest.create(RutasReloj.DIA).apply { dataMap.putString(RutasReloj.CLAVE_DIA, dia.encode()) }
            Wearable.getDataClient(contexto).putDataItem(pedido.asPutDataRequest().setUrgent()).await()
            // Mientras la app no tenga su propio selector de tema, «Como el teléfono» sigue el modo oscuro del sistema.
            val oscuro = (contexto.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            val tema = PutDataMapRequest.create(RutasReloj.TEMA).apply { dataMap.putBoolean(RutasReloj.CLAVE_OSCURO, oscuro) }
            Wearable.getDataClient(contexto).putDataItem(tema.asPutDataRequest().setUrgent()).await()
        }
    }
}

/**
 * Acciones tocadas en el reloj (Ya voy · Aplazar 5 min · Cancelar sesión de hoy): se ejecutan con el mismo
 * receptor que los botones de la notificación, así teléfono y reloj nunca hacen cosas distintas.
 * Mensaje: «accion|idActividad|inicioISO».
 */
class ServicioReloj : WearableListenerService() {
    override fun onMessageReceived(evento: MessageEvent) {
        if (evento.path != RutasReloj.ACCION) return
        val partes = String(evento.data, Charsets.UTF_8).split('|')
        if (partes.size != 3) return
        // Asistencia («¿Fuiste?» en el reloj): va al mismo receptor, con su propia acción.
        if (partes[0] == "asisti" || partes[0] == "falte") {
            sendBroadcast(
                Intent(this, AccionAvisoReceiver::class.java)
                    .setAction(if (partes[0] == "asisti") AccionesAsistencia.ASISTI else AccionesAsistencia.FALTE)
                    .putExtra(ProgramadorAvisos.EXTRA_ACTIVIDAD, partes[1].toLongOrNull() ?: return)
                    .putExtra(ProgramadorAvisos.EXTRA_INICIO, partes[2]),
            )
            return
        }
        val accion = when (partes[0]) {
            "ya_voy" -> AccionAviso.VoySaliendo
            "aplazar" -> AccionAviso.MasCinco
            "cancelar" -> AccionAviso.HoyNoVoy
            else -> return
        }
        val id = partes[1].toLongOrNull() ?: return
        val inicio = runCatching { LocalDateTime.parse(partes[2]) }.getOrNull() ?: return
        sendBroadcast(
            Intent(this, AccionAvisoReceiver::class.java)
                .setAction(accion.accion)
                .putExtra(ProgramadorAvisos.EXTRA_ACTIVIDAD, id)
                .putExtra(ProgramadorAvisos.EXTRA_INICIO, inicio.toString()),
        )
    }
}
