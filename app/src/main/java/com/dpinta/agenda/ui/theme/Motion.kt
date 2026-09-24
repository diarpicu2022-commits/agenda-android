package com.dpinta.agenda.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Movimiento. Contrato de diseño, cláusula C8. Inventario cerrado: nada más se anima.
 * Solo transform y opacity (graphicsLayer), nunca layout.
 */
object AgendaMotion {
    /** ease-out-quint (C8.2). */
    val salida: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Cambio de estado de la banda: fundido cruzado de alpha (C8.2). */
    const val CAMBIO_ESTADO_MS = 200

    /** Cambio de dígito tipo tablero: translationY + alpha, solo en los dígitos que cambian (C8.2). */
    const val DIGITO_MS = 180

    /** Recorrido vertical del dígito que entra: de -8 dp a 0 (C8.2). */
    val desplazamientoDigito: Dp = 8.dp

    /** Hilo del día (C8.2). Se usa en el paso de Hoy. */
    const val HILO_MS = 240

    /** Pulsación de botón (C8.2). */
    const val PULSACION_MS = 120
}

/**
 * Movimiento reducido (C8.4): si la escala de duración de animaciones del sistema es 0
 * (incluye «Quitar animaciones» de accesibilidad), todo pasa a corte instantáneo.
 */
@Composable
fun rememberReducirMovimiento(): Boolean {
    val contexto = LocalContext.current
    return remember(contexto) {
        Settings.Global.getFloat(
            contexto.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
