package com.dpinta.agenda.wear

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Poses del compañero (anexo 2026-11-10-campuswatch-vivo, A+ paso 2). */
enum class PoseCuy { PREGUNTA, FELIZ, PREOCUPADO, DORMIDO }

/**
 * El cuy estudiante de Nariño, de lado: pelo en `aviso` (ámbar) con manchas del fondo, ojo y patas en tinta, birrete en
 * `ruta` y un acento por pose (brillo en `exito`, gota en `ruta`, «z» en `tintaSuave`). Es compañía, no información: TalkBack no lo lee (el texto de al lado
 * ya dice lo mismo). [progreso] de 0 a 1 dibuja la insignia de check cuando la confirmación la lleva.
 */
@Composable
fun Cuy(pose: PoseCuy, modifier: Modifier = Modifier, insignia: Boolean? = null, progreso: Float = 1f) {
    Canvas(modifier.clearAndSetSemantics { }) { cuy(pose, insignia, progreso) }
}

private fun DrawScope.cuy(pose: PoseCuy, insignia: Boolean?, progreso: Float) {
    val s = size.minDimension
    val k = s / 100f // el dibujo está pensado en una caja de 100 × 100
    fun o(x: Float, y: Float) = Offset(x * k, y * k)
    val tinta = Reloj.tinta
    val fondo = Reloj.fondo
    // Salto de alegría: medio cuerpo más arriba.
    val dy = if (pose == PoseCuy.FELIZ) -4f else 0f
    fun oy(x: Float, y: Float) = o(x, y + dy)

    // Cuerpo de lado, mirando a la derecha: una hogaza alargada en ámbar con manchas del fondo (el cuy pinto de Nariño).
    val pelo = Reloj.aviso
    drawOval(pelo, oy(10f, 46f), Size(66f * k, 40f * k))
    drawCircle(pelo, 21f * k, oy(70f, 60f))
    drawOval(fondo, oy(18f, 52f), Size(22f * k, 14f * k)) // mancha del lomo
    drawOval(fondo, oy(78f, 58f), Size(13f * k, 16f * k)) // mancha de la cara
    // Oreja caída y patitas.
    drawOval(tinta, oy(56f, 40f), Size(12f * k, 9f * k))
    drawOval(tinta, oy(26f, 82f), Size(13f * k, 7f * k))
    drawOval(tinta, oy(58f, 82f), Size(13f * k, 7f * k))
    drawCircle(tinta, 2.6f * k, oy(90.5f, 64f)) // nariz

    // Ojo según la pose, en tinta sobre el ámbar.
    val ojo = oy(74f, 54f)
    val trazo = 3f * k
    when (pose) {
        PoseCuy.FELIZ -> drawArc(tinta, 200f, 140f, false, ojo - Offset(4.5f * k, 4.5f * k), Size(9f * k, 9f * k), style = Stroke(trazo, cap = StrokeCap.Round))
        PoseCuy.DORMIDO -> drawLine(tinta, ojo - Offset(4.5f * k, 0f), ojo + Offset(4.5f * k, 0f), trazo, StrokeCap.Round)
        PoseCuy.PREOCUPADO -> {
            drawCircle(tinta, 3.6f * k, ojo)
            drawLine(tinta, ojo + Offset(-5f * k, -8f * k), ojo + Offset(4f * k, -6f * k), 2.2f * k, StrokeCap.Round)
        }
        PoseCuy.PREGUNTA -> {
            drawCircle(tinta, 4f * k, ojo)
            drawCircle(fondo, 1.4f * k, ojo + Offset(1.2f * k, -1.2f * k))
        }
    }

    // Birrete sobre la cabeza: tablero en rombo, gorro y borla.
    val ruta = Reloj.ruta
    drawPath(Path().apply {
        moveTo(70f * k, (24f + dy) * k); lineTo(96f * k, (32f + dy) * k); lineTo(70f * k, (40f + dy) * k); lineTo(44f * k, (32f + dy) * k); close()
    }, ruta)
    drawRect(ruta, oy(59f, 33f), Size(22f * k, 9f * k))
    drawLine(ruta, oy(70f, 32f), oy(48f, 35f), 1.8f * k, StrokeCap.Round)
    drawLine(ruta, oy(48f, 35f), oy(48f, 44f), 1.8f * k, StrokeCap.Round)
    drawCircle(ruta, 2.4f * k, oy(48f, 45f))

    // Acento por pose, arriba a la izquierda (el cuy mira a la derecha).
    when (pose) {
        PoseCuy.FELIZ -> for ((x, y) in listOf(12f to 26f, 28f to 14f, 6f to 44f)) destello(o(x, y), 5f * k)
        PoseCuy.PREOCUPADO -> drawPath(Path().apply {
            moveTo(30f * k, 22f * k); cubicTo(26f * k, 30f * k, 24f * k, 34f * k, 28f * k, 37f * k)
            cubicTo(32f * k, 39f * k, 36f * k, 34f * k, 30f * k, 22f * k); close()
        }, Reloj.ruta)
        PoseCuy.DORMIDO -> {
            zeta(o(30f, 26f), 9f * k)
            zeta(o(16f, 12f), 6f * k)
        }
        PoseCuy.PREGUNTA -> {
            val c = o(24f, 18f)
            drawArc(Reloj.ruta, 200f, 250f, false, c - Offset(6f * k, 6f * k), Size(12f * k, 12f * k), style = Stroke(3f * k, cap = StrokeCap.Round))
            drawLine(Reloj.ruta, c + Offset(0f, 6f * k), c + Offset(0f, 10f * k), 3f * k, StrokeCap.Round)
            drawCircle(Reloj.ruta, 1.8f * k, c + Offset(0f, 15f * k))
        }
    }

    // Insignia de la confirmación: círculo con check (o «!» si no llegó al teléfono), abajo a la derecha.
    if (insignia != null) {
        val c = o(16f, 84f)
        val r = 14f * k
        val color = if (insignia) Reloj.exito else Reloj.aviso
        drawCircle(fondo, r + 3f * k, c)
        drawCircle(color, r, c)
        val p = if (insignia) Path().apply { moveTo(c.x - 6f * k, c.y); lineTo(c.x - 1.5f * k, c.y + 5f * k); lineTo(c.x + 7f * k, c.y - 5f * k) }
        else Path().apply { moveTo(c.x, c.y - 7f * k); lineTo(c.x, c.y + 2f * k) }
        val medida = androidx.compose.ui.graphics.PathMeasure().apply { setPath(p, false) }
        val parcial = Path()
        medida.getSegment(0f, medida.length * progreso, parcial, true)
        drawPath(parcial, fondo, style = Stroke(3.4f * k, cap = StrokeCap.Round))
        if (!insignia && progreso >= 1f) drawCircle(fondo, 2f * k, c + Offset(0f, 7f * k))
    }
}

/** Ojo cerrado contento: arco hacia arriba. */
private fun DrawScope.arco(c: Offset, r: Float, arriba: Boolean, trazo: Float) {
    drawArc(Reloj.fondo, if (arriba) 200f else 20f, 140f, false, c - Offset(r, r), Size(2 * r, 2 * r), style = Stroke(trazo, cap = StrokeCap.Round))
}

/** Brillo de cuatro puntas. */
private fun DrawScope.destello(c: Offset, r: Float) {
    drawLine(Reloj.exito, c - Offset(0f, r), c + Offset(0f, r), r * 0.45f, StrokeCap.Round)
    drawLine(Reloj.exito, c - Offset(r, 0f), c + Offset(r, 0f), r * 0.45f, StrokeCap.Round)
}

/** «z» de sueño. */
private fun DrawScope.zeta(c: Offset, r: Float) {
    val p = Path().apply { moveTo(c.x - r / 2, c.y - r / 2); lineTo(c.x + r / 2, c.y - r / 2); lineTo(c.x - r / 2, c.y + r / 2); lineTo(c.x + r / 2, c.y + r / 2) }
    drawPath(p, Reloj.tintaSuave, style = Stroke(r * 0.28f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}
