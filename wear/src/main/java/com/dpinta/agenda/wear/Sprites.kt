package com.dpinta.agenda.wear

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.WatchSession

/*
 * «Ruta y compañero» (anexo 2026-11-10-campuswatch-vivo, dirección A+, decisión de Diego del 2026-11-10).
 * Sprites dibujados con la misma familia del reloj: formas llenas en tinta o en el color de la urgencia, trazo redondo
 * de 2,6 dp, sin degradados. El destino cambia con el tipo de actividad y el viajero con el modo de transporte y la
 * urgencia. [cuadro] es el fotograma 0..3 del ciclo: la pantalla lo anima un momento al encenderse y luego lo deja fijo.
 */

/** Destino a las 12 según el tipo: campus para clases, tienda para turnos, casa para lo personal y las entregas. */
fun DrawScope.destino(c: Offset, kind: WatchSession.Kind, color: Color) = when (kind) {
    WatchSession.Kind.CLASE -> campus(c, color)
    WatchSession.Kind.TRABAJO -> tienda(c, color)
    WatchSession.Kind.PERSONAL, WatchSession.Kind.ENTREGA -> casa(c, color)
}

/** Fachada del campus: escalinata, tres columnas, entablamento y frontón. */
private fun DrawScope.campus(c: Offset, color: Color) {
    val u = 1.dp.toPx()
    val ancho = 18 * u
    val suelo = c.y + 7 * u
    val izq = c.x - ancho / 2
    drawRect(color, Offset(izq - u, suelo - 2 * u), Size(ancho + 2 * u, 2 * u))
    for (k in 0..2) drawRect(color, Offset(izq + 2 * u + k * 6 * u, suelo - 10 * u), Size(2.6f * u, 8 * u))
    drawRect(color, Offset(izq, suelo - 12.5f * u), Size(ancho, 2.5f * u))
    drawPath(Path().apply {
        moveTo(izq - u, suelo - 12.5f * u); lineTo(c.x, suelo - 18.5f * u); lineTo(izq + ancho + u, suelo - 12.5f * u); close()
    }, color)
}

/** Tienda: cuerpo, toldo a rayas (huecos del color del fondo) y puerta. */
private fun DrawScope.tienda(c: Offset, color: Color) {
    val u = 1.dp.toPx()
    val ancho = 18 * u
    val suelo = c.y + 7 * u
    val izq = c.x - ancho / 2
    drawRect(color, Offset(izq, suelo - 11 * u), Size(ancho, 11 * u))
    drawRect(Reloj.fondo, Offset(c.x - 2.5f * u, suelo - 6.5f * u), Size(5 * u, 6.5f * u))
    // Toldo: trapecio con tres franjas del fondo.
    drawPath(Path().apply {
        moveTo(izq - 2 * u, suelo - 11 * u); lineTo(izq + u, suelo - 16 * u); lineTo(izq + ancho - u, suelo - 16 * u)
        lineTo(izq + ancho + 2 * u, suelo - 11 * u); close()
    }, color)
    for (k in 1..3) drawRect(Reloj.fondo, Offset(izq + k * 5 * u - 0.6f * u, suelo - 15.5f * u), Size(1.4f * u, 4 * u))
}

/** Casa: tejado a dos aguas, cuerpo, ventana y puerta. */
private fun DrawScope.casa(c: Offset, color: Color) {
    val u = 1.dp.toPx()
    val ancho = 15 * u
    val suelo = c.y + 7 * u
    val izq = c.x - ancho / 2
    drawRect(color, Offset(izq, suelo - 9 * u), Size(ancho, 9 * u))
    drawPath(Path().apply {
        moveTo(izq - 2.5f * u, suelo - 8.5f * u); lineTo(c.x, suelo - 17 * u); lineTo(izq + ancho + 2.5f * u, suelo - 8.5f * u); close()
    }, color)
    drawRect(Reloj.fondo, Offset(izq + 2.5f * u, suelo - 6.5f * u), Size(3.5f * u, 3 * u))
    drawRect(Reloj.fondo, Offset(c.x + 1.5f * u, suelo - 5.5f * u), Size(3.5f * u, 5.5f * u))
}

/** El viajero en la punta de la cuenta, según el modo; a pie corre si la salida ya es urgente. */
fun DrawScope.viajero(p: Offset, mode: TransportMode?, urgente: Boolean, cuadro: Int) = when (mode) {
    TransportMode.TRANSPORTE_PUBLICO -> bus(p, cuadro)
    TransportMode.MOTO -> moto(p, cuadro)
    TransportMode.CARRO -> carro(p, cuadro)
    TransportMode.A_PIE, null -> persona(p, urgente, cuadro)
}

