package com.dpinta.agenda.ui.semana

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dpinta.agenda.ui.components.BotonIcono
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.EstadoPrimerUso
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.hoy.EsqueletoCarga
import com.dpinta.agenda.ui.hoy.MarcadorTipo
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Icono
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ESPANOL: Locale = Locale.forLanguageTag("es")
private val DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL)
private val DIA_CORTO = DateTimeFormatter.ofPattern("EEE d MMM", ESPANOL)

/** C4.6: franjas de 48 dp por hora (mínimo; ver [MedidasSemana]). */
private val FRANJA: Dp = AgendaSpacing.s48

/** Columna de horas de Semana: solo la cifra («7», «14»), en `celda`. */
private val COLUMNA_HORAS: Dp = AgendaSpacing.s20

@Composable
fun SemanaRuta(onCrear: () -> Unit, onEditar: (Long) -> Unit, viewModel: SemanaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    SemanaPantalla(
        estado = estado,
        onCrear = onCrear,
        onEditar = onEditar,
        onAnterior = viewModel::anterior,
        onSiguiente = viewModel::siguiente,
        onEstaSemana = viewModel::estaSemana,
    )
}

@Composable
fun SemanaPantalla(
    estado: SemanaUiState,
    onCrear: () -> Unit,
    onEditar: (Long) -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onEstaSemana: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AgendaTheme.colores
    Column(modifier.fillMaxSize().background(c.papel)) {
        when (estado) {
            SemanaUiState.Cargando -> {
                Cabecera("Semana")
                EsqueletoCarga("Cargando la semana")
            }
            SemanaUiState.PrimerUso -> {
                Cabecera("Semana")
                EstadoPrimerUso(onAnadir = onCrear)
            }
            is SemanaUiState.Semana -> {
                Cabecera(titulo(estado), accion = {
                    Row {
                        BotonIcono(Icono.Atras, "Semana anterior", c.tinta, onClick = onAnterior)
                        // Sin glifo propio de «adelante» (C7.3): la misma flecha, reflejada.
                        BotonIcono(Icono.Atras, "Semana siguiente", c.tinta, Modifier.graphicsLayer(scaleX = -1f), onClick = onSiguiente)
                    }
                })
                Semana(estado, onEditar, onEstaSemana)
            }
        }
    }
}

/** «Semana 8 · 21–27 sep» dentro de un semestre; «28 sep – 4 oct» fuera de él. */
private fun titulo(s: SemanaUiState.Semana): String {
    val domingo = s.lunes.plusDays(6)
    val mes = { d: java.time.LocalDate -> d.month.getDisplayName(TextStyle.SHORT, ESPANOL).removeSuffix(".") }
    val rango = if (s.lunes.month == domingo.month) {
        "${s.lunes.dayOfMonth}–${domingo.dayOfMonth} ${mes(domingo)}"
    } else {
        "${s.lunes.dayOfMonth} ${mes(s.lunes)} – ${domingo.dayOfMonth} ${mes(domingo)}"
    }
    return s.numero?.let { "Semana $it · $rango" } ?: rango
}

/**
 * Medidas de la rejilla según la letra del teléfono. Con la letra normal, 7 columnas iguales y
 * franjas de 48 dp (C4.6). Si el salón no cabe, la columna toma el ancho que necesita y la rejilla
 * se desliza en horizontal; si no cabe en alto, la franja crece (C2.4: nada se recorta; enmienda 2026-09-25).
 */
private data class MedidasSemana(val horas: Dp, val columna: Dp, val franja: Dp, val desliza: Boolean)

@Composable
private fun medir(s: SemanaUiState.Semana, ancho: Dp): MedidasSemana {
    val t = AgendaTheme.tipo
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    val es24h = rememberEs24h()
    fun anchoDe(texto: String, estilo: androidx.compose.ui.text.TextStyle): Dp =
        with(densidad) { medidor.measure(texto, estilo, softWrap = false, maxLines = 1).size.width.toDp() }
    val horas = maxOf(COLUMNA_HORAS, anchoDe(if (es24h) "22" else "12", t.celda) + AgendaSpacing.s4)
    val contenido = (
        s.bloques.flatMap { listOf(anchoDe(it.salon, t.celda), anchoDe(it.abreviado, t.meta)) } +
            s.dias.flatMap { listOf(anchoDe(nombreDia(it), t.meta), anchoDe(it.fecha.dayOfMonth.toString(), t.cuerpo)) } +
            AgendaMedidas.marcadorTipo
        ).max()
    // 4 dp de relleno a la izquierda y 4 dp de separación a la derecha (C4.1).
    val minima = contenido + AgendaSpacing.s8
    val disponible = ancho - AgendaTheme.reticula.margen * 2 - horas
    val lineaSalon = with(densidad) { medidor.measure("B-204", t.celda).size.height.toDp() }
    val franja = maxOf(FRANJA, AgendaSpacing.s4 + AgendaMedidas.marcadorTipo + AgendaSpacing.s4 + lineaSalon + AgendaSpacing.s4)
    return MedidasSemana(horas, maxOf(disponible / 7, minima), franja, minima * 7 > disponible)
}

