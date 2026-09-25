package com.dpinta.agenda.ui.actividades

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.EstadoPrimerUso
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.hoy.EsqueletoCarga
import com.dpinta.agenda.ui.hoy.MarcadorTipo
import com.dpinta.agenda.ui.hoy.TipoFila
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es"))

@Composable
fun ActividadesRuta(onCrear: () -> Unit, onEditar: (Long) -> Unit, onLugar: (Long) -> Unit, viewModel: ActividadesViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ActividadesPantalla(estado, onCrear = onCrear, onEditar = onEditar, onLugar = onLugar)
}

/** Actividades (arquitectura de información del anexo): Clases · Trabajo · Puntuales y Lugares guardados. */
@Composable
fun ActividadesPantalla(
    estado: ActividadesUiState,
    onCrear: () -> Unit,
    onEditar: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onLugar: (Long) -> Unit = {},
) {
    Column(modifier.fillMaxSize().background(AgendaTheme.colores.papel)) {
        Cabecera("Actividades")
        when (estado) {
            ActividadesUiState.Cargando -> EsqueletoCarga("Cargando actividades")
            ActividadesUiState.Vacia -> EstadoPrimerUso(onAnadir = onCrear)
            is ActividadesUiState.Lista -> Lista(estado, onEditar, onLugar)
        }
    }
}

@Composable
private fun Lista(estado: ActividadesUiState.Lista, onEditar: (Long) -> Unit, onLugar: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        for (seccion in estado.secciones) {
            item(key = "s-${seccion.grupo}") { TituloSeccion(seccion.grupo.titulo) }
            items(seccion.filas, key = { "a-${it.id}" }) { FilaActividad(it, onEditar = { onEditar(it.id) }) }
        }
        if (estado.lugares.isNotEmpty()) {
            item(key = "s-lugares") { TituloSeccion("Lugares guardados") }
            items(estado.lugares, key = { "l-${it.id}" }) { FilaLugar(it, onEditar = { onLugar(it.id) }) }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        texto,
        style = AgendaTheme.tipo.seccion,
        color = AgendaTheme.colores.tinta,
        modifier = Modifier
            .padding(start = AgendaTheme.reticula.margen, end = AgendaTheme.reticula.margen, top = AgendaSpacing.s32, bottom = AgendaSpacing.s8)
            .semantics { heading() },
    )
}

@Composable
private fun FilaActividad(fila: ActividadFila, onEditar: () -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val es24h = rememberEs24h()
    val cuando = fila.cuando.map { textoCuando(it, es24h) }
    // C2.2: el rango (con «→», que Atkinson no tiene) va en Archivo, con el estilo fila-hora.
    val rango = t.filaHora.toSpanStyle().copy(color = c.tinta)
    val detalle = buildAnnotatedString {
        cuando.forEachIndexed { i, (dias, horas) ->
            if (i > 0) append(" · ")
            append("$dias · ")
            withStyle(rango) { append(horas) }
        }
        if (fila.lugar.isNotBlank()) append(if (cuando.isEmpty()) fila.lugar else " · ${fila.lugar}")
    }
    val detalleHablado = detalle.text.replace(" → ", " a ")
    val tipo = when (fila.tipo) {
        TipoFila.Clase -> "clase"
        TipoFila.Trabajo -> "trabajo"
        TipoFila.Puntual -> "puntual"
    }
    val frase = listOf(fila.titulo, tipo, fila.salon.takeIf { it.isNotBlank() }?.let { "salón $it" }, detalleHablado)
        .filterNotNull().filter { it.isNotBlank() }.joinToString(", ")
    val interaccion = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .anilloFoco(interaccion, c.tinta)
            .clickable(interactionSource = interaccion, indication = ripple(color = c.tinta), onClickLabel = "Editar", role = Role.Button, onClick = onEditar)
            .clearAndSetSemantics {
                contentDescription = frase
                role = Role.Button
                onClick(label = "Editar") { onEditar(); true }
            },
    ) {
        Column(
            Modifier
                .heightIn(min = AgendaSpacing.s64)
                .padding(horizontal = AgendaTheme.reticula.margen, vertical = AgendaSpacing.s12),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MarcadorTipo(fila.tipo)
                Spacer(Modifier.width(AgendaSpacing.s8))
                Text(fila.titulo, style = t.cuerpo, color = c.tinta, maxLines = 2, modifier = Modifier.weight(1f))
                if (fila.salon.isNotBlank()) {
                    Spacer(Modifier.width(AgendaSpacing.s12))
                    Text(fila.salon, style = t.filaHora, color = c.tinta)
                }
            }
            // Lleva horas: en tinta, AAA (C9.1).
            if (detalle.isNotEmpty()) Text(detalle, style = t.meta, color = c.tinta)
        }
        HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
    }
}

@Composable
private fun FilaLugar(lugar: LugarFila, onEditar: () -> Unit) {
    val c = AgendaTheme.colores
    val uso = if (lugar.actividades == 1) "1 actividad" else "${lugar.actividades} actividades"
    val interaccion = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .anilloFoco(interaccion, c.tinta)
            .clickable(interactionSource = interaccion, indication = ripple(color = c.tinta), onClickLabel = "Tiempos de trayecto", role = Role.Button, onClick = onEditar)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = AgendaSpacing.s48)
                .padding(horizontal = AgendaTheme.reticula.margen, vertical = AgendaSpacing.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(lugar.nombre, style = AgendaTheme.tipo.cuerpo, color = c.tinta, modifier = Modifier.weight(1f))
            Text(uso, style = AgendaTheme.tipo.meta, color = c.tinta2)
        }
        HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
    }
}

/** («lun, mié y vie», «8:00 → 10:00 a. m.») o («vie 26 sep», «9:00 → 9:30 p. m.»); rango con flecha (referente Timepage). */
private fun textoCuando(c: Cuando, es24h: Boolean): Pair<String, String> {
    val desde = formatearHora(c.inicio, es24h)
    val hasta = formatearHora(c.fin, es24h)
    // Misma franja (a. m./p. m.): el sufijo va una vez, al final.
    val rango = if (desde.sufijo12h == hasta.sufijo12h) "${desde.cifra} → ${hasta.enLinea}" else "${desde.enLinea} → ${hasta.enLinea}"
    val cuando = when (c) {
        is Cuando.Semanal -> textoDias(c.dias)
        is Cuando.Fecha -> FORMATO_FECHA.format(c.fecha).replace(".", "")
    }
    return cuando to rango
}
