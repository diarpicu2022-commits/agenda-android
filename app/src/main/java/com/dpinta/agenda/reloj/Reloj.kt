package com.dpinta.agenda.reloj

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
    ): WatchDay {
        val validas = sesiones.filter { it.activityId in agenda.actividades }.sortedBy { it.start }
        return WatchDay(ahora, validas.map { s ->
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
    suspend fun publicar(agenda: Agenda, sesiones: (Agenda, LocalDate, LocalDate) -> List<Occurrence>) {
        val ahora = LocalDateTime.now(reloj)
        val lista = sesiones(agenda, ahora.toLocalDate(), ahora.toLocalDate().plusDays(1))
        val trayectos = HashMap<Occurrence, TravelEstimate?>()
        for (s in lista) {
            val modo = agenda.actividades[s.activityId]?.modo ?: continue
            trayectos[s] = repositorio.trayectoHacia(agenda, lista, s, modo, reloj.instant())
        }
        val dia = MapeadorReloj.dia(agenda, lista, ahora, reloj.zone) { trayectos[it] }
        // Sin reloj emparejado o sin Play Services la app sigue igual: el reloj es un acompañante.
        runCatching {
            val pedido = PutDataMapRequest.create(RutasReloj.DIA).apply { dataMap.putString(RutasReloj.CLAVE_DIA, dia.encode()) }
            Wearable.getDataClient(contexto).putDataItem(pedido.asPutDataRequest().setUrgent()).await()
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
