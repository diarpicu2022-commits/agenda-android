package com.dpinta.agenda.avisos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.trayectoHacia
import com.dpinta.agenda.domain.AlarmKind
import com.dpinta.agenda.domain.AlarmPlanner
import com.dpinta.agenda.domain.AlarmSettings
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.PlannedAlarm
import com.dpinta.agenda.domain.ScheduleExpander
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.widget.WidgetSiguiente
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deja programadas en AlarmManager exactamente las alarmas que calcula [AlarmPlanner] (arquitectura §4).
 * Se llama al cambiar la agenda, al reiniciar y al cambiar la hora o la zona: cancela las que ya no
 * tocan y vuelve a poner las demás, así que llamarlo dos veces no duplica nada.
 */
@Singleton
class ProgramadorAvisos @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
    private val repositorio: AgendaRepository,
    private val reloj: Clock,
) {
    private val alarmas = contexto.getSystemService(AlarmManager::class.java)
    private val registro = contexto.getSharedPreferences("avisos_programados", Context.MODE_PRIVATE)
    private val cerrojo = Mutex()

    /** Si es falso, los avisos pueden llegar unos minutos tarde y la UI tiene que decirlo (Flujo 3). */
    val exactas: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()

    suspend fun reprogramar() = cerrojo.withLock {
        val nuevas = planear(repositorio.agenda().first())
        val codigos = nuevas.map(::codigo).toSet()
        val anteriores = registro.getStringSet(CLAVE, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }
        for (c in anteriores) if (c !in codigos) pendiente(c, null)?.let(alarmas::cancel)
        for (a in nuevas) programar(a)
        programarResumen(vacia = nuevas.isEmpty())
        programarWidget()
        WidgetSiguiente.actualizar(contexto)
        registro.edit { putStringSet(CLAVE, codigos.map { it.toString() }.toSet()) }
    }

    /** Sesiones de actividades que existen entre [desde] y [hasta], ambos incluidos. */
    fun sesiones(agenda: Agenda, desde: LocalDate, hasta: LocalDate): List<Occurrence> = ScheduleExpander.expand(
        rules = agenda.reglas,
        oneOff = agenda.puntuales,
        placeOf = { agenda.actividades[it]?.lugarId },
        from = desde,
        to = hasta,
        semester = agenda.semestreEn(desde),
        exceptions = agenda.excepciones,
    ).filter { it.activityId in agenda.actividades }

    private suspend fun planear(agenda: Agenda): List<PlannedAlarm> {
        if (agenda.vacia) return emptyList()
        val hoy = LocalDate.now(reloj)
        val sesiones = sesiones(agenda, hoy, hoy.plusDays(DIAS))
        // AlarmPlanner no suspende: las estimaciones se leen antes.
        val estimaciones = HashMap<Occurrence, TravelEstimate?>()
        for (s in sesiones) {
            val modo = agenda.actividades.getValue(s.activityId).modo
            estimaciones[s] = repositorio.trayectoHacia(agenda, sesiones, s, modo, reloj.instant())
        }
        return AlarmPlanner.plan(
            occurrences = sesiones,
            zone = reloj.zone,
            now = reloj.instant(),
            settingsOf = { id ->
                val act = agenda.actividades.getValue(id)
                AlarmSettings(margin = act.margen, remindBefore = act.aviso.takeIf { !it.isZero })
            },
            travelOf = { estimaciones[it] },
        )
    }

    private fun programar(a: PlannedAlarm) {
        val intento = pendiente(codigo(a), a) ?: return
        poner(a.at.toEpochMilli(), intento)
    }

    private fun poner(ms: Long, intento: PendingIntent) {
        if (exactas) {
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intento)
        } else {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intento)
        }
    }

    /** Lo llama el receptor al entregar el resumen: el de hoy ya no se vuelve a programar. */
    fun resumenEntregado(fecha: LocalDate) = registro.edit { putString(CLAVE_RESUMEN, fecha.toString()) }

    /**
     * El resumen de hoy mientras no se haya entregado y no llegue más de [RETRASO_MAXIMO_RESUMEN] tarde;
     * si no, el de mañana. Reprogramar (p. ej. el precálculo de las 6:00) no puede saltarse el de hoy.
     */
    private fun programarResumen(vacia: Boolean) {
        val intent = Intent(contexto, AlarmaReceiver::class.java).setAction(ACCION_RESUMEN)
        val pendiente = PendingIntent.getBroadcast(
            contexto, CODIGO_RESUMEN, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (vacia) {
            alarmas.cancel(pendiente)
            return
        }
        val ahora = LocalDateTime.now(reloj)
        val hoy = ahora.toLocalDate()
        var cuando = hoy.atTime(HORA_RESUMEN)
        val entregadoHoy = registro.getString(CLAVE_RESUMEN, null) == hoy.toString()
        if (entregadoHoy || ahora.isAfter(cuando.plus(RETRASO_MAXIMO_RESUMEN))) {
            cuando = cuando.plusDays(1)
        } else if (cuando.isBefore(ahora)) {
            cuando = ahora
        }
        poner(cuando.atZone(reloj.zone).toInstant().toEpochMilli(), pendiente)
    }

    /**
     * El widget cambia de sesión cuando la siguiente empieza: una alarma inexacta a esa hora lo
     * redibuja (y vuelve a programar la próxima). Sin sesiones por delante no hace falta.
     */
    private suspend fun programarWidget() {
        val intent = Intent(contexto, AlarmaReceiver::class.java).setAction(ACCION_WIDGET)
        val pendiente = PendingIntent.getBroadcast(contexto, CODIGO_WIDGET, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val agenda = repositorio.agenda().first()
        val ahora = LocalDateTime.now(reloj)
        val siguiente = sesiones(agenda, ahora.toLocalDate(), ahora.toLocalDate().plusDays(DIAS)).map { it.start }.filter { it.isAfter(ahora) }.minOrNull()
        if (siguiente == null) {
            alarmas.cancel(pendiente)
            return
        }
        alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, siguiente.atZone(reloj.zone).toInstant().toEpochMilli() + 1_000, pendiente)
    }

    /** Con [a] nulo solo busca uno ya existente, para cancelarlo. */
    private fun pendiente(codigo: Int, a: PlannedAlarm?): PendingIntent? {
        val intent = Intent(contexto, AlarmaReceiver::class.java).setAction(ACCION)
        if (a != null) {
            intent.putExtra(EXTRA_TIPO, a.kind.name)
                .putExtra(EXTRA_ACTIVIDAD, a.activityId)
                .putExtra(EXTRA_INICIO, a.occurrenceStart.toString())
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (a == null) PendingIntent.FLAG_NO_CREATE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(contexto, codigo, intent, flags)
    }

    companion object {
        /** Una semana por delante: al reiniciar, al cambiar la agenda o en cada precálculo se vuelve a llenar. */
        const val DIAS = 7L
        private const val CLAVE = "codigos"
        private const val CLAVE_RESUMEN = "resumen_entregado"

        /** Un resumen que llega más tarde que esto ya no es «matutino». */
        val RETRASO_MAXIMO_RESUMEN: Duration = Duration.ofHours(2)
        const val ACCION = "com.dpinta.agenda.AVISO"
        const val ACCION_RESUMEN = "com.dpinta.agenda.RESUMEN"
        const val CODIGO_RESUMEN = 1
        const val ACCION_WIDGET = "com.dpinta.agenda.WIDGET"
        const val CODIGO_WIDGET = 2

        /** Hora del resumen matutino hasta que exista el ajuste para elegirla (arquitectura, P2.8). */
        val HORA_RESUMEN: LocalTime = LocalTime.of(6, 0)
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_ACTIVIDAD = "actividad"
        const val EXTRA_INICIO = "inicio"

        /** Clave estable por (tipo, actividad, sesión): reprogramar sustituye, no duplica. */
        fun codigo(a: PlannedAlarm): Int = codigo(a.kind, a.activityId, a.occurrenceStart)

        fun codigo(tipo: AlarmKind, actividad: Long, inicio: LocalDateTime): Int =
            "${tipo.name}|$actividad|$inicio".hashCode()
    }
}
