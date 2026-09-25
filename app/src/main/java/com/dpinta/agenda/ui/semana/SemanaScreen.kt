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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
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

/** C4.6: franjas de 48 dp por hora. */
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

@Composable
private fun Semana(s: SemanaUiState.Semana, onEditar: (Long) -> Unit, onEstaSemana: () -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    if (s.ahora == null) {
        Row(Modifier.padding(horizontal = m)) { BotonSubrayado("Volver a esta semana", tinta = c.tinta, onClick = onEstaSemana) }
    }
    TiraDias(s)
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
    Rejilla(s, onEditar)
}

/** Tira de días: el de hoy invertido (C6), los demás sin caja. */
@Composable
private fun TiraDias(s: SemanaUiState.Semana) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    Row(Modifier.fillMaxWidth().padding(start = AgendaTheme.reticula.margen + COLUMNA_HORAS, end = AgendaTheme.reticula.margen)) {
        for (d in s.dias) {
            val fondo = if (d.esHoy) c.tinta else c.papel
            val tinta = if (d.esHoy) c.papel else c.tinta
            val nombre = d.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, ESPANOL).removeSuffix(".")
            Column(
                Modifier
                    .weight(1f)
                    .background(fondo)
                    .padding(horizontal = AgendaSpacing.s4, vertical = AgendaSpacing.s4)
                    .clearAndSetSemantics {
                        contentDescription = DIA_LARGO.format(d.fecha) + if (d.esHoy) ", hoy" else ""
                    },
            ) {
                Text(nombre, style = t.meta, color = tinta, maxLines = 1)
                Text(d.fecha.dayOfMonth.toString(), style = t.cuerpo, color = tinta)
            }
        }
    }
}

@Composable
private fun Rejilla(s: SemanaUiState.Semana, onEditar: (Long) -> Unit) {
    val c = AgendaTheme.colores
    val es24h = rememberEs24h()
    val horas = s.hasta - s.desde
    Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().height(FRANJA * horas).padding(horizontal = AgendaTheme.reticula.margen)) {
            // Columna de horas: la cifra en la parte alta de cada franja.
            Column(Modifier.width(COLUMNA_HORAS)) {
                for (h in s.desde until s.hasta) {
                    val cifra = if (es24h || h % 12 != 0) (if (es24h) h else h % 12).toString() else "12"
                    Text(cifra, style = AgendaTheme.tipo.celda, color = c.tinta, modifier = Modifier.height(FRANJA).clearAndSetSemantics { })
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                val columna = maxWidth / 7
                // Filete de 1,5 dp al inicio de cada hora (C6: separar filas).
                for (h in 0 until horas) {
                    HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete, modifier = Modifier.offset(y = FRANJA * h))
                }
                for (b in s.bloques) Celda(b, s.desde, columna, onEditar)
                s.ahora?.let { ahora ->
                    val hoy = s.dias.indexOfFirst { it.esHoy }
                    val y = FRANJA * ((ahora.hour - s.desde) + ahora.minute / 60f)
                    if (hoy >= 0 && ahora.hour >= s.desde && ahora.hour < s.hasta) LineaAhora(columna * hoy, y, columna, formatearHora(ahora, es24h).cifra)
                }
            }
        }
    }
}

@Composable
private fun Celda(b: Bloque, desde: Int, columna: Dp, onEditar: (Long) -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val es24h = rememberEs24h()
    val arriba = FRANJA * ((b.inicio.hour - desde) + b.inicio.minute / 60f)
    val minutos = (b.fin.toSecondOfDay() - b.inicio.toSecondOfDay()) / 60f
    // C9.2: nunca menos de 48 dp tocables, aunque la sesión dure menos de una hora.
    val alto = maxOf(FRANJA * (minutos / 60f), AgendaMedidas.toque)
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
