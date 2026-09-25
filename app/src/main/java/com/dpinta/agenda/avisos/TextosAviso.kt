package com.dpinta.agenda.avisos

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
    ): TextoAviso {
        val hora = formatearHora(salirA, es24h).enLinea + if (estimacion.fromCache) " aprox." else ""
        return TextoAviso("Sal a las $hora · $actividad", unir(salon, trayecto(estimacion, margen)))
    }

    /** «Sal ya · Cálculo empieza 8:00» / «B-204 · 23 min en bus + 7 de margen». */
    fun salYa(
        empieza: LocalTime,
        actividad: String,
        salon: String,
        estimacion: TravelEstimate,
        margen: Duration,
        es24h: Boolean,
    ): TextoAviso = TextoAviso(
        "Sal ya · $actividad empieza ${formatearHora(empieza, es24h).enLinea}",
        unir(salon, trayecto(estimacion, margen)),
    )

    /** «Vas 4 min tarde · llegas 8:04 a B-204» / «Cálculo empieza 8:00». */
    fun vasTarde(
        minutosTarde: Long,
        llegas: LocalTime,
        empieza: LocalTime,
        actividad: String,
        salon: String,
        es24h: Boolean,
    ): TextoAviso {
        val destino = if (salon.isBlank()) "" else " a ${salon.trim()}"
        return TextoAviso(
            "Vas ${minutosTarde.coerceAtLeast(1)} min tarde · llegas ${formatearHora(llegas, es24h).enLinea}$destino",
            "$actividad empieza ${formatearHora(empieza, es24h).enLinea}",
        )
    }

    /** «Hoy: 3 cosas · primera salida 7:32». Sin traslado hoy, solo la cuenta. */
    fun resumen(cosas: Int, primeraSalida: LocalTime?, es24h: Boolean): TextoAviso {
        val cuenta = if (cosas == 1) "Hoy: 1 cosa" else "Hoy: $cosas cosas"
        val salida = primeraSalida?.let { "primera salida ${formatearHora(it, es24h).enLinea}" }.orEmpty()
        return TextoAviso(unir(cuenta, salida), null)
    }

    /** «Empieza Cálculo · B-204». */
    fun empieza(actividad: String, salon: String): TextoAviso = TextoAviso(unir("Empieza $actividad", salon), null)

    private fun trayecto(estimacion: TravelEstimate, margen: Duration): String {
        val modo = BandaMapeador.aModoUi(estimacion.mode).etiqueta
        return "${estimacion.duration.toMinutes()} min $modo + ${margen.toMinutes()} de margen"
    }

    private fun unir(vararg partes: String): String = partes.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")
}
