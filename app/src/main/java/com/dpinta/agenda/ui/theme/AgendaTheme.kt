package com.dpinta.agenda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

val LocalAgendaColors = staticCompositionLocalOf { AgendaColoresClaro }
val LocalAgendaType = staticCompositionLocalOf { AgendaType() }
val LocalAgendaGrid = staticCompositionLocalOf { AgendaGrid.Compacta }

/**
 * Tema de la agenda (contrato de diseño, dirección A «Tablero de salidas»).
 *
 * - Colores propios en claro y oscuro (C3). Sin dynamicColor: la app no toma el color
 *   del fondo de pantalla, así el contraste medido se mantiene.
 * - Material 3 como esqueleto de comportamiento: sus roles de color, tipo y forma
 *   se derivan de los tokens (C3, C2.3, C5.1).
 * - Retícula según el ancho real de la ventana (C4.2).
 */
@Composable
fun AgendaTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colores = if (oscuro) AgendaColoresOscuro else AgendaColoresClaro
    val tipo = remember { AgendaType() }
    val anchoPx = LocalWindowInfo.current.containerSize.width
    val anchoDp = with(LocalDensity.current) { anchoPx.toDp().value.toInt() }
    val reticula = AgendaGrid.paraAncho(anchoDp)

    CompositionLocalProvider(
        LocalAgendaColors provides colores,
        LocalAgendaType provides tipo,
        LocalAgendaGrid provides reticula,
    ) {
        MaterialTheme(
            colorScheme = colores.toMaterialScheme(),
            typography = tipo.toMaterialTypography(),
            shapes = AgendaShapes,
            content = content,
        )
    }
}

/** Acceso a los tokens: `AgendaTheme.colores.tinta`, `AgendaTheme.tipo.horaSalida`… */
object AgendaTheme {
    val colores: AgendaColors
        @Composable @ReadOnlyComposable get() = LocalAgendaColors.current
    val tipo: AgendaType
        @Composable @ReadOnlyComposable get() = LocalAgendaType.current
    val reticula: AgendaGrid
        @Composable @ReadOnlyComposable get() = LocalAgendaGrid.current
    val espacio: AgendaSpacing get() = AgendaSpacing
    val medidas: AgendaMedidas get() = AgendaMedidas
}
