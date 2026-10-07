package com.dpinta.agenda.avisos

import com.dpinta.agenda.widget.WidgetSiguiente
import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dpinta.agenda.MainActivity
import com.dpinta.agenda.R
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.SesionesEnCurso
import com.dpinta.agenda.data.ajustes.AjustesAvisos
import com.dpinta.agenda.data.agenda.trayectoHacia
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.MorningBriefing
import com.dpinta.agenda.domain.AlarmKind
import com.dpinta.agenda.domain.DepartureCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Corre [trabajo] fuera del hilo principal sin que el sistema mate el receptor antes de terminar. */
internal fun BroadcastReceiver.enSegundoPlano(trabajo: suspend () -> Unit) {
    val pendiente = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            trabajo()
        } finally {
            pendiente.finish()
        }
    }
}

/** Llega a la hora de cada alarma y publica el aviso; el precálculo solo vuelve a planear. */
@AndroidEntryPoint
class AlarmaReceiver : BroadcastReceiver() {

    @Inject lateinit var repositorio: AgendaRepository
    @Inject lateinit var programador: ProgramadorAvisos
    @Inject lateinit var sesiones: SesionesEnCurso
    @Inject lateinit var ajustes: AjustesAvisos
    @Inject lateinit var reloj: Clock

    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action == ProgramadorAvisos.ACCION_WIDGET) {
            // Empezó una sesión: reprogramar redibuja el widget y fija el próximo cambio.
            enSegundoPlano { programador.reprogramar() }
            return
        }
        if (intent.action == ProgramadorAvisos.ACCION_RESUMEN) {
            enSegundoPlano {
                publicarResumen(contexto)
                programador.reprogramar()
            }
            return
        }
        if (intent.action != ProgramadorAvisos.ACCION) return
        // Cada alarma es un cambio de estado (prepárate, sal ya, vas tarde): el widget se redibuja con él.
        WidgetSiguiente.actualizar(contexto)
        val tipo = intent.getStringExtra(ProgramadorAvisos.EXTRA_TIPO)?.let(AlarmKind::valueOf) ?: return
        val id = intent.getLongExtra(ProgramadorAvisos.EXTRA_ACTIVIDAD, -1)
        val inicio = intent.getStringExtra(ProgramadorAvisos.EXTRA_INICIO)?.let(LocalDateTime::parse) ?: return
        enSegundoPlano {
            when (tipo) {
                // Sin Routes API todavía: se vuelve a planear con la última estimación guardada.
                AlarmKind.PRECALCULO -> programador.reprogramar()
                else -> publicar(contexto, tipo, id, inicio)
            }
        }
    }

    @SuppressLint("MissingPermission") // puedeAvisar() lo comprueba antes
    private suspend fun publicar(contexto: Context, tipo: AlarmKind, id: Long, inicio: LocalDateTime) {
        if (!puedeAvisar(contexto)) return
        val agenda = repositorio.agenda().first()
        val act = agenda.actividades[id] ?: return
        val zona = reloj.zone
        val empiezaEn = inicio.atZone(zona).toInstant()
        // Una alarma atrasada (hora cambiada, teléfono dormido) no avisa de algo que ya empezó.
        if (!reloj.instant().isBefore(empiezaEn)) return
        val es24h = DateFormat.is24HourFormat(contexto)
        val deSalida = tipo != AlarmKind.AVISO
        // Si ya tocó «Voy saliendo», no se le vuelve a apurar.
        if (deSalida && sesiones.salida.value?.first == SesionesEnCurso.Sesion(id, inicio)) return
        val (canal, texto) = if (deSalida) {
            val dia = programador.sesiones(agenda, inicio.toLocalDate(), inicio.toLocalDate())
            val sesion = dia.firstOrNull { it.activityId == id && it.start == inicio } ?: return
            val estimacion = repositorio.trayectoHacia(agenda, dia, sesion, act.modo, reloj.instant()) ?: return
            val plan = DepartureCalculator.plan(empiezaEn, act.margen, estimacion, reloj.instant())
            val empieza = inicio.toLocalTime()
            when (tipo) {
                AlarmKind.SALIDA -> Canal.Salida to TextosAviso.salida(
                    plan.leaveAt.atZone(zona).toLocalTime(), act.titulo, act.salon, estimacion, act.margen, es24h,
                )
                AlarmKind.SAL_YA -> Canal.SalYa to TextosAviso.salYa(empieza, act.titulo, act.salon, estimacion, act.margen, es24h)
                else -> Canal.SalYa to TextosAviso.vasTarde(
                    minutosTarde = Duration.between(plan.leaveAt, reloj.instant()).toMinutes(),
                    llegas = plan.arriveIfLeavingNow.atZone(zona).toLocalTime(),
                    empieza = empieza,
                    actividad = act.titulo,
                    salon = act.salon,
                    es24h = es24h,
                )
            }
        } else {
            Canal.Empieza to TextosAviso.empieza(act.titulo, act.salon)
        }
        val aviso = base(contexto, canal, texto)
            .setWhen(empiezaEn.toEpochMilli())
            .setShowWhen(true)
            .setCategory(if (deSalida) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
        if (deSalida) {
            val acciones = if (tipo == AlarmKind.SALIDA) AccionAviso.entries else listOf(AccionAviso.VoySaliendo)
            AccionAviso.anadir(contexto, aviso, id, inicio, acciones)
        }
        // Salida, Sal ya y Vas tarde comparten número: cada uno sustituye al anterior.
        val numero = ProgramadorAvisos.codigo(if (deSalida) AlarmKind.SALIDA else tipo, id, inicio)
        NotificationManagerCompat.from(contexto).notify(numero, aviso.build())
    }

    /** «Hoy: 3 cosas · primera salida 7:32» (canal Resumen). Un día sin nada no avisa. */
    @SuppressLint("MissingPermission") // puedeAvisar() lo comprueba antes
    private suspend fun publicarResumen(contexto: Context) {
        val agenda = repositorio.agenda().first()
        val zona = reloj.zone
        val hoy = LocalDate.now(reloj)
        val ajuste = ajustes.resumen.value
        if (!ajuste.activo) return
        val previsto = hoy.atTime(ajuste.hora).atZone(zona).toInstant()
        if (Duration.between(previsto, reloj.instant()) > ProgramadorAvisos.RETRASO_MAXIMO_RESUMEN) return
        programador.resumenEntregado(hoy)
        if (!puedeAvisar(contexto)) return
        val deHoy = programador.sesiones(agenda, hoy, hoy)
        if (deHoy.isEmpty()) return
        val estimaciones = deHoy.associateWith { s ->
            repositorio.trayectoHacia(agenda, deHoy, s, agenda.actividades.getValue(s.activityId).modo, reloj.instant())
        }
        val resumen = MorningBriefing.of(
            date = hoy,
            occurrences = deHoy,
            zone = zona,
            now = reloj.instant(),
            marginOf = { agenda.actividades.getValue(it).margen },
            travelOf = { estimaciones[it] },
            isWork = { agenda.actividades.getValue(it).tipo == ActivityKind.TRABAJO },
            tasks = emptyList(),
        )
        val primera = resumen.firstDeparture?.atZone(zona)?.toLocalTime()
        val texto = TextosAviso.resumen(resumen.sessions.size, primera, DateFormat.is24HourFormat(contexto))
        val aviso = base(contexto, Canal.Resumen, texto).addAction(0, "Ver día", abrirApp(contexto))
        NotificationManagerCompat.from(contexto).notify(ProgramadorAvisos.CODIGO_RESUMEN, aviso.build())
    }

    private fun puedeAvisar(contexto: Context) =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun abrirApp(contexto: Context): PendingIntent = PendingIntent.getActivity(
        contexto,
        0,
        Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun base(contexto: Context, canal: Canal, texto: TextoAviso) =
        NotificationCompat.Builder(contexto, canal.id)
            .setSmallIcon(R.drawable.ic_aviso)
            .setContentTitle(texto.titulo)
            .setContentText(texto.texto)
            .setContentIntent(abrirApp(contexto))
            .setAutoCancel(true)
}

/** Reinicio, cambio de hora o de zona, o app actualizada: las alarmas exactas se pierden y hay que ponerlas otra vez. */
@AndroidEntryPoint
class ReprogramarReceiver : BroadcastReceiver() {

    @Inject lateinit var programador: ProgramadorAvisos

    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action !in ACCIONES) return
        enSegundoPlano { programador.reprogramar() }
    }

    private companion object {
        val ACCIONES = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
