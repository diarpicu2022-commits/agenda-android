package com.dpinta.agenda.avisos

import android.Manifest
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
import com.dpinta.agenda.domain.AlarmKind
import com.dpinta.agenda.domain.DepartureCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
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
    @Inject lateinit var reloj: Clock

    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action != ProgramadorAvisos.ACCION) return
        val tipo = intent.getStringExtra(ProgramadorAvisos.EXTRA_TIPO)?.let(AlarmKind::valueOf) ?: return
        val id = intent.getLongExtra(ProgramadorAvisos.EXTRA_ACTIVIDAD, -1)
        val inicio = intent.getStringExtra(ProgramadorAvisos.EXTRA_INICIO)?.let(LocalDateTime::parse) ?: return
        enSegundoPlano {
            when (tipo) {
                // Sin Routes API todavía: se vuelve a planear con la última estimación guardada.
                AlarmKind.PRECALCULO -> programador.reprogramar()
                AlarmKind.AVISO, AlarmKind.SALIDA -> publicar(contexto, tipo, id, inicio)
            }
        }
    }

    private suspend fun publicar(contexto: Context, tipo: AlarmKind, id: Long, inicio: LocalDateTime) {
        if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val act = repositorio.agenda().first().actividades[id] ?: return
        val zona = reloj.zone
        val empiezaEn = inicio.atZone(zona).toInstant()
        val (canal, texto) = if (tipo == AlarmKind.SALIDA) {
            val lugar = act.lugarId ?: return
            val estimacion = repositorio.estimacion(lugar, act.modo) ?: return
            val plan = DepartureCalculator.plan(empiezaEn, act.margen, estimacion, reloj.instant())
            val salirA = plan.leaveAt.atZone(zona).toLocalTime()
            Canal.Salida to TextosAviso.salida(salirA, act.titulo, act.salon, estimacion, act.margen, DateFormat.is24HourFormat(contexto))
        } else {
            Canal.Empieza to TextosAviso.empieza(act.titulo, act.salon)
        }
        val abrir = PendingIntent.getActivity(
            contexto,
            0,
            Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val aviso = NotificationCompat.Builder(contexto, canal.id)
            .setSmallIcon(R.drawable.ic_aviso)
            .setContentTitle(texto.titulo)
            .setContentText(texto.texto)
            .setWhen(empiezaEn.toEpochMilli())
            .setShowWhen(true)
            .setCategory(if (tipo == AlarmKind.SALIDA) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(abrir)
            .setAutoCancel(true)
        if (tipo == AlarmKind.SALIDA) AccionAviso.anadir(contexto, aviso, id, inicio)
        NotificationManagerCompat.from(contexto).notify(ProgramadorAvisos.codigo(tipo, id, inicio), aviso.build())
    }
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
