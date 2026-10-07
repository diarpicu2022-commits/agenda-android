package com.dpinta.agenda.wear

import android.content.Context
import com.dpinta.agenda.domain.WatchDay
import com.dpinta.agenda.domain.WatchSession
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime

/** Rutas compartidas con el teléfono (app: reloj/Reloj.kt · RutasReloj). */
object Rutas {
    const val DIA = "/campuswatch/dia"
    const val ACCION = "/campuswatch/accion"
    const val CLAVE_DIA = "dia"
}

/**
 * El día que dejó el teléfono, guardado en el reloj: CampusWatch funciona con datos locales (03-campuswatch.md,
 * «Sin conexión»). Solo horario de hoy y mañana; nada se envía a ningún servidor.
 */
object AlmacenDia {
    private const val PREFS = "campuswatch"
    private const val CLAVE = "dia"
    private val estado = MutableStateFlow<WatchDay?>(null)
    @Volatile private var cargado = false

    fun dia(contexto: Context): StateFlow<WatchDay?> {
        if (!cargado) {
            estado.value = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, null)?.let(WatchDay::decode)
            cargado = true
        }
        return estado
    }

    fun guardar(contexto: Context, texto: String) {
        val dia = WatchDay.decode(texto) ?: return
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLAVE, texto).apply()
        estado.value = dia
        cargado = true
        Superficies.actualizar(contexto)
    }

    /** Al abrir la app: si hay un día más nuevo en la capa de datos (p. ej. el reloj estuvo apagado), se toma. */
    suspend fun sincronizar(contexto: Context) {
        runCatching {
            val items = Wearable.getDataClient(contexto).dataItems.await()
            try {
                for (item in items) {
                    if (item.uri.path == Rutas.DIA) DataMapItem.fromDataItem(item).dataMap.getString(Rutas.CLAVE_DIA)?.let { guardar(contexto, it) }
                }
            } finally {
                items.release()
            }
        }
    }
}

class ReceptorDia : WearableListenerService() {
    override fun onDataChanged(eventos: DataEventBuffer) {
        for (e in eventos) {
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == Rutas.DIA) {
                DataMapItem.fromDataItem(e.dataItem).dataMap.getString(Rutas.CLAVE_DIA)?.let { AlmacenDia.guardar(this, it) }
            }
        }
    }
}

/** Acciones del reloj (02-urgencia.md · «Acciones de aviso»); el teléfono las ejecuta con su mismo receptor. */
enum class Accion(val clave: String, val etiqueta: String) {
    YaVoy("ya_voy", "Ya voy"),
    Aplazar("aplazar", "Aplazar 5 min"),
    Cancelar("cancelar", "Cancelar sesión de hoy"),
}

object Telefono {
    /** Envía la acción a los teléfonos conectados. Devuelve falso si no hay ninguno (sin conexión). */
    suspend fun enviar(contexto: Context, accion: Accion, sesion: WatchSession): Boolean = runCatching {
        val nodos = Wearable.getNodeClient(contexto).connectedNodes.await()
        val datos = "${accion.clave}|${sesion.activityId}|${sesion.start}".toByteArray(Charsets.UTF_8)
        for (n in nodos) Wearable.getMessageClient(contexto).sendMessage(n.id, Rutas.ACCION, datos).await()
        nodos.isNotEmpty()
    }.getOrDefault(false)
}

/** Pie de estado de los datos locales: «Actualizado 07:12» si es reciente; si no, «Datos locales · 07:12». */
fun pieDatos(dia: WatchDay, ahora: LocalDateTime): String {
    val hora = Formato.hora(dia.computedAt.toLocalTime())
    return if (java.time.Duration.between(dia.computedAt, ahora).toMinutes() <= 15) "Actualizado $hora" else "Datos locales · $hora"
}
