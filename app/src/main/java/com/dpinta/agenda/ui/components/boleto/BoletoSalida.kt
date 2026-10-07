package com.dpinta.agenda.ui.components.boleto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dpinta.agenda.domain.Urgency
import com.dpinta.agenda.ui.components.banda.AccionBanda
import com.dpinta.agenda.ui.components.banda.BandaSalidaModelo
import com.dpinta.agenda.ui.components.banda.EstadoBanda
import com.dpinta.agenda.ui.components.banda.antiguedad
import com.dpinta.agenda.ui.components.banda.fraseTalkBack
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.theme.AgendaDs
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Formas

object EtiquetasBoleto {
    const val BOLETO = "boleto"
    const val PRIMARIA = "boleto-primaria"
    const val SECUNDARIA = "boleto-secundaria"
    const val MODO = "boleto-modo"
}

/** Nivel de urgencia de cada estado de la salida (02-urgencia.md). */
fun EstadoBanda.urgencia(): Urgency = when (this) {
    EstadoBanda.Espera -> Urgency.UPCOMING
    EstadoBanda.Preparate -> Urgency.SOON
    EstadoBanda.SalYa -> Urgency.LEAVE_NOW
    EstadoBanda.VasTarde -> Urgency.URGENT
    EstadoBanda.EnCamino -> Urgency.UPCOMING
    EstadoBanda.SinTraslado -> Urgency.UPCOMING
}

fun AgendaDs.colorUrgencia(u: Urgency): Color = when (u) {
    Urgency.CALM -> urgCalma
    Urgency.UPCOMING -> urgProxima
    Urgency.SOON -> urgPronto
    Urgency.LEAVE_NOW -> urgSalir
    Urgency.URGENT -> urgUrgente
    Urgency.MISSED -> urgPerdida
}

/** Fondo de la parte superior del boleto: el «-suave» del nivel. */
private fun AgendaDs.suaveUrgencia(u: Urgency): Color = when (u) {
    Urgency.SOON -> avisoSuave
    Urgency.LEAVE_NOW -> horaSuave
    Urgency.URGENT -> criticoSuave
    else -> rutaSuave
}

fun Urgency.palabra(): String = when (this) {
    Urgency.CALM -> "Calma"
    Urgency.UPCOMING -> "Próxima"
    Urgency.SOON -> "Pronto"
    Urgency.LEAVE_NOW -> "Salir ahora"
    Urgency.URGENT -> "Urgente"
    Urgency.MISSED -> "Perdida"
}

/** Etiqueta de la acción en el sistema nuevo (02-urgencia.md · «Acciones de aviso»). */
private fun AccionBanda.texto(): String = when (this) {
    AccionBanda.VoySaliendo -> "Ya voy"
    AccionBanda.MasCinco -> "Aplazar 5 min"
    AccionBanda.YaLlegue -> "Ya llegué"
}

/** «8:07 a. m.» nunca se parte en dos líneas en el boleto (medido con fuente 2,0). */
private fun String.inseparable() = replace(' ', ' ')

private fun cuenta(min: Long): String = when {
    min < 60 -> "$min min"
    min % 60 == 0L -> "${min / 60} h"
    else -> "${min / 60} h ${min % 60} min"
}

/**
 * Boleto de salida (DepartureCard): la única forma con muescas laterales y perforación — es un pasaje, no una tarjeta.
 * Arriba: rótulo, nivel de urgencia, la cuenta (lo más grande) y el tramo origen ···· tiempo ···· destino.
 * Abajo: qué empieza y cuándo, el viaje y el margen; la acción principal en `hora` y «Aplazar 5 min» al lado.
 */