private fun nombreDia(d: DiaSemana) = d.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, ESPANOL).removeSuffix(".")

@Composable
private fun Semana(s: SemanaUiState.Semana, onEditar: (Long) -> Unit, onEstaSemana: () -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val medidas = medir(s, maxWidth)
        // Una sola posición horizontal para la tira de días y la rejilla: se deslizan juntas.
        val lateral = rememberScrollState()
        // Si la rejilla se desliza, se abre con hoy a la vista (una columna antes, para dar contexto).
        val hoy = s.dias.indexOfFirst { it.esHoy }
        val desde = with(LocalDensity.current) { (medidas.columna * maxOf(hoy - 1, 0)).roundToPx() }
        LaunchedEffect(s.lunes, medidas.desliza) { if (medidas.desliza && hoy >= 0) lateral.scrollTo(desde) }
        Column {
            if (s.ahora == null) {
                Row(Modifier.padding(horizontal = m)) { BotonSubrayado("Volver a esta semana", tinta = c.tinta, onClick = onEstaSemana) }
            }
            TiraDias(s, medidas, lateral)
            for (d in s.dias.filter { it.sinClase != null }) {
                Text(
                    "${d.sinClase} · ${DIA_CORTO.format(d.fecha).replace(".", "")}",
                    style = t.meta,
                    color = c.tinta,
                    modifier = Modifier.padding(horizontal = m, vertical = AgendaSpacing.s4),
                )
            }
            if (s.bloques.isEmpty()) {
                Text("Esta semana no tienes nada fijo.", style = t.cuerpo, color = c.tinta, modifier = Modifier.padding(horizontal = m, vertical = AgendaSpacing.s12))
            }
            HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
            Rejilla(s, medidas, lateral, onEditar)
        }
    }
}

private fun Modifier.deslizable(medidas: MedidasSemana, estado: ScrollState): Modifier =
    if (medidas.desliza) horizontalScroll(estado) else this

