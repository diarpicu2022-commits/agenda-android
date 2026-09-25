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
        val modo = BandaMapeador.aModoUi(estimacion.mode).etiqueta
        val trayecto = "${estimacion.duration.toMinutes()} min $modo + ${margen.toMinutes()} de margen"
        return TextoAviso("Sal a las $hora · $actividad", unir(salon, trayecto))
    }

    /** «Empieza Cálculo · B-204». */
    fun empieza(actividad: String, salon: String): TextoAviso = TextoAviso(unir("Empieza $actividad", salon), null)

    private fun unir(vararg partes: String): String = partes.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")
}