@Composable
fun BoletoSalida(
    modelo: BandaSalidaModelo,
    onAccion: (AccionBanda) -> Unit,
    onCambiarModo: () -> Unit,
    modifier: Modifier = Modifier,
    es24h: Boolean = rememberEs24h(),
) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val u = modelo.estado.urgencia()
    val color = ds.colorUrgencia(u)
    val salida = formatearHora(modelo.horaSalida, es24h).enLinea.inseparable()
    val inicio = formatearHora(modelo.horaInicio, es24h).enLinea.inseparable()
    val llegada = formatearHora(modelo.horaLlegada, es24h).enLinea.inseparable()
    val enCamino = modelo.estado == EstadoBanda.EnCamino

    val rotulo = when (modelo.estado) {
        EstadoBanda.EnCamino -> "EN CAMINO"
        EstadoBanda.SinTraslado -> "PRÓXIMA"
        else -> "BOLETO DE SALIDA"
    }
    val (antes, cifra, despues) = when (modelo.estado) {
        EstadoBanda.Espera, EstadoBanda.Preparate -> Triple("Debes salir en", cuenta(modelo.minutosParaSalir), "Hora de salida $salida")
        EstadoBanda.SalYa -> Triple("Es hora de salir", "Sal ya", "$salida · llegas $llegada")
        EstadoBanda.VasTarde -> Triple("Vas tarde", "Sal ya", "Si sales ahora llegas $llegada")
        EstadoBanda.EnCamino -> Triple("Llegada estimada", llegada, null)
        EstadoBanda.SinTraslado -> Triple("Empieza a las", inicio, null)
    }
    val detalle = buildList {
        add("${modelo.actividad} comienza a las $inicio")
        if (modelo.salon.isNotBlank()) add(modelo.salon)
        if (modelo.hayTrayecto) {
            add("Viaje ${modelo.duracion.toMinutes()} min")
            add("Margen ${modelo.margen.toMinutes()} min")
        }
    }.joinToString(" · ")
    val viejo = modelo.hayTrayecto && modelo.datoViejo && !modelo.manual

    Column(
        modifier
            .fillMaxWidth()
            .testTag(EtiquetasBoleto.BOLETO)
            .clip(Formas.lamina)
            .background(ds.superficie)
            .border(1.dp, ds.linea, Formas.lamina),
    ) {
        // ── Parte superior: el pasaje.
        Column(
            Modifier
                .fillMaxWidth()
                .background(ds.suaveUrgencia(u))
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp)
                .semantics(mergeDescendants = true) { contentDescription = fraseTalkBack(modelo, es24h) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rotulo, style = t.etiqueta, color = ds.tintaSuave, modifier = Modifier.weight(1f).semantics { heading() })
                InsigniaUrgencia(if (enCamino) null else u, if (enCamino) "Ya saliste" else u.palabra())
            }
            Spacer(Modifier.height(10.dp))
            Text(antes, style = t.apoyo, color = ds.tintaSuave)
            Text(cifra, style = t.horaHero, color = if (enCamino || modelo.estado == EstadoBanda.SinTraslado) ds.tinta else color, maxLines = 1)
            if (despues != null) Text(despues, style = t.apoyo, color = ds.tintaSuave)
            if (modelo.hayTrayecto) {
                Spacer(Modifier.height(12.dp))
                Tramo(
                    origen = "Salida",
                    destino = modelo.lugar.ifBlank { modelo.salon },
                    centro = "${modelo.duracion.toMinutes()} min ${modelo.modo.etiqueta}",
                    onCentro = onCambiarModo,
                )
            }
        }
        Perforacion()
        // ── Parte inferior: el detalle y las acciones.
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp)) {
            Text(detalle, style = t.apoyo, color = ds.tintaSuave)
            if (viejo) {
                Spacer(Modifier.height(6.dp))
                Text("Tiempo de viaje estimado hace ${antiguedad(modelo.calculadoHace)}: puede haber cambiado.", style = t.apoyo, color = ds.aviso)
            }
            val primaria = when (modelo.estado) {
                EstadoBanda.Espera, EstadoBanda.Preparate, EstadoBanda.SalYa, EstadoBanda.VasTarde -> AccionBanda.VoySaliendo
                EstadoBanda.EnCamino -> AccionBanda.YaLlegue
                EstadoBanda.SinTraslado -> null
            }
            val secundaria = if (modelo.estado == EstadoBanda.Espera || modelo.estado == EstadoBanda.Preparate || modelo.estado == EstadoBanda.SalYa) AccionBanda.MasCinco else null
            if (primaria != null) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(Formas.pastilla)
                            .background(if (primaria == AccionBanda.YaLlegue) ds.ruta else ds.hora)
                            .clickable(role = Role.Button) { onAccion(primaria) }
                            .testTag(EtiquetasBoleto.PRIMARIA),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(primaria.texto(), style = t.cuerpoFuerte, color = if (primaria == AccionBanda.YaLlegue) ds.sobreRuta else ds.sobreHora)
                    }
                    if (secundaria != null) {
                        Box(
                            Modifier
                                .heightIn(min = 48.dp)
                                .widthIn(min = 48.dp)
                                .clip(Formas.pastilla)
                                .clickable(role = Role.Button) { onAccion(secundaria) }
                                .padding(horizontal = 12.dp)
                                .testTag(EtiquetasBoleto.SECUNDARIA),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(secundaria.texto(), style = t.cuerpoFuerte, color = ds.ruta)
                        }
                    }
                }
            }
        }
    }
}

