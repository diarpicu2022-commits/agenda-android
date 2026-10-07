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
 * Las líneas de cifras del widget se dibujan aquí como imagen, con la familia «display» del sistema (Bricolage
 * Grotesque) y el peso exacto: por RemoteViews el lanzador no respeta los ejes variables. TalkBack lee la frase
 * completa del contenedor, no las imágenes. Enmienda 2026-10-07: antes Archivo 62.
 */
class WidgetDibujo(private val contexto: Context) {

    private val bricolage = ResourcesCompat.getFont(contexto, R.font.bricolage_grotesque_variable)
    private val figtree = ResourcesCompat.getFont(contexto, R.font.figtree_variable)
    private val tinta = contexto.getColor(R.color.widget_tinta)
    private val tintaSuave = contexto.getColor(R.color.widget_tinta_suave)
    private val hora = contexto.getColor(R.color.widget_hora)
    private val metricas = contexto.resources.displayMetrics

    private fun pincel(sp: Float, peso: Int, espaciado: Float = 0f, cifras: Boolean = true, color0: Int = tinta) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (cifras) bricolage else figtree
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metricas)
        color = color0
        fontVariationSettings = "'wght' $peso"
        letterSpacing = espaciado
        fontFeatureSettings = "tnum"
    }

    /** `etiqueta`: 12 sp, Figtree 700, +0,06 em, en tinta-suave («SAL A LAS», «EMPIEZA»). */
    fun rotulo(texto: String, anchoMax: Int) = dibujar(listOf(texto to pincel(12f, 700, 0.06f, cifras = false, color0 = tintaSuave)), anchoMax)

    /** La hora con las métricas de `salon` (40 sp, 800) y, en 12 h, el `sufijo` (24 sp, 700) en la misma línea base. */
    fun hora(cifra: String, sufijo: String?, anchoMax: Int): Bitmap {
        // `hora-xl` 36/650 en el acento `hora` (la hora de salida es lo más grande del widget).
        val partes = mutableListOf(cifra to pincel(36f, 650, -0.02f, color0 = hora))
        if (sufijo != null) partes += " $sufijo" to pincel(16f, 600, color0 = hora)
        return dibujar(partes, anchoMax)
    }

    /** Salón: Bricolage 600, 18 sp, en tinta. */
    fun salon(texto: String, anchoMax: Int) = dibujar(listOf(texto to pincel(18f, 600)), anchoMax)

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
