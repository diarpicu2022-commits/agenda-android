package com.dpinta.agenda.wear

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.dpinta.agenda.domain.WatchRhythm
import androidx.compose.foundation.shape.CircleShape
import com.dpinta.agenda.domain.WatchAttendance
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
fun PantallaProxima(s: WatchSession, ahora: LocalDateTime, zona: ZoneId, ambiente: Boolean, ritmo: WatchRhythm? = null) {
    val u = s.urgency(ahora, zona)
    val color = Reloj.urgencia(u, s.exam)
    val restante = Duration.between(ahora, s.start)
    Box(Modifier.fillMaxSize().background(Reloj.fondo), contentAlignment = Alignment.Center) {
        if (!ambiente) PaisajeGaleras(Modifier.fillMaxSize(), ahora.hour)
        val cuadro = cuadroDeCamino(ambiente, restante.toMinutes())
        AnilloCuenta(restante, s.leaveAt?.let { Duration.between(it, s.start) }, color, Modifier.fillMaxSize(), ambiente,
            kind = s.kind, mode = s.mode, urgente = u == Urgency.URGENT || u == Urgency.LEAVE_NOW || u == Urgency.MISSED, cuadro = cuadro)
        Column(
            Modifier.padding(horizontal = 30.dp).semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (s.exam) Text(t("PARCIAL", "EXAM"), style = Tipo.rotulo, color = if (ambiente) Reloj.tinta else Reloj.ciruela)
            else Text(if (u == Urgency.MISSED) t("Empezó hace", "Started") else t("Empieza en", "Starts in"), style = Tipo.apoyo, color = Reloj.tintaSuave)
            Text(
                Formato.cuenta(if (u == Urgency.MISSED) Duration.between(s.start, ahora) else restante),
                style = Tipo.hora,
                color = if (u == Urgency.MISSED) Reloj.urgencia(Urgency.MISSED) else Reloj.tinta,
                maxLines = 1,
            )
            Text(
                s.title, style = Tipo.titulo, color = Reloj.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (u == Urgency.MISSED) TextDecoration.LineThrough else null,
            )
            Text(listOf(s.room.ifBlank { s.place }, Formato.hora(s.start.toLocalTime())).filter { it.isNotBlank() }.joinToString(" · "),
                style = Tipo.apoyo, color = Reloj.aula, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!ambiente) LineaRitmo(s, ritmo)
        }
    }
}

/**
 * En curso: la sesión que está pasando se queda en la esfera mientras dura (decisión de Diego, 2026-10-07). El anillo
 * es la línea del día enrollada: se vacía hasta el final de la sesión. «AHORA» en `hora`, la cuenta hasta que termina,
 * el nombre y «aula · hasta 10:00».
 */
