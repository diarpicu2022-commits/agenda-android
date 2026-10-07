package com.dpinta.agenda.ui.semana

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.hoy.ConflictoFila
import com.dpinta.agenda.ui.hoy.EstacionDelDia
import com.dpinta.agenda.ui.hoy.FilaHoy
import com.dpinta.agenda.ui.hoy.TipoFila
import com.dpinta.agenda.ui.hoy.lineaDe
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Formas
import java.time.format.TextStyle
import java.util.Locale

private val ES: Locale = Locale.forLanguageTag("es")

/**
 * Semana del sistema (PantallaSemana / WeeklySchedule): selector de días en pastillas — cada una con las estaciones de
 * ese día — y, debajo, la línea del día elegido con las mismas estaciones que Hoy. Los cruces se dicen con el aviso
 * «X y Y se cruzan de 09:00 a 10:00» (02-urgencia: medio círculo `aviso`, nunca solo color).
 */
@Composable
internal fun SemanaSistema(s: SemanaUiState.Semana, onEditar: (Long) -> Unit, onEstaSemana: () -> Unit) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val es24h = rememberEs24h()
    val hoy = s.dias.indexOfFirst { it.esHoy }
    var elegido by remember(s.lunes) { mutableStateOf(if (hoy >= 0) hoy else 0) }
    val delDia = s.bloques.filter { it.dia == elegido }.sortedBy { it.inicio }
    val ahora = s.ahora
    val filas = delDia.map { b ->
        val cruce = delDia.firstOrNull { o -> o !== b && o.inicio < b.fin && b.inicio < o.fin }
        FilaHoy(
            clave = "${b.actividadId}-${b.inicio}",
            actividadId = b.actividadId,
            inicio = b.inicio,
            fin = b.fin,
            titulo = b.titulo,
            tipo = b.tipo,
            salon = b.salon,
            lugar = "",
            pasada = elegido < hoy || (elegido == hoy && ahora != null && !b.fin.isAfter(ahora)),
            conflicto = cruce?.let { ConflictoFila.Cruce(it.titulo, it.inicio) },
        )
    }
    val cruces = delDia.flatMapIndexed { i, a -> delDia.drop(i + 1).filter { b -> b.inicio < a.fin && a.inicio < b.fin }.map { a to it } }
    val dia = s.dias[elegido]

    LazyColumn(Modifier.fillMaxSize().testTag("semana-lista")) {
        item(key = "dias") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                s.dias.forEachIndexed { i, d ->
                    PastillaDia(
                        dia = d,
                        tipos = s.bloques.filter { it.dia == i }.sortedBy { it.inicio }.map { it.tipo }.distinct(),
                        elegido = i == elegido,
                        onClick = { elegido = i },
                    )
                }
            }
        }
        if (hoy < 0) {
            item(key = "volver") {
                Text(
                    "Volver a esta semana", style = t.cuerpoFuerte, color = ds.ruta,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .heightIn(min = 48.dp)
                        .clip(Formas.pastilla)
                        .selectable(selected = false, role = Role.Button, onClick = onEstaSemana)
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                )
            }
        }
        item(key = "titulo-dia") {
            Text(
                dia.fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } + " " + dia.fecha.dayOfMonth,
                style = t.encabezado, color = ds.tinta,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            )
        }
        dia.sinClase?.let { motivo ->
            item(key = "sin-clase") {
                Text(motivo, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.ciruela,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
        for ((a, b) in cruces) {
            item(key = "cruce-${a.actividadId}-${b.actividadId}-${a.inicio}") {
                val desde = formatearHora(maxOf(a.inicio, b.inicio), es24h).enLinea
                val hasta = formatearHora(minOf(a.fin, b.fin), es24h).enLinea
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    MedioCirculo(ds.aviso, Modifier.padding(top = 3.dp).size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${a.titulo} y ${b.titulo.replaceFirstChar { it.lowercase() }} se cruzan de $desde a $hasta.",
                        style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.aviso)
                }
            }
        }
        if (filas.isEmpty()) {
            item(key = "vacio") {
                Text(
                    if (dia.fecha.dayOfWeek.value >= 6) "Sin actividades fijas. Disfruta el día." else "No tienes actividades este día.",
                    style = t.cuerpo, color = ds.tintaSuave, modifier = Modifier.padding(16.dp),
                )
            }
        }
        itemsIndexed(filas, key = { _, f -> f.clave }) { i, f ->
            EstacionDelDia(
                fila = f,
                enCurso = elegido == hoy && ahora != null && !f.inicio.isAfter(ahora) && f.fin.isAfter(ahora),
                antesDeAhora = f.pasada,
                primera = i == 0,
                ultima = i == filas.lastIndex,
                es24h = es24h,
                onEditar = { onEditar(f.actividadId) },
            )
        }
        item(key = "pie") { Spacer(Modifier.height(96.dp)) }
    }
}

/** Pastilla de día (selector de día del sistema): nombre corto, número y las estaciones del día. Hoy lleva anillo `hora`. */
@Composable
private fun PastillaDia(dia: DiaSemana, tipos: List<TipoFila>, elegido: Boolean, onClick: () -> Unit) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val nombre = dia.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, ES).removeSuffix(".").replaceFirstChar { it.uppercase() }
    val tinta = if (elegido) ds.sobreRuta else ds.tinta
    Column(
        Modifier
            .widthIn(min = 58.dp)
            .heightIn(min = 72.dp)
            .clip(Formas.lamina)
            .background(if (elegido) ds.ruta else ds.superficie)
            .border(if (dia.esHoy) 2.dp else 1.dp, if (dia.esHoy) ds.hora else ds.linea, Formas.lamina)
            .selectable(selected = elegido, role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(dia.fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES)).append(" ").append(dia.fecha.dayOfMonth)
                    if (dia.esHoy) append(", hoy")
                    append(", ").append(if (tipos.isEmpty()) "sin actividades" else "${tipos.size} tipos de actividad")
                    dia.sinClase?.let { append(", ").append(it) }
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("dia-semana-${dia.fecha}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(nombre, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = if (elegido) ds.sobreRuta else ds.tintaSuave)
        Text(dia.fecha.dayOfMonth.toString(), style = t.encabezado.copy(fontFamily = t.horaLg.fontFamily), color = tinta)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(10.dp)) {
            if (dia.sinClase != null) {
                Canvas(Modifier.size(9.dp)) { drawCircle(if (elegido) ds.sobreRuta else ds.ciruela, size.width / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())) }
            }
            tipos.take(3).forEach { tipo ->
                val c = if (elegido) ds.sobreRuta else ds.lineaDe(tipo)
                Canvas(Modifier.size(9.dp)) {
                    val w = size.width
                    when (tipo) {
                        TipoFila.Clase -> drawCircle(c, w / 2)
                        TipoFila.Trabajo -> drawRect(c, Offset.Zero, Size(w, w))
                        TipoFila.Puntual -> drawPath(Path().apply { moveTo(w / 2, 0f); lineTo(w, w); lineTo(0f, w); close() }, c)
                    }
                }
            }
        }
    }
}

/** Ícono «medio círculo» de atención (◐). */
@Composable
private fun MedioCirculo(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.width / 2
        drawCircle(color, r - 1.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
        drawArc(color, 90f, 180f, true, Offset(1.dp.toPx(), 1.dp.toPx()), Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()))
    }
}
