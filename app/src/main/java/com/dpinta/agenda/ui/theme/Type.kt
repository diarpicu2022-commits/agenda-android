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
 * Tipografía del sistema Agenda + CampusWatch (README · Tipografía; enmienda 2026-10-07):
 * Bricolage Grotesque («display»: horas, cuentas regresivas, títulos) y Figtree («ui»: todo lo demás).
 * Empaquetadas en res/font (OFL). Nunca se usa FontFamily.Default.
 */

private fun bricolage(peso: Int) = Font(
    resId = R.font.bricolage_grotesque_variable,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

private fun figtree(peso: Int) = Font(
    resId = R.font.figtree_variable,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

/** Familia «display». */
val Bricolage = FontFamily(bricolage(600), bricolage(650), bricolage(700))

/** Familia «ui». */
val Figtree = FontFamily(figtree(400), figtree(450), figtree(600), figtree(650), figtree(700))

// Nombres anteriores: apuntan a las familias nuevas mientras cada pantalla migra.
val Archivo62 = Bricolage
val Atkinson = Figtree

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

/** Escala del sistema (tokens.json · type). Ningún texto por debajo de 12 sp; el que se lee, 14 sp o más. */
@Immutable
data class AgendaType(
    /** hora-hero 56/56 · 700 · −0,03 em: la cuenta de salida y la hora de la tarjeta Ahora. */
    val horaHero: TextStyle = estilo(Bricolage, 700, 56, 56, tracking = -0.03f),
    /** hora-xl 36/40 · 650 · −0,02 em: hora de salida, total de horas. */
    val horaXl: TextStyle = estilo(Bricolage, 650, 36, 40, tracking = -0.02f),
    /** hora-lg 22/26 · 600 · −0,01 em: marcas de hora en la línea del día. */
    val horaLg: TextStyle = estilo(Bricolage, 600, 22, 26, tracking = -0.01f),
    /** hora-sm 14/18 · 600: rangos dentro de ítems y chips. */
    val horaSm: TextStyle = estilo(Bricolage, 600, 14, 18),
    /** titulo 28/32 · 700 · −0,02 em: título de pantalla y saludo. */
    val titulo: TextStyle = estilo(Figtree, 700, 28, 32, tracking = -0.02f),
    /** encabezado 19/24 · 650: nombre de la actividad actual, encabezado de tarjeta. */
    val encabezado: TextStyle = estilo(Figtree, 650, 19, 24),
    /** cuerpo-fuerte 16/22 · 600: nombre de actividad en la línea del día. */
    val cuerpoFuerte: TextStyle = estilo(Figtree, 600, 16, 22),
    /** cuerpo 16/24 · 400. */
    val cuerpo: TextStyle = estilo(Figtree, 400, 16, 24),
    /** apoyo 14/20 · 450: lugar, metadatos, ayuda de campo. */
    val apoyo: TextStyle = estilo(Figtree, 450, 14, 20),
    /** etiqueta 12/16 · 700 · +0,06 em, en mayúsculas: AHORA, PRÓXIMA, PARCIAL (máximo dos palabras). */
    val etiqueta: TextStyle = estilo(Figtree, 700, 12, 16, tracking = 0.06f),
) {
    // Roles del contrato anterior → roles del sistema (se retiran al migrar cada pantalla).
    val horaSalida: TextStyle get() = horaHero
    val salon: TextStyle get() = horaXl
    val rotulo: TextStyle get() = etiqueta
    val sufijo: TextStyle get() = apoyo
    val actividad: TextStyle get() = encabezado
    val seccion: TextStyle get() = titulo
    val filaHora: TextStyle get() = horaLg
    val celda: TextStyle get() = horaSm
    val meta: TextStyle get() = apoyo
    val boton: TextStyle get() = cuerpoFuerte
}

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
