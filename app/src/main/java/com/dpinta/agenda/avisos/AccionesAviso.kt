package com.dpinta.agenda.avisos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.SesionesEnCurso
import com.dpinta.agenda.domain.AlarmKind
import com.dpinta.agenda.domain.SessionException
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

/** Botones del canal Salida, en el orden del anexo: Voy saliendo · +5 min · Hoy no voy. */
enum class AccionAviso(val etiqueta: String) {
    VoySaliendo("Voy saliendo"),
    MasCinco("+5 min"),
    HoyNoVoy("Hoy no voy"),
    ;

    val accion: String get() = "com.dpinta.agenda.aviso.$name"

    companion object {
        fun anadir(contexto: Context, aviso: NotificationCompat.Builder, actividad: Long, inicio: LocalDateTime) {
            for (a in entries) {
                val intent = Intent(contexto, AccionAvisoReceiver::class.java)
                    .setAction(a.accion)
                    .putExtra(ProgramadorAvisos.EXTRA_ACTIVIDAD, actividad)
                    .putExtra(ProgramadorAvisos.EXTRA_INICIO, inicio.toString())
                val codigo = ProgramadorAvisos.codigo(AlarmKind.SALIDA, actividad, inicio) + a.ordinal + 1
                val pendiente = PendingIntent.getBroadcast(
                    contexto, codigo, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                aviso.addAction(0, a.etiqueta, pendiente)
            }
        }
    }
}

@AndroidEntryPoint
class AccionAvisoReceiver : BroadcastReceiver() {

    @Inject lateinit var repositorio: AgendaRepository
    @Inject lateinit var sesiones: SesionesEnCurso
    @Inject lateinit var reloj: Clock

    override fun onReceive(contexto: Context, intent: Intent) {
        val accion = AccionAviso.entries.firstOrNull { it.accion == intent.action } ?: return
        val id = intent.getLongExtra(ProgramadorAvisos.EXTRA_ACTIVIDAD, -1)
        val inicio = intent.getStringExtra(ProgramadorAvisos.EXTRA_INICIO)?.let(LocalDateTime::parse) ?: return
        val codigo = ProgramadorAvisos.codigo(AlarmKind.SALIDA, id, inicio)
        NotificationManagerCompat.from(contexto).cancel(codigo)
        val sesion = SesionesEnCurso.Sesion(id, inicio)
        when (accion) {
            AccionAviso.VoySaliendo -> sesiones.salida.value = sesion to reloj.instant()
            AccionAviso.MasCinco -> {
                val cuando = reloj.instant() + Duration.ofMinutes(5)
                sesiones.pospuesto.value = cuando
                repetirSalida(contexto, id, inicio, cuando.toEpochMilli())
            }
            // Cancela solo esta sesión; al cambiar la agenda se quitan sus alarmas.
            AccionAviso.HoyNoVoy -> enSegundoPlano {
                repositorio.guardarExcepcion(SessionException.Cancelled(id, inicio.toLocalDate()))
            }
        }
    }

    /** Vuelve a lanzar el aviso de salida; código propio para que reprogramar no lo pise. */
    private fun repetirSalida(contexto: Context, id: Long, inicio: LocalDateTime, ms: Long) {
        val intent = Intent(contexto, AlarmaReceiver::class.java)
            .setAction(ProgramadorAvisos.ACCION)
            .putExtra(ProgramadorAvisos.EXTRA_TIPO, AlarmKind.SALIDA.name)
            .putExtra(ProgramadorAvisos.EXTRA_ACTIVIDAD, id)
            .putExtra(ProgramadorAvisos.EXTRA_INICIO, inicio.toString())
        val pendiente = PendingIntent.getBroadcast(
            contexto,
            ProgramadorAvisos.codigo(AlarmKind.SALIDA, id, inicio) - 1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val alarmas = contexto.getSystemService(AlarmManager::class.java)
        alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pendiente)
    }
}