/** Tira de días: el de hoy invertido (C6), los demás sin caja. */
@Composable
private fun TiraDias(s: SemanaUiState.Semana, medidas: MedidasSemana, lateral: ScrollState) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    Row(Modifier.fillMaxWidth().padding(start = m + medidas.horas, end = m)) {
        Row(Modifier.weight(1f).deslizable(medidas, lateral)) {
            for (d in s.dias) {
                val fondo = if (d.esHoy) c.tinta else c.papel
                val tinta = if (d.esHoy) c.papel else c.tinta
                Column(
                    Modifier
                        .width(medidas.columna)
                        .background(fondo)
                        .padding(horizontal = AgendaSpacing.s4, vertical = AgendaSpacing.s4)
                        .clearAndSetSemantics {
                            contentDescription = DIA_LARGO.format(d.fecha) + if (d.esHoy) ", hoy" else ""
                        },
                ) {
                    Text(nombreDia(d), style = t.meta, color = tinta, maxLines = 1, softWrap = false)
                    Text(d.fecha.dayOfMonth.toString(), style = t.cuerpo, color = tinta, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun Rejilla(s: SemanaUiState.Semana, medidas: MedidasSemana, lateral: ScrollState, onEditar: (Long) -> Unit) {
    val c = AgendaTheme.colores
    val es24h = rememberEs24h()
    val horas = s.hasta - s.desde
    val franja = medidas.franja
    val columna = medidas.columna
    Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().height(franja * horas).padding(horizontal = AgendaTheme.reticula.margen)) {
            // Columna de horas, fija aunque la rejilla se deslice: la cifra en la parte alta de cada franja.
            Column(Modifier.width(medidas.horas)) {
                for (h in s.desde until s.hasta) {
                    val cifra = if (es24h || h % 12 != 0) (if (es24h) h else h % 12).toString() else "12"
                    Text(
                        cifra,
                        style = AgendaTheme.tipo.celda,
                        color = c.tinta,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.height(franja).clearAndSetSemantics { },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight().deslizable(medidas, lateral)) {
                Box(Modifier.width(columna * 7).fillMaxHeight()) {
                    // Filete de 1,5 dp al inicio de cada hora (C6: separar filas).
                    for (h in 0 until horas) {
                        HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete, modifier = Modifier.offset(y = franja * h))
                    }
                    for (b in s.bloques) Celda(b, s.desde, columna, franja, onEditar)
                    s.ahora?.let { ahora ->
                        val hoy = s.dias.indexOfFirst { it.esHoy }
                        val y = franja * ((ahora.hour - s.desde) + ahora.minute / 60f)
                        if (hoy >= 0 && ahora.hour >= s.desde && ahora.hour < s.hasta) LineaAhora(columna * hoy, y, columna, formatearHora(ahora, es24h).cifra)
                    }
                }
            }
        }
    }
}

@Composable
private fun Celda(b: Bloque, desde: Int, columna: Dp, franja: Dp, onEditar: (Long) -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val es24h = rememberEs24h()
    val arriba = franja * ((b.inicio.hour - desde) + b.inicio.minute / 60f)
    val minutos = (b.fin.toSecondOfDay() - b.inicio.toSecondOfDay()) / 60f
    // C9.2: nunca menos de 48 dp tocables, aunque la sesión dure menos de una hora.
    val alto = maxOf(franja * (minutos / 60f), AgendaMedidas.toque)
    val ancho = columna / b.carriles
    val frase = listOf(
        b.fecha(),
        "${formatearHora(b.inicio, es24h).enLinea} a ${formatearHora(b.fin, es24h).enLinea}",
        b.titulo,
        b.salon.takeIf { it.isNotBlank() }?.let { "salón $it" },
    ).filterNotNull().joinToString(", ")
    val interaccion = remember { MutableInteractionSource() }
    Box(
        Modifier
            .offset(x = columna * b.dia + ancho * b.carril, y = arriba)
            .size(width = ancho, height = alto)
            // Separación entre celdas contiguas: 4 dp a la derecha y 1,5 dp abajo (el filete de la hora).
            .padding(end = AgendaSpacing.s4, bottom = AgendaMedidas.filete)
            .anilloFoco(interaccion, c.tinta)
            .background(c.papel2)
            .clickable(interactionSource = interaccion, indication = ripple(color = c.tinta), onClickLabel = "Editar", role = Role.Button) { onEditar(b.actividadId) }
            .clearAndSetSemantics {
                contentDescription = frase
                role = Role.Button
                onClick(label = "Editar") { onEditar(b.actividadId); true }
            }
            .clipToBounds()
            .padding(start = AgendaSpacing.s4, top = AgendaSpacing.s4),
    ) {
        Column {
            MarcadorTipo(b.tipo)
            Spacer(Modifier.height(AgendaSpacing.s4))
            if (b.salon.isNotBlank()) Text(b.salon, style = t.celda, color = c.tinta, maxLines = 1, softWrap = false)
            Text(b.abreviado, style = t.meta, color = c.tinta, maxLines = 1, softWrap = false)
        }
    }
}

private fun Bloque.fecha(): String =
    java.time.DayOfWeek.of(dia + 1).getDisplayName(TextStyle.FULL, ESPANOL)

/** Línea de «ahora» solo en la columna de hoy (C6): punto de 8 dp + filete + hora escrita. */
@Composable
private fun LineaAhora(x: Dp, y: Dp, ancho: Dp, hora: String) {
    val c = AgendaTheme.colores
    val punto = 8.dp // C5.4: el único círculo del sistema
    Box(Modifier.offset(x = x, y = y - punto / 2).width(ancho).heightIn(min = punto)) {
        Box(Modifier.size(punto).background(c.tinta, CircleShape))
        HorizontalDivider(thickness = AgendaMedidas.filete, color = c.tinta, modifier = Modifier.offset(y = punto / 2 - AgendaMedidas.filete / 2))
    }
    Text(
        hora,
        style = AgendaTheme.tipo.celda,
        color = c.tinta,
        modifier = Modifier
            .offset(x = x + punto + AgendaSpacing.s4, y = y - AgendaSpacing.s20)
            .background(c.papel)
            .clearAndSetSemantics { contentDescription = "ahora $hora" },
    )
}
