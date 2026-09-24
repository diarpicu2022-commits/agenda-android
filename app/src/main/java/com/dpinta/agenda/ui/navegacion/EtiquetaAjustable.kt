package com.dpinta.agenda.ui.navegacion

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/** Mínimo efectivo de la etiqueta de pestaña (enmienda 2026-09-24): 14 sp ya escalados. */
internal const val MINIMO_EFECTIVO_SP = 14f

/** Paso de reducción: medio punto, «lo justo». */
private const val PASO_SP = 0.5f

/**
 * Tamaño (en sp del estilo, antes de la escala de fuente) para que [cabe] sea cierto, bajando
 * de [baseSp] en pasos de medio punto y sin pasar de un tamaño efectivo de [minimoEfectivoSp].
 * Si ni al mínimo cabe, devuelve el mínimo: nunca baja de él.
 */
internal fun tamanoQueCabe(
    baseSp: Float,
    escalaFuente: Float,
    minimoEfectivoSp: Float = MINIMO_EFECTIVO_SP,
    cabe: (Float) -> Boolean,
): Float {
    val minimo = minOf(baseSp, minimoEfectivoSp / escalaFuente)
    var tamano = baseSp
    while (tamano > minimo && !cabe(tamano)) tamano = maxOf(minimo, tamano - PASO_SP)
    return tamano
}

/**
 * Etiqueta de pestaña de una sola línea (enmienda 2026-09-24 a C2.3/C2.4): si no cabe, se
 * reduce SOLO ella, lo justo, hasta 14 sp efectivos; nunca parte palabras (sin guiones ni
 * corte). El resto de la barra no cambia.
 */
@Composable
internal fun EtiquetaAjustable(texto: String, estilo: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val escala = LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        val disponible = constraints.maxWidth
        val tamano = remember(texto, disponible, escala, estilo) {
            tamanoQueCabe(estilo.fontSize.value, escala) { sp ->
                medidor.measure(texto, estilo.copy(fontSize = sp.sp), softWrap = false, maxLines = 1).size.width <= disponible
            }
        }
        val factor = tamano / estilo.fontSize.value
        Text(
            texto,
            style = estilo.copy(
                fontSize = tamano.sp,
                lineHeight = (estilo.lineHeight.value * factor).sp,
                hyphens = Hyphens.None,
                textAlign = TextAlign.Center,
            ),
            color = color,
            maxLines = 1,
            softWrap = false,
            // Último recurso si ni a 14 sp efectivos cabe: se recorta con elipsis, nunca se parte.
            overflow = TextOverflow.Ellipsis,
        )
    }
}
