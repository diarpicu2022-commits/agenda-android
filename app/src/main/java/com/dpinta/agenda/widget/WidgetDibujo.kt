package com.dpinta.agenda.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.dpinta.agenda.R
import kotlin.math.ceil

/**
 * Las líneas en Archivo del widget se dibujan aquí, en la app, como imagen: a través de RemoteViews
 * el lanzador carga Archivo pero no respeta el eje de ancho 62 (medido en el emulador: «9:00» salía
 * con proporción 2,36 frente a 2,12 de Archivo 62/800). Así el widget usa exactamente la misma letra
 * que la banda (C2.1). TalkBack lee la frase completa del contenedor, no las imágenes.
 */
class WidgetDibujo(private val contexto: Context) {

    private val archivo = ResourcesCompat.getFont(contexto, R.font.archivo_variable)
    private val tinta = contexto.getColor(R.color.widget_tinta)
    private val metricas = contexto.resources.displayMetrics

    private fun pincel(sp: Float, peso: Int, espaciado: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = archivo
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metricas)
        color = tinta
        fontVariationSettings = "'wdth' 62, 'wght' $peso"
        letterSpacing = espaciado
        fontFeatureSettings = "tnum"
    }

    /** `rotulo`: 20 sp, 700, +0,04 em (C2.3). */
    fun rotulo(texto: String, anchoMax: Int) = dibujar(listOf(texto to pincel(20f, 700, 0.04f)), anchoMax)

    /** La hora con las métricas de `salon` (40 sp, 800) y, en 12 h, el `sufijo` (24 sp, 700) en la misma línea base. */
    fun hora(cifra: String, sufijo: String?, anchoMax: Int): Bitmap {
        val partes = mutableListOf(cifra to pincel(40f, 800))
        if (sufijo != null) partes += " $sufijo" to pincel(24f, 700)
        return dibujar(partes, anchoMax)
    }

    /** `fila-hora`: 18 sp, 700. */
    fun salon(texto: String, anchoMax: Int) = dibujar(listOf(texto to pincel(18f, 700)), anchoMax)

    /** Tramos seguidos sobre una misma línea base; si no cabe en [anchoMax] px, se reduce entera (nunca se corta). */
    private fun dibujar(partes: List<Pair<String, Paint>>, anchoMax: Int): Bitmap {
        val ancho = partes.sumOf { (t, p) -> p.measureText(t).toDouble() }.toFloat()
        val sube = partes.maxOf { -it.second.fontMetrics.ascent }
        val baja = partes.maxOf { it.second.fontMetrics.descent }
        val escala = if (anchoMax in 1 until ceil(ancho).toInt()) anchoMax / ancho else 1f
        val bmp = createBitmap(maxOf(1, ceil(ancho * escala).toInt()), maxOf(1, ceil((sube + baja) * escala).toInt()))
        val lienzo = Canvas(bmp)
        lienzo.scale(escala, escala)
        var x = 0f
        for ((t, p) in partes) {
            lienzo.drawText(t, x, sube, p)
            x += p.measureText(t)
        }
        return bmp
    }
}
