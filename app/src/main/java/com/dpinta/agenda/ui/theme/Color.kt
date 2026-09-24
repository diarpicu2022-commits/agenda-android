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

// Filete = tinta al 18 % (0.18 x 255 = 45.9 -> 0x2E).
private const val ALFA_FILETE = 0x2E000000

val AgendaColoresClaro = AgendaColors(
    papel = Color(0xFFF2EFE6),
    papel2 = Color(0xFFE8E4D8),
    tinta = Color(0xFF14130F),
    tinta2 = Color(0xFF5A574E),
    filete = Color(ALFA_FILETE or 0x14130F),
    senal = Color(0xFFFFB000),
    sobreSenal = Color(0xFF14130F),
    tarde = Color(0xFF8E2A17),
    sobreTarde = Color(0xFFF3EEE2),
    alertaTexto = Color(0xFF8E2A17),
    clase = Color(0xFF1F4FB8),
    trabajo = Color(0xFF1E6B45),
    puntual = Color(0xFF14130F),
    foco = Color(0xFF14130F),
    esOscuro = false,
)

val AgendaColoresOscuro = AgendaColors(
    papel = Color(0xFF0E0F0C),
    papel2 = Color(0xFF1A1B17),
    tinta = Color(0xFFF2EFE6),
    tinta2 = Color(0xFFA29E92),
    filete = Color(ALFA_FILETE or 0xF2EFE6),
    senal = Color(0xFFFFB547),
    sobreSenal = Color(0xFF0E0F0C),
    tarde = Color(0xFF8E2A17),
    sobreTarde = Color(0xFFF3EEE2),
    alertaTexto = Color(0xFFFF8A6B),
    clase = Color(0xFF8FB0FF),
    trabajo = Color(0xFF6FCB98),
    puntual = Color(0xFFF2EFE6),
    foco = Color(0xFFF2EFE6),
    esOscuro = true,
)

/**
 * Mapea los tokens a los roles de Material 3 para que los componentes M3 hereden el sistema.
 * El ámbar ([AgendaColors.senal]) NO se asigna a ningún rol M3: así ningún componente
 * estándar puede pintarlo por accidente (C3.1). Sin tinte tonal: surfaceTint = papel (C5.2).
 */
fun AgendaColors.toMaterialScheme(): ColorScheme {
    val base = if (esOscuro) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = tinta,
        onPrimary = papel,
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