/**
 * Persona con ciclo de cuatro fotogramas (contacto, paso, contacto, paso). Corriendo: el tronco se inclina, la zancada
 * es más larga y los brazos van doblados.
 */
private fun DrawScope.persona(p: Offset, corre: Boolean, cuadro: Int) {
    val u = 1.dp.toPx()
    val w = 2.6f * u
    val tinta = Reloj.tinta
    val inclina = if (corre) 2.2f * u else 0.6f * u
    val rebote = if (cuadro % 2 == 1) -0.8f * u else 0f
    val cadera = p + Offset(0f, 2 * u + rebote)
    val hombro = p + Offset(inclina, -5 * u + rebote)
    drawCircle(tinta, 3.4f * u, p + Offset(inclina + 0.8f * u, -10 * u + rebote))
    drawLine(tinta, hombro, cadera, w, StrokeCap.Round)
    // Piernas: abiertas en 0 y 2 (lados opuestos), juntas en 1 y 3.
    val zancada = if (corre) 5.5f * u else 4f * u
    val (adelante, atras) = when (cuadro % 4) {
        0 -> zancada to -zancada * 0.8f
        2 -> -zancada * 0.8f to zancada
        else -> 1.2f * u to -0.6f * u
    }
    drawLine(tinta, cadera, cadera + Offset(adelante, 7 * u - rebote), w, StrokeCap.Round)
    drawLine(tinta, cadera, cadera + Offset(atras, 7 * u - rebote), w, StrokeCap.Round)
    // Brazos en contra de las piernas.
    if (corre) {
        val codo = hombro + Offset(-atras * 0.45f, 3 * u)
        drawLine(tinta, hombro, codo, w * 0.85f, StrokeCap.Round)
        drawLine(tinta, codo, codo + Offset(2.5f * u, -1.5f * u), w * 0.85f, StrokeCap.Round)
    } else {
        drawLine(tinta, hombro, hombro + Offset(atras * 0.75f, 5 * u), w * 0.85f, StrokeCap.Round)
    }
    // Mochila: el estudiante va a clase.
    drawRoundRect(tinta, hombro + Offset(-4.6f * u, 0.4f * u), Size(3 * u, 4.6f * u), CornerRadius(u, u))
}

/** Rueda con un rayo que gira un cuarto por fotograma. */
private fun DrawScope.rueda(c: Offset, r: Float, cuadro: Int) {
    val u = 1.dp.toPx()
    drawCircle(Reloj.tinta, r, c)
    drawCircle(Reloj.fondo, r * 0.42f, c)
    val ang = Math.toRadians(45.0 * cuadro)
    val d = Offset((kotlin.math.cos(ang) * r * 0.9f).toFloat(), (kotlin.math.sin(ang) * r * 0.9f).toFloat())
    drawLine(Reloj.fondo, c - d, c + d, 0.9f * u)
}

/** Bus urbano: caja redondeada, tres ventanas, puerta y dos ruedas; rebota medio punto en los fotogramas impares. */
private fun DrawScope.bus(p: Offset, cuadro: Int) {
    val u = 1.dp.toPx()
    val y = if (cuadro % 2 == 1) -0.6f * u else 0f
    val izq = p.x - 10 * u
    val arriba = p.y - 7 * u + y
    drawRoundRect(Reloj.tinta, Offset(izq, arriba), Size(20 * u, 10 * u), CornerRadius(2.4f * u, 2.4f * u))
    for (k in 0..2) drawRect(Reloj.fondo, Offset(izq + 2 * u + k * 5 * u, arriba + 2 * u), Size(3.6f * u, 3 * u))
    drawRect(Reloj.fondo, Offset(izq + 17 * u, arriba + 2 * u), Size(1.6f * u, 6 * u))
    rueda(Offset(izq + 5 * u, arriba + 10.5f * u), 2.4f * u, cuadro)
    rueda(Offset(izq + 15 * u, arriba + 10.5f * u), 2.4f * u, cuadro)
}

