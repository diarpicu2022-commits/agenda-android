package com.dpinta.agenda.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado. Contrato de diseño, cláusula C4.1: 4, 8, 12, 16, 20, 24, 32, 48, 64.
 * Cualquier otro valor de espaciado exige justificación escrita junto al código.
 */
@Immutable
object AgendaSpacing {
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s16: Dp = 16.dp
    val s20: Dp = 20.dp
    val s24: Dp = 24.dp
    val s32: Dp = 32.dp
    val s48: Dp = 48.dp
    val s64: Dp = 64.dp

    val escala: List<Dp> = listOf(s4, s8, s12, s16, s20, s24, s32, s48, s64)
}

/**
 * Medidas de forma (no son espaciado; las fija el contrato en C5, C6 y C9, no la escala C4.1).
 */
@Immutable
object AgendaMedidas {
    /** Filete de separación (C6). */
    val filete: Dp = 1.5.dp
    /** Marcador de tipo de actividad (C6). */
    val marcadorTipo: Dp = 10.dp
    /** Punto de la línea de «ahora» (C5.4). */
    val puntoAhora: Dp = 8.dp
    /** Hilo del día en la columna de horas (C6). */
    val hilo: Dp = 2.dp
    /** Área táctil mínima (C9.2). Alto de botón (C5.3). */
    val toque: Dp = 48.dp
    /** Botón Crear cuadrado (C5.3). */
    val botonCrear: Dp = 56.dp
    /** Anillo de foco y su separación (C9.4). */
    val anilloFoco: Dp = 2.dp
    /** Subrayado del botón secundario de la banda (C5.3). */
    val subrayado: Dp = 1.5.dp
    /** Iconos en navegación y acciones, y en línea con texto meta (C7.2). */
    val icono: Dp = 24.dp
    val iconoEnLinea: Dp = 20.dp
}

/** Retícula según el ancho disponible (C4.2). */
@Immutable
data class AgendaGrid(
    val margen: Dp,
    val columnas: Int,
    val medianil: Dp,
    /** Columna fija de horas en Hoy (C4.2, C4.5). */
    val columnaHoras: Dp = AgendaSpacing.s64,
) {
    companion object {
        /** 360 dp: margen 20, 4 columnas, medianil 12. */
        val Compacta = AgendaGrid(margen = AgendaSpacing.s20, columnas = 4, medianil = AgendaSpacing.s12)
        /** 412 dp: margen 24, 4 columnas, medianil 16. */
        val Holgada = AgendaGrid(margen = AgendaSpacing.s24, columnas = 4, medianil = AgendaSpacing.s16)
        /** 600 dp o más (plegable, tablet): margen 32, 8 columnas, medianil 16. */
        val Ancha = AgendaGrid(margen = AgendaSpacing.s32, columnas = 8, medianil = AgendaSpacing.s16)

        /**
         * El corte entre compacta y holgada se pone a 400 dp: por debajo, los anchos
         * reales de 360 a 393 dp usan la compacta; de 400 a 599 dp, la holgada.
         */
        fun paraAncho(anchoDp: Int): AgendaGrid = when {
            anchoDp >= 600 -> Ancha
            anchoDp >= 400 -> Holgada
            else -> Compacta
        }
    }
}
