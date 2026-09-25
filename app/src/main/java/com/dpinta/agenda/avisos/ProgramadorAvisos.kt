package com.dpinta.agenda.avisos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.domain.AlarmKind
import com.dpinta.agenda.domain.AlarmPlanner
import com.dpinta.agenda.domain.AlarmSettings
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.PlannedAlarm
import com.dpinta.agenda.domain.ScheduleExpander
import com.dpinta.agenda.domain.TravelEstimate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
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
        registro.edit { putStringSet(CLAVE, codigos.map { it.toString() }.toSet()) }
    }

    private suspend fun planear(agenda: Agenda): List<PlannedAlarm> {
        if (agenda.vacia) return emptyList()
        val hoy = LocalDate.now(reloj)
        val sesiones = ScheduleExpander.expand(
            rules = agenda.reglas,
            oneOff = agenda.puntuales,
            placeOf = { agenda.actividades[it]?.lugarId },
            from = hoy,
            to = hoy.plusDays(DIAS),
            semester = agenda.semestreEn(hoy),
            exceptions = agenda.excepciones,
        ).filter { it.activityId in agenda.actividades }
        // AlarmPlanner no suspende: las estimaciones se leen antes.
        val estimaciones = HashMap<Occurrence, TravelEstimate?>()
        for (s in sesiones) {
            val modo = agenda.actividades.getValue(s.activityId).modo
            estimaciones[s] = s.placeId?.let { repositorio.estimacion(it, modo) }
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
        val ms = a.at.toEpochMilli()
        if (exactas) {
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intento)
        } else {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intento)
        }
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
        const val ACCION = "com.dpinta.agenda.AVISO"
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_ACTIVIDAD = "actividad"
        const val EXTRA_INICIO = "inicio"

        /** Clave estable por (tipo, actividad, sesión): reprogramar sustituye, no duplica. */
        fun codigo(a: PlannedAlarm): Int = codigo(a.kind, a.activityId, a.occurrenceStart)

        fun codigo(tipo: AlarmKind, actividad: Long, inicio: LocalDateTime): Int =
            "${tipo.name}|$actividad|$inicio".hashCode()
    }
}