/** Moto de lado: dos ruedas, cuerpo con asiento, manubrio y el conductor sentado con casco y visera. */
private fun DrawScope.moto(p: Offset, cuadro: Int) {
    val u = 1.dp.toPx()
    val w = 2.4f * u
    val y = if (cuadro % 2 == 1) -0.5f * u else 0f
    rueda(Offset(p.x - 7 * u, p.y + 4 * u), 3 * u, cuadro)
    rueda(Offset(p.x + 7 * u, p.y + 4 * u), 3 * u, cuadro)
    // Cuerpo: tanque y asiento en una sola pieza.
    drawPath(Path().apply {
        moveTo(p.x - 7 * u, p.y + 2 * u + y); lineTo(p.x + 5 * u, p.y + 2 * u + y); lineTo(p.x + 6.5f * u, p.y - 1.5f * u + y)
        lineTo(p.x + 1 * u, p.y - 2.5f * u + y); lineTo(p.x - 6 * u, p.y - 1 * u + y); close()
    }, Reloj.tinta)
    drawLine(Reloj.tinta, Offset(p.x + 5.5f * u, p.y - 1 * u + y), Offset(p.x + 7 * u, p.y + 4 * u), w * 0.8f, StrokeCap.Round)
    drawLine(Reloj.tinta, Offset(p.x + 5 * u, p.y - 4.5f * u + y), Offset(p.x + 6.5f * u, p.y - 1.5f * u + y), w * 0.8f, StrokeCap.Round)
    // Conductor sentado.
    val cadera = Offset(p.x - 2.5f * u, p.y - 3 * u + y)
    val hombro = Offset(p.x - 0.5f * u, p.y - 9 * u + y)
    drawLine(Reloj.tinta, cadera, hombro, w, StrokeCap.Round)
    drawLine(Reloj.tinta, hombro, Offset(p.x + 5 * u, p.y - 4.5f * u + y), w * 0.8f, StrokeCap.Round)
    drawLine(Reloj.tinta, cadera, Offset(p.x + 1.5f * u, p.y + 0.5f * u + y), w * 0.9f, StrokeCap.Round)
    val casco = hombro + Offset(0.6f * u, -3.6f * u)
    drawCircle(Reloj.tinta, 3.2f * u, casco)
    drawRect(Reloj.fondo, casco + Offset(0.6f * u, -1 * u), Size(2.4f * u, 1.3f * u))
}

/** Carro: carrocería con techo, dos ventanas y dos ruedas. */
private fun DrawScope.carro(p: Offset, cuadro: Int) {
    val u = 1.dp.toPx()
    val y = if (cuadro % 2 == 1) -0.5f * u else 0f
    val izq = p.x - 10 * u
    val base = p.y + 1 * u + y
    drawRoundRect(Reloj.tinta, Offset(izq, base - 5 * u), Size(20 * u, 6 * u), CornerRadius(2 * u, 2 * u))
    drawPath(Path().apply {
        moveTo(izq + 4 * u, base - 5 * u); lineTo(izq + 7 * u, base - 10 * u); lineTo(izq + 14 * u, base - 10 * u)
        lineTo(izq + 17 * u, base - 5 * u); close()
    }, Reloj.tinta)
    drawRect(Reloj.fondo, Offset(izq + 7.6f * u, base - 9 * u), Size(2.8f * u, 3.4f * u))
    drawRect(Reloj.fondo, Offset(izq + 11.4f * u, base - 9 * u), Size(2.8f * u, 3.4f * u))
    rueda(Offset(izq + 5 * u, base + 1.5f * u), 2.6f * u, cuadro)
    rueda(Offset(izq + 15 * u, base + 1.5f * u), 2.6f * u, cuadro)
}

/**
 * El cielo de Pasto por encima del texto: dos nubes y el sol de día o la luna de noche, en `superficieFuerte` como el
 * Galeras. Es paisaje, no información; queda en la franja alta, lejos de la cifra.
 */
fun DrawScope.cielo(hora: Int) {
    val u = 1.dp.toPx()
    val w = size.width
    val h = size.height
    val tono = Reloj.superficieFuerte
    fun nube(c: Offset, k: Float) {
        drawCircle(tono, 5 * u * k, c)
        drawCircle(tono, 7 * u * k, c + Offset(7 * u * k, -2.5f * u * k))
        drawCircle(tono, 5.5f * u * k, c + Offset(14 * u * k, 0f))
        drawRoundRect(tono, c + Offset(-2 * u * k, 0f), Size(19 * u * k, 5 * u * k), CornerRadius(2.5f * u * k, 2.5f * u * k))
    }
    nube(Offset(w * 0.14f, h * 0.27f), 0.9f)
    nube(Offset(w * 0.70f, h * 0.17f), 0.75f)
    val astro = Offset(w * 0.27f, h * 0.17f)
    if (hora in 6..17) {
        drawCircle(tono, 6 * u, astro)
        for (k in 0 until 8) {
            val a = Math.toRadians(45.0 * k)
            val d = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
            drawLine(tono, astro + d * (8.5f * u), astro + d * (11 * u), 1.8f * u, StrokeCap.Round)
        }
    } else {
        drawCircle(tono, 6 * u, astro)
        drawCircle(Reloj.fondo, 5.2f * u, astro + Offset(2.6f * u, -1.6f * u))
    }
}