@Composable
fun PantallaEnCurso(s: WatchSession, ahora: LocalDateTime, ambiente: Boolean) {
    val total = Duration.between(s.start, s.end).toMinutes().coerceAtLeast(1)
    val queda = Duration.between(ahora, s.end)
    val color = if (s.exam) Reloj.ciruela else Reloj.tipo(s.kind)
    Box(Modifier.fillMaxSize().background(Reloj.fondo), contentAlignment = Alignment.Center) {
        AnilloSesion(queda.toMinutes().toFloat() / total, color, Modifier.fillMaxSize(), ambiente)
        Column(
            Modifier.padding(horizontal = 30.dp).semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(t("AHORA", "NOW"), style = Tipo.rotulo, color = if (ambiente) Reloj.tinta else Reloj.hora)
            Text(s.title, style = Tipo.titulo, color = Reloj.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t("Termina en", "Ends in"), style = Tipo.apoyo, color = Reloj.tintaSuave)
            Text(Formato.cuenta(queda), style = Tipo.hora, color = Reloj.tinta, maxLines = 1)
            Text(listOf(s.room.ifBlank { s.place }, t("hasta ", "until ") + Formato.hora(s.end.toLocalTime())).filter { it.isNotBlank() }.joinToString(" · "),
                style = Tipo.apoyo, color = Reloj.aula, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 2 · Es hora de salir (WatchLeaveNow): rótulo, destino y cuenta; «Ya voy» debajo y el resto de acciones al bajar. */
@Composable
fun PantallaSalir(s: WatchSession, ahora: LocalDateTime, zona: ZoneId, ambiente: Boolean, ritmo: WatchRhythm? = null, onAccion: (Accion) -> Unit) {
    val u = s.urgency(ahora, zona)
    val color = Reloj.urgencia(u, s.exam)
    Box(Modifier.fillMaxSize().background(Reloj.fondo), contentAlignment = Alignment.Center) {
        AnilloCuenta(Duration.between(ahora, s.start), s.leaveAt?.let { Duration.between(it, s.start) }, color, Modifier.fillMaxSize(), ambiente,
            kind = s.kind, mode = s.mode, urgente = true, cuadro = cuadroDeCamino(ambiente, Duration.between(ahora, s.start).toMinutes()))
        Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (u == Urgency.URGENT) t("SAL YA", "LEAVE NOW") else t("ES HORA DE SALIR", "TIME TO LEAVE"), style = Tipo.rotulo, color = if (ambiente) Reloj.tinta else color,
                modifier = Modifier.semantics { heading() })
            Text(s.title, style = Tipo.titulo, color = Reloj.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t("comienza en ", "starts in ") + Formato.cuenta(Duration.between(ahora, s.start)), style = Tipo.apoyo, color = Reloj.cifraApoyo)
            if (!ambiente) LineaRitmo(s, ritmo, conHora = false)
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

/**
 * «Tu ritmo» (anexo 2026-11-10-agenda-ia): lo que aprendió el teléfono de las salidas reales. Una línea de apoyo, con la
 * hora corregida; va en `cifraApoyo` porque es una hora que se lee para decidir (tinta en claro, AAA).
 */
@Composable
fun LineaRitmo(s: WatchSession, ritmo: WatchRhythm?, conHora: Boolean = true) {
    val salida = s.leaveAt ?: return
    val r = ritmo ?: return
    val hora = Formato.hora(salida.minusMinutes(r.lateMinutes.toLong()).toLocalTime())
    // Dos líneas: en una sola se cortaba justo la hora («sal 0…»). En «Es hora de salir» la hora corregida ya pasó.
    val texto = t("Sueles salir ${r.lateMinutes} min tarde", "You usually leave ${r.lateMinutes} min late") + if (conHora) t("\nsal a las $hora", "\nleave at $hora") else ""
    Text(texto, style = Tipo.apoyo, color = Reloj.cifraApoyo, maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics {
            contentDescription = t("Tu ritmo: sueles salir ${r.lateMinutes} minutos tarde.", "Your rhythm: you usually leave ${r.lateMinutes} minutes late.") + if (conHora) t(" Sal a las $hora", " Leave at $hora") else ""
        })
}

/**
 * El Galeras al fondo (anexo 2026-11-10-campuswatch-vivo, A): silueta del volcán con su cráter, en `superficieFuerte`
 * (apenas distinta del fondo, 1,11 a 1,24:1). Es paisaje, no información: el texto se mide sobre él.
 */
@Composable
fun PaisajeGaleras(modifier: Modifier = Modifier, hora: Int = 12) {
    androidx.compose.foundation.Canvas(modifier) {
        cielo(hora)
        val w = size.width; val h = size.height
        val volcan = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.86f)
            cubicTo(w * 0.18f, h * 0.84f, w * 0.30f, h * 0.76f, w * 0.40f, h * 0.70f)
            lineTo(w * 0.46f, h * 0.715f)
            lineTo(w * 0.52f, h * 0.70f)
            cubicTo(w * 0.62f, h * 0.75f, w * 0.78f, h * 0.83f, w, h * 0.85f)
            lineTo(w, h)
            close()
        }
        drawPath(volcan, Reloj.superficieFuerte)
    }
}

/** «¿Fuiste?» (W6): la clase de hoy ya terminó y no tiene marca. Repertorio de Acciones: dos botones de 48 dp. */
@Composable
fun PantallaFuiste(a: WatchAttendance, onRespuesta: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Reloj.fondo).padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Cuy(PoseCuy.PREGUNTA, Modifier.size(34.dp))
            Spacer(Modifier.width(6.dp))
            Text(t("¿Fuiste?", "Did you go?"), style = Tipo.apoyo, color = Reloj.tintaSuave, modifier = Modifier.semantics { heading() })
        }
        Text(a.title, style = Tipo.titulo, color = Reloj.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        BotonReloj(t("Sí, fui", "Yes, I went"), principal = true, color = Reloj.exito, ancho = 0.9f) { onRespuesta(true) }
        BotonReloj(t("No fui", "I didn't go"), ancho = 0.9f) { onRespuesta(false) }
    }
}

/**
 * Asistencia (W6): faltas por materia, con el repertorio del Horario (una fila por materia). La cifra de faltas es lo que
 * se lee para decidir: va en tinta; si la materia tiene límite y se está cerca o pasado, en `aviso` o `critico`.
 */
@Composable
fun PantallaAsistencia(lista: List<WatchAttendance>) {
    Column(
        Modifier.fillMaxSize().background(Reloj.fondo).padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            val enLimite = lista.any { a -> a.maxAbsences != null && a.absences >= a.maxAbsences!! }
            Cuy(if (enLimite) PoseCuy.PREOCUPADO else PoseCuy.FELIZ, Modifier.size(30.dp))
            Spacer(Modifier.width(6.dp))
            Text(t("Asistencia", "Attendance"), style = Tipo.apoyo, color = Reloj.ruta, modifier = Modifier.semantics { heading() })
        }
        if (lista.isEmpty()) {
            Text(t("Todavía no hay clases que contar", "No classes to count yet"), style = Tipo.apoyo, color = Reloj.tintaSuave, textAlign = TextAlign.Center)
        }
        for (a in lista.take(3)) {
            val faltas = if (a.absences == 1) t("1 falta", "1 absence") else t("${a.absences} faltas", "${a.absences} absences")
            val limite = a.maxAbsences?.let { t(" de $it", " of $it") }.orEmpty()
            val max = a.maxAbsences
            val color = when {
                max != null && a.absences > max -> Reloj.critico
                max != null && max - a.absences <= 1 -> Reloj.aviso
                else -> Reloj.tinta
            }
            // Un bloque centrado por materia (nombre y, debajo, las faltas): en la esfera redonda una fila de dos columnas
            // cortaba los nombres.
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 2.dp)) {
                Text(a.title, style = Tipo.apoyo, color = Reloj.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (color != Reloj.tinta) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(faltas + limite, style = Tipo.apoyo.copy(fontWeight = Tipo.titulo.fontWeight),
                        color = if (Reloj.tema == TemaReloj.CLARO) Reloj.tinta else color)
                }
            }
        }
        if (lista.size > 3) Text(t("y ${lista.size - 3} más", "and ${lista.size - 3} more"), style = Tipo.apoyo, color = Reloj.tintaSuave)
    }
}

