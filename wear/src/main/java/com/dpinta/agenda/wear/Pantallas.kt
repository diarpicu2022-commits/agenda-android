package com.dpinta.agenda.wear

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.dpinta.agenda.domain.Urgency
import com.dpinta.agenda.domain.WatchDay
import com.dpinta.agenda.domain.WatchSession
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

private val llegada = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f) // ease-llegada

/** 1 · Próxima (WatchNextActivity): anillo, «Empieza en», cifra, nombre y aula. Nunca más de tres datos. */
@Composable
fun PantallaProxima(s: WatchSession, ahora: LocalDateTime, zona: ZoneId, ambiente: Boolean) {
    val u = s.urgency(ahora, zona)
    val color = Reloj.urgencia(u, s.exam)
    val restante = Duration.between(ahora, s.start)
    Box(Modifier.fillMaxSize().background(Reloj.fondo), contentAlignment = Alignment.Center) {
        AnilloCuenta(restante, s.leaveAt?.let { Duration.between(it, s.start) }, color, Modifier.fillMaxSize(), ambiente)
        Column(
            Modifier.padding(horizontal = 30.dp).semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (s.exam) Text("PARCIAL", style = Tipo.rotulo, color = if (ambiente) Reloj.tinta else Reloj.ciruela)
            else Text(if (u == Urgency.MISSED) "Empezó hace" else "Empieza en", style = Tipo.apoyo, color = Reloj.tintaSuave)
            Text(
                Formato.cuenta(if (u == Urgency.MISSED) Duration.between(s.start, ahora) else restante),
                style = Tipo.hora,
                color = if (u == Urgency.MISSED) Color(CW.urgPerdida) else Reloj.tinta,
                maxLines = 1,
            )
            Text(
                s.title, style = Tipo.titulo, color = Reloj.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (u == Urgency.MISSED) TextDecoration.LineThrough else null,
            )
            Text(listOf(s.room.ifBlank { s.place }, Formato.hora(s.start.toLocalTime())).filter { it.isNotBlank() }.joinToString(" · "),
                style = Tipo.apoyo, color = Reloj.tintaSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 2 · Es hora de salir (WatchLeaveNow): rótulo, destino y cuenta; «Ya voy» debajo y el resto de acciones al bajar. */
@Composable
fun PantallaSalir(s: WatchSession, ahora: LocalDateTime, zona: ZoneId, ambiente: Boolean, onAccion: (Accion) -> Unit) {
    val u = s.urgency(ahora, zona)
    val color = Reloj.urgencia(u, s.exam)
    Box(Modifier.fillMaxSize().background(Reloj.fondo), contentAlignment = Alignment.Center) {
        AnilloCuenta(Duration.between(ahora, s.start), s.leaveAt?.let { Duration.between(it, s.start) }, color, Modifier.fillMaxSize(), ambiente)
        Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (u == Urgency.URGENT) "SAL YA" else "ES HORA DE SALIR", style = Tipo.rotulo, color = if (ambiente) Reloj.tinta else color,
                modifier = Modifier.semantics { heading() })
            Text(s.title, style = Tipo.titulo, color = Reloj.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("comienza en ${Formato.cuenta(Duration.between(ahora, s.start))}", style = Tipo.apoyo, color = Reloj.tintaSuave)
            Spacer(Modifier.height(10.dp))
            if (!ambiente) BotonReloj(Accion.YaVoy.etiqueta, principal = true, color = color) { onAccion(Accion.YaVoy) }
        }
    }
}

/** 4 · Acciones (WatchActions): Ya voy, Aplazar 5 min, Cancelar sesión de hoy; uno por fila, toque de 48 dp. */
@Composable
fun PantallaAcciones(s: WatchSession, color: Color, onAccion: (Accion) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Reloj.fondo).padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(s.title, style = Tipo.apoyo, color = Reloj.tintaSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
        BotonReloj(Accion.YaVoy.etiqueta, principal = true, color = color) { onAccion(Accion.YaVoy) }
        BotonReloj(Accion.Aplazar.etiqueta) { onAccion(Accion.Aplazar) }
        BotonReloj(Accion.Cancelar.etiqueta) { onAccion(Accion.Cancelar) }
    }
}

/** 5 · Horario (WatchSchedule): hoy en filas cortas, la actual resaltada, y el estado de los datos locales al pie. */
@Composable
fun PantallaHorario(dia: WatchDay, fecha: java.time.LocalDate, ahora: LocalDateTime, zona: ZoneId) {
    val filas = dia.on(fecha)
    val actual = dia.current(ahora) ?: dia.next(ahora)
    Column(
        Modifier.fillMaxSize().background(Reloj.fondo).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Formato.dia(fecha, ahora.toLocalDate()), style = Tipo.apoyo.copy(fontWeight = Tipo.titulo.fontWeight), color = Reloj.ruta,
            modifier = Modifier.semantics { heading() })
        if (filas.isEmpty()) {
            Text("Nada fijo", style = Tipo.titulo, color = Reloj.tintaSuave, modifier = Modifier.padding(vertical = 8.dp))
        }
        for (s in filas.take(4)) FilaHorario(s, s == actual, s.end.isBefore(ahora) || s.end == ahora)
        if (filas.size > 4) Text("y ${filas.size - 4} más", style = Tipo.apoyo, color = Reloj.tintaSuave)
        Text(pieDatos(dia, ahora), style = Tipo.apoyo, color = Reloj.tintaSuave, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FilaHorario(s: WatchSession, actual: Boolean, hecha: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (actual) Reloj.horaSuave else Color.Transparent)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(Formato.hora(s.start.toLocalTime()), style = Tipo.apoyo.copy(fontWeight = Tipo.titulo.fontWeight),
            color = if (actual) Reloj.hora else Reloj.tintaSuave)
        Spacer(Modifier.width(8.dp))
        MarcaTipo(s, hecha, actual)
        Spacer(Modifier.width(8.dp))
        Text(s.title, style = Tipo.apoyo.copy(fontWeight = Tipo.titulo.fontWeight, textAlign = TextAlign.Start),
            color = if (hecha) Reloj.tintaSuave else Reloj.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Marca de tipo con la misma forma que en Agenda: clase ●, trabajo ■, entrega ▲, parcial ◆; hecha → ✓. */
@Composable
private fun MarcaTipo(s: WatchSession, hecha: Boolean, actual: Boolean) {
    val c = if (s.exam) Reloj.ciruela else Reloj.tipo(s.kind)
    Canvas(Modifier.size(12.dp)) {
        val w = size.width
        when {
            hecha -> {
                val p = Path().apply { moveTo(w * 0.15f, w * 0.55f); lineTo(w * 0.42f, w * 0.8f); lineTo(w * 0.88f, w * 0.22f) }
                drawPath(p, Reloj.tintaSuave, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            s.exam -> drawPath(Path().apply { moveTo(w / 2, 0f); lineTo(w, w / 2); lineTo(w / 2, w); lineTo(0f, w / 2); close() }, c)
            s.kind == WatchSession.Kind.TRABAJO -> drawRect(c, Offset(w * 0.1f, w * 0.1f), androidx.compose.ui.geometry.Size(w * 0.8f, w * 0.8f))
            s.kind == WatchSession.Kind.ENTREGA -> drawPath(Path().apply { moveTo(w / 2, w * 0.08f); lineTo(w * 0.95f, w * 0.9f); lineTo(w * 0.05f, w * 0.9f); close() }, c)
            else -> {
                drawCircle(c, w * 0.4f)
                if (actual) drawCircle(Reloj.hora, w * 0.55f, style = Stroke(2.dp.toPx()))
            }
        }
    }
}

/** 7 · Confirmación (WatchConfirmation): un check dibujado y la frase de lo que pasó; vuelve solo. */
@Composable
fun PantallaConfirmacion(texto: String, detalle: String?, ok: Boolean = true, onFin: () -> Unit) {
    val contexto = LocalContext.current
    val trazo = remember { Animatable(if (sinMovimiento(contexto)) 1f else 0f) }
    LaunchedEffect(Unit) {
        trazo.animateTo(1f, tween(480, easing = llegada)) // dur-lenta
        kotlinx.coroutines.delay(1600)
        onFin()
    }
    Column(
        Modifier.fillMaxSize().background(Reloj.fondo).padding(horizontal = 30.dp)
            .semantics(mergeDescendants = true) { contentDescription = listOfNotNull(texto, detalle).joinToString(". ") },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(56.dp)) {
            val w = size.width
            // Si no llegó al teléfono no hay check: un aviso (no se confirma lo que no pasó).
            val color = if (ok) Reloj.exito else Color(CW.aviso)
            drawCircle(color, w / 2, style = Stroke(3.dp.toPx()))
            val p = if (ok) Path().apply { moveTo(w * 0.28f, w * 0.52f); lineTo(w * 0.44f, w * 0.68f); lineTo(w * 0.74f, w * 0.36f) }
            else Path().apply { moveTo(w * 0.5f, w * 0.26f); lineTo(w * 0.5f, w * 0.58f) }
            val medida = androidx.compose.ui.graphics.PathMeasure().apply { setPath(p, false) }
            val parcial = Path()
            medida.getSegment(0f, medida.length * trazo.value, parcial, true)
            drawPath(parcial, color, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (!ok && trazo.value >= 1f) drawCircle(color, 2.6.dp.toPx(), Offset(w * 0.5f, w * 0.73f))
        }
        Spacer(Modifier.height(10.dp))
        Text(texto, style = Tipo.titulo, color = Reloj.tinta)
        if (detalle != null) Text(detalle, style = Tipo.apoyo, color = Reloj.tintaSuave)
    }
}

/** Sin datos todavía: el reloj necesita que Agenda en el teléfono le pase el horario una vez. */
@Composable
fun PantallaSinDatos() {
    Column(
        Modifier.fillMaxSize().background(Reloj.fondo).padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sin horario", style = Tipo.titulo, color = Reloj.tinta)
        Text("Abre Agenda en el teléfono para traerlo.", style = Tipo.apoyo, color = Reloj.tintaSuave)
    }
}

/** Nada más hoy ni mañana. */
@Composable
fun PantallaLibre(dia: WatchDay, ahora: LocalDateTime) {
    Column(
        Modifier.fillMaxSize().background(Reloj.fondo).padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nada fijo por ahora", style = Tipo.titulo, color = Reloj.tinta)
        Text(pieDatos(dia, ahora), style = Tipo.apoyo, color = Reloj.tintaSuave)
    }
}

/** Botón del reloj: pastilla de 48 dp de alto. El principal lleva el color de la urgencia; los demás, superficie. */
@Composable
fun BotonReloj(texto: String, principal: Boolean = false, color: Color = Reloj.hora, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth(if (principal) 0.62f else 0.9f)
            .heightIn(min = Reloj.toque)
            .clip(RoundedCornerShape(50))
            .background(if (principal) color else Reloj.superficie)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = Tipo.apoyo.copy(fontWeight = Tipo.hora.fontWeight), color = if (principal) Reloj.sobreHora else Reloj.tinta,
            modifier = Modifier.padding(horizontal = 12.dp), maxLines = 1)
    }
}
