package com.dpinta.agenda.ui.hoy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.theme.AgendaDs
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Formas
import java.time.LocalTime

/** Ancho de la columna de horas y posición del riel (01-linea-del-dia.md; size-carril 56). */
private val COLUMNA_HORA = 64.dp
private val CARRIL = 28.dp
private val ESTACION = 14.dp

/** Color de línea de cada tipo (README · «Cada categoría es una línea de transporte con su color y su forma»). */
internal fun AgendaDs.lineaDe(tipo: TipoFila): Color = when (tipo) {
    TipoFila.Clase -> lineaUniversidad
    TipoFila.Trabajo -> lineaTrabajo
    TipoFila.Puntual -> lineaEntrega
}

/**
 * Estación de la línea del día (TimelineItem): hora a la izquierda, riel con la estación (círculo = universidad,
 * cuadrado = trabajo, triángulo = entrega; nunca solo color) y el bloque con nombre, rango, aula y lugar.
 * La sesión en curso va en un bloque `hora-suave` con el rótulo AHORA. Lo terminado lleva ✓ y se apaga.
 */
@Composable
internal fun EstacionDelDia(
    fila: FilaHoy,
    enCurso: Boolean,
    antesDeAhora: Boolean,
    primera: Boolean,
    ultima: Boolean,
    es24h: Boolean,
    onEditar: () -> Unit,
) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val inicio = formatearHora(fila.inicio, es24h)
    val fin = formatearHora(fila.fin, es24h)
    val conflicto = fila.conflicto?.let { textoConflicto(it, es24h) }
    val color = ds.lineaDe(fila.tipo)
    val tipoHablado = when (fila.tipo) {
        TipoFila.Clase -> "clase"
        TipoFila.Trabajo -> "trabajo"
        TipoFila.Puntual -> "entrega"
    }
    val frase = buildString {
        if (enCurso) append("Ahora. ")
        append("${inicio.enLinea} a ${fin.enLinea}, ${fila.titulo}, $tipoHablado")
        if (fila.salon.isNotBlank()) append(", salón ${fila.salon}")
        if (fila.lugar.isNotBlank()) append(", ${fila.lugar}")
        if (fila.pasada) append(", completada")
        conflicto?.let { append(". Conflicto: $it") }
    }
    val interaccion = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 64.dp)
            .anilloFoco(interaccion, ds.ruta)
            .clickable(interactionSource = interaccion, indication = ripple(color = ds.ruta), onClickLabel = "Editar", role = Role.Button, onClick = onEditar)
            .clearAndSetSemantics {
                contentDescription = frase
                role = Role.Button
                onClick(label = "Editar") { onEditar(); true }
            },
    ) {
        // Hora (hora-lg en tabulares; apagada si ya pasó).
        Box(Modifier.width(COLUMNA_HORA).padding(start = 16.dp, top = 14.dp)) {
            Text(inicio.cifra, style = t.cuerpoFuerte.copy(fontFamily = t.horaLg.fontFamily, fontFeatureSettings = "tnum"),
                color = if (fila.pasada) ds.tintaSuave else ds.tinta)
        }
        // Riel + estación.
        Box(
            Modifier
                .width(CARRIL)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val y = 14.dp.toPx() + ESTACION.toPx() / 2 + 2.dp.toPx()
                    val riel = 4.dp.toPx() // stroke-riel
                    val hecho = if (antesDeAhora || enCurso) color else ds.linea
                    if (!primera) drawLine(if (antesDeAhora) ds.linea else hecho, Offset(x, 0f), Offset(x, y), riel)
                    if (!ultima) {
                        val efecto = if (antesDeAhora || enCurso) null else PathEffect.dashPathEffect(floatArrayOf(0.1f, 8.dp.toPx()))
                        drawLine(if (enCurso) color else ds.lineaFuerte, Offset(x, y), Offset(x, size.height), if (efecto == null) riel else 3.dp.toPx(), StrokeCap.Round, efecto)
                    }
                    val r = ESTACION.toPx() / 2
                    val c = Offset(x, y)
                    if (fila.pasada) {
                        drawCircle(ds.superficieFuerte, r, c)
                        val p = Path().apply { moveTo(c.x - r * 0.45f, c.y); lineTo(c.x - r * 0.1f, c.y + r * 0.38f); lineTo(c.x + r * 0.5f, c.y - r * 0.4f) }
                        drawPath(p, ds.tintaSuave, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    } else when (fila.tipo) {
                        TipoFila.Clase -> {
                            if (enCurso) drawCircle(ds.hora, r + 4.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                            drawCircle(color, r, c)
                        }
                        TipoFila.Trabajo -> drawRect(color, Offset(c.x - r, c.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2))
                        TipoFila.Puntual -> drawPath(Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r, c.y + r); lineTo(c.x - r, c.y + r); close() }, color)
                    }
                },
        )
        // Bloque.
        Column(
            Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)
                .then(if (enCurso) Modifier.clip(Formas.bloque).background(ds.horaSuave).padding(horizontal = 14.dp, vertical = 10.dp) else Modifier.padding(top = 6.dp)),
        ) {
            if (enCurso) {
                Text("AHORA", style = t.etiqueta, color = ds.sobreHora,
                    modifier = Modifier.clip(Formas.pastilla).background(ds.hora).padding(horizontal = 8.dp, vertical = 2.dp))
                Spacer(Modifier.height(6.dp))
            } else if (fila.tipo != TipoFila.Clase) {
                Text(if (fila.tipo == TipoFila.Trabajo) "TURNO" else "ENTREGA", style = t.etiqueta, color = color)
            }
            Text(fila.titulo, style = t.cuerpoFuerte, color = if (fila.pasada) ds.tintaSuave else ds.tinta, maxLines = 2)
            val detalle = listOf("${inicio.enLinea} – ${fin.enLinea}", fila.salon, fila.lugar).filter { it.isNotBlank() }.joinToString(" · ")
            Text(detalle, style = t.apoyo, color = ds.tintaSuave)
            if (fila.pasada) Text("✓ Completada", style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.tintaSuave)
            conflicto?.let {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(14.dp)) {
                        val w = size.width
                        drawPath(Path().apply { moveTo(w / 2, w * 0.08f); lineTo(w * 0.95f, w * 0.9f); lineTo(w * 0.05f, w * 0.9f); close() }, ds.critico, style = Stroke(1.6.dp.toPx()))
                        drawLine(ds.critico, Offset(w / 2, w * 0.38f), Offset(w / 2, w * 0.62f), 1.6.dp.toPx(), StrokeCap.Round)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = t.apoyo, color = ds.critico)
                }
            }
        }
    }
}

/** Marcador Ahora (NowMarker): pastilla «● Ahora 09:12» en `hora` y una línea que cruza el día. */
@Composable
internal fun MarcadorAhora(ahora: LocalTime, es24h: Boolean) {
    val ds = AgendaTheme.ds
    val hora = formatearHora(ahora, es24h)
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .testTag(EtiquetasHoy.AHORA)
            .semantics { contentDescription = "Ahora, ${hora.enLinea}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.padding(start = 10.dp).clip(Formas.pastilla).background(ds.hora).padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(ds.sobreHora, Formas.estacion))
            Spacer(Modifier.width(6.dp))
            Text("Ahora ${hora.enLinea}", style = AgendaTheme.tipo.etiqueta.copy(letterSpacing = AgendaTheme.tipo.cuerpo.letterSpacing), color = ds.sobreHora)
        }
        Box(Modifier.weight(1f).height(2.dp).background(ds.hora))
    }
}
