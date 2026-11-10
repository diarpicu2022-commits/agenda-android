package com.dpinta.agenda.avisos

import com.dpinta.agenda.ui.components.Lengua
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.hoy.BandaMapeador
import java.time.Duration
import java.time.LocalTime

/** Título y texto de una notificación. Orden de C10: tiempo → qué → dónde. */
data class TextoAviso(val titulo: String, val texto: String?)

/** Redacción literal de la tabla de canales del anexo. Puro: se prueba en JVM. */
object TextosAviso {

    /** «Sal a las 7:32 · Cálculo» / «B-204 · 23 min en bus + 7 de margen». Con dato guardado, la hora lleva «aprox.» (C10). */
    fun salida(
        salirA: LocalTime,
        actividad: String,
        salon: String,
        estimacion: TravelEstimate,
        margen: Duration,
        es24h: Boolean,
        l: Lengua = Lengua.ES,
        ritmoMinutos: Int? = null,
    ): TextoAviso {
        val hora = formatearHora(salirA, es24h, l).enLinea + if (estimacion.fromCache) l(" aprox.", " approx.") else ""
        val ritmo = ritmoMinutos?.let { l("sueles salir $it min tarde", "you usually leave $it min late") }.orEmpty()
        return TextoAviso(l("Sal a las $hora · $actividad", "Leave at $hora · $actividad"), unir(salon, trayecto(estimacion, margen, l), ritmo))
    }

    /** «Sal ya · Cálculo empieza 8:00» / «B-204 · 23 min en bus + 7 de margen». */
    fun salYa(
        empieza: LocalTime,
        actividad: String,
        salon: String,
        estimacion: TravelEstimate,
        margen: Duration,
        es24h: Boolean,
        l: Lengua = Lengua.ES,
    ): TextoAviso = TextoAviso(
        l("Sal ya · $actividad empieza ${formatearHora(empieza, es24h, l).enLinea}",
            "Leave now · $actividad starts ${formatearHora(empieza, es24h, l).enLinea}"),
        unir(salon, trayecto(estimacion, margen, l)),
    )

    /** «Vas 4 min tarde · llegas 8:04 a B-204» / «Cálculo empieza 8:00». */
    fun vasTarde(
        minutosTarde: Long,
        llegas: LocalTime,
        empieza: LocalTime,
        actividad: String,
        salon: String,
        es24h: Boolean,
        l: Lengua = Lengua.ES,
    ): TextoAviso {
        val destino = if (salon.isBlank()) "" else l(" a ${salon.trim()}", " at ${salon.trim()}")
        val min = minutosTarde.coerceAtLeast(1)
        val llegada = formatearHora(llegas, es24h, l).enLinea
        return TextoAviso(
            l("Vas $min min tarde · llegas $llegada$destino", "$min min late · you arrive $llegada$destino"),
            l("$actividad empieza ${formatearHora(empieza, es24h, l).enLinea}", "$actividad starts ${formatearHora(empieza, es24h, l).enLinea}"),
        )
    }

    /** «Hoy: 3 cosas · primera salida 7:32». Sin traslado hoy, solo la cuenta. */
    fun resumen(cosas: Int, primeraSalida: LocalTime?, es24h: Boolean, l: Lengua = Lengua.ES): TextoAviso {
        val cuenta = if (cosas == 1) l("Hoy: 1 cosa", "Today: 1 thing") else l("Hoy: $cosas cosas", "Today: $cosas things")
        val salida = primeraSalida?.let { l("primera salida ", "first departure ") + formatearHora(it, es24h, l).enLinea }.orEmpty()
        return TextoAviso(unir(cuenta, salida), null)
    }

    /** «Empieza Cálculo · B-204». */
    fun empieza(actividad: String, salon: String, l: Lengua = Lengua.ES): TextoAviso =
        TextoAviso(unir(l("Empieza $actividad", "$actividad starts"), salon), null)

    private fun trayecto(estimacion: TravelEstimate, margen: Duration, l: Lengua): String {
        val modo = BandaMapeador.aModoUi(estimacion.mode).etiqueta(l)
        return l("${estimacion.duration.toMinutes()} min $modo + ${margen.toMinutes()} de margen",
            "${estimacion.duration.toMinutes()} min $modo + ${margen.toMinutes()} min buffer")
    }

    private fun unir(vararg partes: String): String = partes.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")
}
