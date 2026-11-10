package com.dpinta.agenda.wear

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.Duration
import kotlin.math.cos
import kotlin.math.sin

/**
 * Anillo de cuenta (CountdownRing): la línea del día enrollada. Un riel circular que se **vacía** conforme se acerca
 * la actividad (la última hora ocupa la vuelta entera), con una muesca en la hora de salida. Un solo anillo: no es un
 * monitor de actividad. Se redibuja una vez por minuto (quien lo usa pasa el tiempo ya redondeado).
 *
 * @param restante tiempo hasta el inicio.
 * @param salidaAntes cuánto antes del inicio hay que salir (null si no hay trayecto): posición de la muesca.
 * @param ambiente siempre activo: solo contornos finos, sin color de urgencia.
 */
@Composable
fun AnilloCuenta(
    restante: Duration,
    salidaAntes: Duration?,
    color: Color,
    modifier: Modifier = Modifier,
    ambiente: Boolean = false,
    kind: com.dpinta.agenda.domain.WatchSession.Kind = com.dpinta.agenda.domain.WatchSession.Kind.CLASE,
    mode: com.dpinta.agenda.domain.TransportMode? = null,
    urgente: Boolean = false,
    cuadro: Int = 0,
) {
    val ventana = 60f
    val fraccion = (restante.toMinutes().toFloat() / ventana).coerceIn(0f, 1f)
    Canvas(modifier) {
        val grosor = if (ambiente) 2.dp.toPx() else 10.dp.toPx() // stroke-anillo
        val margen = grosor / 2 + 3.dp.toPx()
        val tam = Size(size.width - margen * 2, size.height - margen * 2)
        val origen = Offset(margen, margen)
        drawArc(if (ambiente) Reloj.linea else Reloj.superficieFuerte, 0f, 360f, false, origen, tam, style = Stroke(grosor))
        if (fraccion > 0f) {
            drawArc(if (ambiente) Reloj.tintaSuave else color, -90f, 360f * fraccion, false, origen, tam,
                style = Stroke(grosor, cap = StrokeCap.Round))
        }
        // Muesca de la salida: un punto sobre el riel, del color de la hora, solo si cae dentro de la ventana.
        val antes = salidaAntes?.toMinutes()?.toFloat()
        if (!ambiente && antes != null && antes in 0f..ventana) {
            val ang = Math.toRadians((-90.0 + 360.0 * antes / ventana))
            val r = tam.width / 2
            val c = Offset(size.width / 2 + (r * cos(ang)).toFloat(), size.height / 2 + (r * sin(ang)).toFloat())
            drawCircle(Reloj.fondo, grosor * 0.55f, c)
            drawCircle(Reloj.hora, grosor * 0.35f, c)
        }
        // «El camino a clase» (anexo 2026-11-10-campuswatch-vivo, dirección A): el edificio de la actividad a las 12 y
        // una figura que camina por dentro del riel, en la punta de la cuenta. Avanza un paso por minuto; sin animación
        // continua (batería) y nada de esto en siempre activo.
        if (!ambiente) {
            val r = tam.width / 2 - grosor * 2.1f
            fun punto(grados: Double) = Offset(
                size.width / 2 + (r * cos(Math.toRadians(grados))).toFloat(),
                size.height / 2 + (r * sin(Math.toRadians(grados))).toFloat(),
            )
            // A+ (Ruta y compañero): el destino cambia con el tipo y el viajero con el modo y la urgencia.
            destino(punto(-90.0), kind, color)
            if (fraccion > 0.04f) {
                if (VIAJERO_SOBRE_RIEL) {
                    // Sobre la vía: el viajero va encima del riel, con un halo del fondo, y nunca toca el texto.
                    val rr = tam.width / 2 - 5.dp.toPx()
                    val a = Math.toRadians(-90.0 + 360.0 * fraccion)
                    val q = Offset(size.width / 2 + (rr * cos(a)).toFloat(), size.height / 2 + (rr * sin(a)).toFloat())
                    drawCircle(Reloj.fondo, 12.dp.toPx(), q)
                    viajero(q + Offset(0f, 1.dp.toPx()), mode, urgente, cuadro)
                } else {
                    viajero(punto(-90.0 + 360.0 * fraccion), mode, urgente, cuadro)
                }
            }
        }
    }
}

/** Anillo de la sesión en curso: fracción que queda de la sesión, en el color de su línea; en siempre activo, contorno. */
@Composable
fun AnilloSesion(queda: Float, color: Color, modifier: Modifier = Modifier, ambiente: Boolean = false) {
    val f = queda.coerceIn(0f, 1f)
    Canvas(modifier) {
        val grosor = if (ambiente) 2.dp.toPx() else 10.dp.toPx()
        val margen = grosor / 2 + 3.dp.toPx()
        val tam = Size(size.width - margen * 2, size.height - margen * 2)
        val origen = Offset(margen, margen)
        drawArc(if (ambiente) Reloj.linea else Reloj.superficieFuerte, 0f, 360f, false, origen, tam, style = Stroke(grosor))
        if (f > 0f) drawArc(if (ambiente) Reloj.tintaSuave else color, -90f, 360f * f, false, origen, tam, style = Stroke(grosor, cap = StrokeCap.Round))
    }
}

/** Dónde va el viajero: dentro del riel (dirección A aprobada) o encima de él. Encima, aprobado por Diego el 2026-11-10 (anexo campuswatch-vivo §5). */
const val VIAJERO_SOBRE_RIEL = true