/** Tema del reloj (anexo 2026-11-09-campuswatch-claro, W2): Claro · Oscuro · Como el teléfono, con el repertorio de
 *  Acciones (un botón de 48 dp por fila); el elegido va como principal. */
@Composable
fun PantallaTema(onElegir: (PreferenciaTema.Opcion) -> Unit) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val cambio by PreferenciaTema.cambio.collectAsState()
    val elegida = remember(cambio) { PreferenciaTema.opcion(contexto) }
    Column(
        Modifier.fillMaxSize().background(Reloj.fondo).padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(t("Tema", "Theme"), style = Tipo.apoyo, color = Reloj.tintaSuave, modifier = Modifier.semantics { heading() })
        for ((o, texto) in listOf(PreferenciaTema.Opcion.CLARO to t("Claro", "Light"), PreferenciaTema.Opcion.OSCURO to t("Oscuro", "Dark"), PreferenciaTema.Opcion.TELEFONO to t("Como el teléfono", "Same as phone"))) {
            // Mismo ancho para las tres: la elegida se distingue por el color, no cambia de tamaño (y «Como el teléfono» cabe).
            BotonReloj(texto, principal = o == elegida, color = Reloj.ruta, ancho = 0.9f) { onElegir(o) }
        }
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
            Text(t("Nada fijo", "Nothing fixed"), style = Tipo.titulo, color = Reloj.tintaSuave, modifier = Modifier.padding(vertical = 8.dp))
        }
        for (s in filas.take(4)) FilaHorario(s, s == actual, s.end.isBefore(ahora) || s.end == ahora)
        if (filas.size > 4) Text(t("y ${filas.size - 4} más", "and ${filas.size - 4} more"), style = Tipo.apoyo, color = Reloj.tintaSuave)
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
            color = if (actual) Reloj.horaFila else Reloj.cifraApoyo)
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
        Cuy(if (ok) PoseCuy.FELIZ else PoseCuy.PREOCUPADO, Modifier.size(64.dp), insignia = ok, progreso = trazo.value)
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
        Cuy(PoseCuy.DORMIDO, Modifier.size(52.dp))
        Spacer(Modifier.height(4.dp))
        Text(t("Sin horario", "No schedule"), style = Tipo.titulo, color = Reloj.tinta)
        Text(t("Abre Agenda en el teléfono para traerlo.", "Open Agenda on the phone to bring it."), style = Tipo.apoyo, color = Reloj.tintaSuave)
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
        Cuy(PoseCuy.DORMIDO, Modifier.size(52.dp))
        Spacer(Modifier.height(4.dp))
        Text(t("Nada fijo por ahora", "Nothing fixed for now"), style = Tipo.titulo, color = Reloj.tinta)
        Text(pieDatos(dia, ahora), style = Tipo.apoyo, color = Reloj.tintaSuave)
    }
}

/** Botón del reloj: pastilla de 48 dp de alto. El principal lleva el color de la urgencia; los demás, superficie. */
@Composable
fun BotonReloj(texto: String, principal: Boolean = false, color: Color = Reloj.hora, ancho: Float? = null, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth(ancho ?: if (principal) 0.62f else 0.9f)
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

/**
 * Fotograma del viajero (A+). Al encender la pantalla camina unos dos segundos (12 fotogramas de 160 ms) y se queda
 * quieto: nada de animación continua, por la batería. En siempre activo o con las animaciones del sistema apagadas no
 * se mueve; el fotograma de reposo alterna con los minutos, como antes.
 */
@Composable
fun cuadroDeCamino(ambiente: Boolean, minutos: Long): Int {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val sinAnimacion = remember {
        android.provider.Settings.Global.getFloat(contexto.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var cuadro by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var anda by remember { mutableStateOf(!ambiente && !sinAnimacion) }
    LaunchedEffect(ambiente) {
        if (ambiente || sinAnimacion) { anda = false; return@LaunchedEffect }
        anda = true
        repeat(12) { kotlinx.coroutines.delay(160); cuadro = (cuadro + 1) % 4 }
        anda = false
    }
    return if (anda) cuadro else if (minutos % 2 == 0L) 0 else 2
}

