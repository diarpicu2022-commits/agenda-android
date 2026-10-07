package com.dpinta.agenda.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Tokens semánticos de color. Contrato de diseño, cláusula C3.
 * Cada token tiene un único uso; está escrito junto a cada campo.
 * Los roles de Material 3 se derivan de estos tokens (ver [toMaterialScheme]), nunca al revés.
 */
@Immutable
data class AgendaColors(
    /** Fondo de toda superficie. */
    val papel: Color,
    /** Barra de navegación inferior y hojas inferiores (segundo neutro). */
    val papel2: Color,
    /** Texto principal, iconos activos, botón Crear. */
    val tinta: Color,
    /** Meta, horas ya pasadas, dato viejo, iconos inactivos. */
    val tinta2: Color,
    /** Filetes de separación. Nada más. */
    val filete: Color,
    /** SOLO el campo de la banda de salida (C3.1). Prohibido en cualquier otro sitio. */
    val senal: Color,
    /** Texto e iconos sobre [senal]. */
    val sobreSenal: Color,
    /** SOLO el campo de la banda en el estado «vas tarde». */
    val tarde: Color,
    /** Texto sobre [tarde]. */
    val sobreTarde: Color,
    /** Texto de conflicto o error inline, siempre con icono (C3.3). */
    val alertaTexto: Color,
    /** Marcador de tipo Clase. Solo el cuadrado de C6 (C3.2). */
    val clase: Color,
    /** Marcador de tipo Trabajo. Solo el cuadrado de C6 (C3.2). */
    val trabajo: Color,
    /** Marcador de tipo Puntual: contorno en tinta. */
    val puntual: Color,
    /** Anillo de foco (C9.4). */
    val foco: Color,
    val esOscuro: Boolean,
)

/**
 * Enmienda 2026-10-07 (Diego, 2026-10-06: «botar el UI feo y colocarle el del sistema de diseño»): los tokens vienen del
 * sistema Agenda + CampusWatch ([AgendaDs], generado). Estos nombres se mantienen como alias mientras cada pantalla
 * pasa a usar los del sistema directamente.
 */
fun AgendaDs.comoAgendaColors() = AgendaColors(
    papel = fondo,
    papel2 = superficieFuerte,
    tinta = tinta,
    tinta2 = tintaSuave,
    filete = linea,
    senal = hora,
    sobreSenal = sobreHora,
    tarde = critico,
    sobreTarde = if (esNoche) fondo else superficie,
    alertaTexto = critico,
    clase = lineaUniversidad,
    trabajo = lineaTrabajo,
    puntual = lineaEntrega,
    foco = ruta,
    esOscuro = esNoche,
)

val AgendaColoresClaro = AgendaDsClaro.comoAgendaColors()
val AgendaColoresOscuro = AgendaDsNoche.comoAgendaColors()

/**
 * Mapea los tokens a los roles de Material 3 para que los componentes M3 hereden el sistema.
 * «hora» ([AgendaColors.senal]) NO se asigna a ningún rol M3: es el acento de «ahora» y solo lo pintan los componentes
 * propios. El primario es «ruta». Sin tinte tonal: surfaceTint = papel.
 */
fun AgendaColors.toMaterialScheme(): ColorScheme {
    val base = if (esOscuro) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = foco,
        onPrimary = if (esOscuro) papel else Color.White,
        primaryContainer = papel2,
        onPrimaryContainer = tinta,
        inversePrimary = papel,
        secondary = tinta,
        onSecondary = papel,
        secondaryContainer = papel2,
        onSecondaryContainer = tinta,
        tertiary = tinta,
        onTertiary = papel,
        tertiaryContainer = papel2,
        onTertiaryContainer = tinta,
        background = papel,
        onBackground = tinta,
        surface = papel,
        onSurface = tinta,
        surfaceVariant = papel2,
        onSurfaceVariant = tinta2,
        surfaceTint = papel,
        inverseSurface = tinta,
        inverseOnSurface = papel,
        error = alertaTexto,
        onError = papel,
        errorContainer = papel2,
        onErrorContainer = alertaTexto,
        outline = tinta2,
        outlineVariant = filete,
        scrim = tinta.copy(alpha = 0.32f),
        surfaceBright = papel,
        surfaceDim = papel2,
        surfaceContainerLowest = papel,
        surfaceContainerLow = papel,
        surfaceContainer = papel2,
        surfaceContainerHigh = papel2,
        surfaceContainerHighest = papel2,
    )
}
