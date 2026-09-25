package com.dpinta.agenda.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dpinta.agenda.R

/*
 * Tipografía. Contrato de diseño, cláusulas C2.1 a C2.3 (C2.1 enmendada el 2026-09-23:
 * Archivo sustituye a Big Shoulders Display por no tener cifras tabulares).
 * Ambas familias van empaquetadas en res/font (OFL; licencias en docs/licencias).
 * Nunca se usa FontFamily.Default.
 */

/** Ancho fijo de Archivo en todo el sistema (C2.1). */
private const val ANCHO_ARCHIVO = 62f

private fun archivo(peso: Int) = Font(
    resId = R.font.archivo_variable,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(peso),
        FontVariation.width(ANCHO_ARCHIVO),
    ),
)

private fun atkinson(peso: Int) = Font(
    resId = R.font.atkinson_hyperlegible_next_variable,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

/** Cifras de hora, salones, rótulos, sufijo y horas de fila. Pesos 800 y 700. */
val Archivo62 = FontFamily(archivo(700), archivo(800))

/** Todo lo demás. Pesos 400, 600 y 700. */
val Atkinson = FontFamily(atkinson(400), atkinson(600), atkinson(700))

/** Cifras tabulares en todo texto (C2.2): las horas y duraciones no cambian de ancho. */
private const val TNUM = "tnum"

/** El interlineado es exactamente el del contrato: sin recorte asimétrico. */
private val interlineadoExacto = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun estilo(
    familia: FontFamily,
    peso: Int,
    tamano: Int,
    linea: Int,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = familia,
    fontWeight = FontWeight(peso),
    fontSize = tamano.sp,
    lineHeight = linea.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = TNUM,
    lineHeightStyle = interlineadoExacto,
)

/** Escala única del sistema (C2.3). Ningún texto por debajo de 14 sp. */
@Immutable
data class AgendaType(
    /** La hora de salida en la banda. Solo ahí. 96/88 Archivo 800. */
    val horaSalida: TextStyle = estilo(Archivo62, 800, 96, 88),
    /** Código de salón en banda y detalle. 40/44 Archivo 800. */
    val salon: TextStyle = estilo(Archivo62, 800, 40, 44),
    /** «SAL A LAS», «SAL YA», «VAS TARDE», «LLEGAS». 20/24 Archivo 700, mayúsculas, +0,04 em. */
    val rotulo: TextStyle = estilo(Archivo62, 700, 20, 24, tracking = 0.04f),
    /** «a. m.» / «p. m.» bajo la hora, solo en 12 h (C1.1). 24/28 Archivo 700. */
    val sufijo: TextStyle = estilo(Archivo62, 700, 24, 28),
    /** Nombre de actividad en la banda. 28/32 Atkinson 600. */
    val actividad: TextStyle = estilo(Atkinson, 600, 28, 32),
    /** Títulos de pantalla y sección. 20/26 Atkinson 700. */
    val seccion: TextStyle = estilo(Atkinson, 700, 20, 26),
    /** Rango «8:00 → 10:00» en la lista del día. 18/24 Archivo 700. */
    val filaHora: TextStyle = estilo(Archivo62, 700, 18, 24),
    /** Código de salón y horas dentro de una celda de Semana. 14/16 Archivo 700 (enmienda 2026-09-25). */
    val celda: TextStyle = estilo(Archivo62, 700, 14, 16),
    /** Texto de lista y formularios. Mínimo del texto que se lee. 16/24 Atkinson 400. */
    val cuerpo: TextStyle = estilo(Atkinson, 400, 16, 24),
    /** Modo, margen, frescura del dato, lugar secundario. 14/20 Atkinson 400. */
    val meta: TextStyle = estilo(Atkinson, 400, 14, 20),
    /** Etiquetas de botón. 16/20 Atkinson 600. */
    val boton: TextStyle = estilo(Atkinson, 600, 16, 20),
)

/**
 * Los 15 roles de Material 3 apuntan a roles del contrato, para que ningún componente M3
 * caiga en la tipografía por defecto ni en tamaños fuera de la escala.
 */
fun AgendaType.toMaterialTypography() = Typography(
    displayLarge = horaSalida,
    displayMedium = salon,
    displaySmall = salon,
    headlineLarge = actividad,
    headlineMedium = actividad,
    headlineSmall = seccion,
    titleLarge = seccion,
    titleMedium = boton,
    titleSmall = boton,
    bodyLarge = cuerpo,
    bodyMedium = cuerpo,
    bodySmall = meta,
    labelLarge = boton,
    labelMedium = meta,
    labelSmall = meta,
)