/** Insignia del nivel: ícono de forma (nunca solo color) y palabra, en pastilla de superficie. */
@Composable
private fun InsigniaUrgencia(u: Urgency?, texto: String) {
    val ds = AgendaTheme.ds
    val color = if (u == null) ds.tintaSuave else ds.colorUrgencia(u)
    Row(
        Modifier.clip(Formas.pastilla).background(ds.superficie).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoUrgencia(u, color, Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = AgendaTheme.tipo.apoyo.copy(fontWeight = AgendaTheme.tipo.cuerpoFuerte.fontWeight), color = color)
    }
}

/** Íconos de urgencia del sistema: ○ calma · ◉ próxima · ◐ pronto · ⟶ salir · ⇉ urgente · ⊘ perdida. */
@Composable
fun IconoUrgencia(u: Urgency?, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val trazo = Stroke(w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val r = w * 0.4f
        val c = Offset(w / 2, w / 2)
        when (u) {
            null, Urgency.CALM -> drawCircle(color, r, c, style = trazo)
            Urgency.UPCOMING -> { drawCircle(color, r, c, style = trazo); drawCircle(color, r * 0.4f, c) }
            Urgency.SOON -> { drawCircle(color, r, c, style = trazo); drawArc(color, 90f, 180f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2)) }
            Urgency.LEAVE_NOW -> drawPath(Path().apply { moveTo(w * 0.1f, w / 2); lineTo(w * 0.85f, w / 2); moveTo(w * 0.55f, w * 0.22f); lineTo(w * 0.88f, w / 2); lineTo(w * 0.55f, w * 0.78f) }, color, style = trazo)
            Urgency.URGENT -> drawPath(Path().apply {
                moveTo(w * 0.08f, w * 0.22f); lineTo(w * 0.42f, w / 2); lineTo(w * 0.08f, w * 0.78f)
                moveTo(w * 0.5f, w * 0.22f); lineTo(w * 0.86f, w / 2); lineTo(w * 0.5f, w * 0.78f)
            }, color, style = trazo)
            Urgency.MISSED -> { drawCircle(color, r, c, style = trazo); drawLine(color, Offset(w * 0.22f, w * 0.78f), Offset(w * 0.78f, w * 0.22f), trazo.width, StrokeCap.Round) }
        }
    }
}

/** Tramo del boleto: «Salida ········ 23 min en bus ········ Campus». El centro cambia el modo de transporte. */
@Composable
private fun Tramo(origen: String, destino: String, centro: String, onCentro: () -> Unit) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    // Con letra grande no caben en una fila: el tiempo arriba y «Salida ···· destino» debajo (medido con fuente 2,0).
    if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) {
        Column {
            Text(centro, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.ruta,
                modifier = Modifier.heightIn(min = 48.dp).clip(Formas.ficha).clickable(role = Role.Button, onClickLabel = "Cambiar el modo de transporte", onClick = onCentro)
                    .padding(vertical = 10.dp).testTag(EtiquetasBoleto.MODO))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(origen, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.tinta, maxLines = 1)
                Puntos(Modifier.weight(1f).padding(horizontal = 8.dp))
                Text(destino, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.tinta, maxLines = 2)
            }
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(origen, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.tinta, maxLines = 1)
        Puntos(Modifier.weight(1f).padding(horizontal = 8.dp))
        Text(
            centro, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.ruta, maxLines = 1,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(Formas.ficha)
                .clickable(role = Role.Button, onClickLabel = "Cambiar el modo de transporte", onClick = onCentro)
                .padding(horizontal = 4.dp, vertical = 14.dp)
                .testTag(EtiquetasBoleto.MODO),
        )
        Puntos(Modifier.weight(1f).padding(horizontal = 8.dp))
        Text(destino, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp))
    }
}

/** Tramo discontinuo (stroke-tramo): los traslados son tramos punteados en la línea del día. */
@Composable
private fun Puntos(modifier: Modifier) {
    val color = AgendaTheme.ds.tinta
    Canvas(modifier.height(4.dp)) {
        drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3.dp.toPx(),
            StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(0.1f, 9.dp.toPx())))
    }
}

/** Perforación con muescas laterales: semicírculos del color de fondo y una línea discontinua entre ellos. */
@Composable
private fun Perforacion() {
    val ds = AgendaTheme.ds
    Box(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .drawBehind {
                val r = 7.dp.toPx()
                drawLine(ds.lineaFuerte, Offset(r * 2, size.height / 2), Offset(size.width - r * 2, size.height / 2), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
                drawCircle(ds.fondo, r, Offset(0f, size.height / 2))
                drawCircle(ds.fondo, r, Offset(size.width, size.height / 2))
                drawCircle(ds.linea, r, Offset(0f, size.height / 2), style = Stroke(1.dp.toPx()))
                drawCircle(ds.linea, r, Offset(size.width, size.height / 2), style = Stroke(1.dp.toPx()))
            },
    )
}
