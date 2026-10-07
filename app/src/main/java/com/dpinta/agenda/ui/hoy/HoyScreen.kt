package com.dpinta.agenda.ui.hoy

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dpinta.agenda.ui.components.BotonIcono
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.EstadoPrimerUso
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.components.banda.AccionBanda
import com.dpinta.agenda.ui.components.boleto.BoletoSalida
import com.dpinta.agenda.ui.components.banda.BandaSalidaMuestras
import com.dpinta.agenda.ui.components.banda.EstadoBanda
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.permisos.FranjaSinAvisos
import com.dpinta.agenda.ui.permisos.PantallaPermisoAvisos
import com.dpinta.agenda.ui.permisos.rememberPermisoAvisos
import com.dpinta.agenda.ui.theme.FormaPuntoAhora
import com.dpinta.agenda.ui.theme.Icono
import com.dpinta.agenda.ui.theme.IconoAgenda
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Etiquetas estables para pruebas y capturas. */
object EtiquetasHoy {
    const val LISTA = "hoy-lista"
    const val AHORA = "hoy-ahora"
}

private val ESPANOL: Locale = Locale.forLanguageTag("es")
private val FORMATO_DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL)
private val FORMATO_DIA_CORTO = DateTimeFormatter.ofPattern("EEEE", ESPANOL)

@Composable
fun HoyRuta(onAjustes: () -> Unit, onCrear: () -> Unit, onEditar: (Long) -> Unit, viewModel: HoyViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val permiso = rememberPermisoAvisos(hayActividades = estado is HoyUiState.Dia)
    if (permiso.explicar) {
        PantallaPermisoAvisos(onActivar = permiso.activar, onAhoraNo = permiso.ahoraNo)
        return
    }
    HoyPantalla(
        estado = estado,
        sinAvisos = !permiso.concedido,
        onActivarAvisos = permiso.activar,
        onAccion = viewModel::onAccion,
        onCambiarModo = viewModel::onCambiarModo,
        onAjustes = onAjustes,
        onCrear = onCrear,
        onEditar = onEditar,
    )
}

/** Pantalla Hoy sin estado: banda arriba (fija) y la lista del día debajo. */
@Composable
fun HoyPantalla(
    estado: HoyUiState,
    onAccion: (AccionBanda) -> Unit,
    onCambiarModo: () -> Unit,
    onAjustes: () -> Unit,
    onCrear: () -> Unit,
    modifier: Modifier = Modifier,
    onEditar: (Long) -> Unit = {},
    sinAvisos: Boolean = false,
    onActivarAvisos: () -> Unit = {},
) {
    val c = AgendaTheme.colores
    Column(modifier.fillMaxSize().background(c.papel)) {
        // La franja ocupa la barra de estado; lo de debajo ya no la reserva.
        val franja = sinAvisos && estado is HoyUiState.Dia
        if (franja) FranjaSinAvisos(onActivar = onActivarAvisos)
        when (estado) {
            HoyUiState.Cargando -> {
                Cabecera("Hoy", accion = { BotonAjustes(onAjustes) })
                EsqueletoCarga()
            }
            HoyUiState.PrimerUso -> {
                Cabecera("Hoy", accion = { BotonAjustes(onAjustes) })
                EstadoPrimerUso(onAnadir = onCrear)
            }
            is HoyUiState.Dia -> {
                // Sistema nuevo (PantallaHoy): fecha y título, el boleto de salida y la línea del día, en un solo desplazamiento.
                ListaDelDia(estado, onEditar, bajoBarraDeEstado = !franja, onAjustes = onAjustes) {
                    val enCurso = estado.enCurso
                    if (enCurso != null) {
                        TarjetaAhora(enCurso, estado.ahora, onEditar = { onEditar(enCurso.actividadId) }, modifier = Modifier.padding(horizontal = 16.dp))
                    } else {
                        estado.banda?.let {
                            BoletoSalida(it, onAccion = onAccion, onCambiarModo = onCambiarModo, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonAjustes(onAjustes: () -> Unit) =
    BotonIcono(Icono.Ajustes, "Ajustes", AgendaTheme.colores.tinta, onClick = onAjustes)

@Composable
private fun ListaDelDia(
    estado: HoyUiState.Dia,
    onEditar: (Long) -> Unit,
    bajoBarraDeEstado: Boolean,
    onAjustes: () -> Unit,
    boleto: @Composable () -> Unit,
) {
    val es24h = rememberEs24h()
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    // La sesión en curso (si hay) se resalta como AHORA; el marcador va justo después de ella.
    val enCurso = estado.filas.indexOfFirst { !it.pasada && !it.inicio.isAfter(estado.ahora) && it.fin.isAfter(estado.ahora) }
    if (bajoBarraDeEstado) Spacer(Modifier.fillMaxWidth().background(ds.fondo).statusBarsPadding())
    LazyColumn(Modifier.fillMaxSize().testTag(EtiquetasHoy.LISTA)) {
        item(key = "cabecera") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(FORMATO_DIA.format(estado.fecha), style = t.apoyo, color = ds.tintaSuave)
                    Text(saludo(estado.ahora), style = t.titulo, color = ds.tinta, modifier = Modifier.semantics { heading() })
                }
                BotonAjustes(onAjustes)
            }
        }
        item(key = "boleto") { boleto() }
        item(key = "tu-dia") {
            Text("Tu día", style = t.encabezado, color = ds.tinta,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp).semantics { heading() })
        }
        if (estado.filas.isEmpty()) {
            item(key = "vacio") { DiaSinNada(estado.siguiente) }
        }
        itemsIndexed(estado.filas, key = { _, f -> f.clave }) { i, fila ->
            if (enCurso < 0 && i == estado.indiceAhora) MarcadorAhora(estado.ahora, es24h)
            EstacionDelDia(
                fila = fila,
                enCurso = i == enCurso,
                antesDeAhora = i < estado.indiceAhora && i != enCurso,
                primera = i == 0,
                ultima = i == estado.filas.lastIndex,
                es24h = es24h,
                onEditar = { onEditar(fila.actividadId) },
            )
            if (i == enCurso) MarcadorAhora(estado.ahora, es24h)
        }
        if (enCurso < 0 && estado.filas.isNotEmpty() && estado.indiceAhora >= estado.filas.size) {
            item(key = "ahora-final") { MarcadorAhora(estado.ahora, es24h) }
        }
        item(key = "pie") { Spacer(Modifier.height(AgendaSpacing.s24 * 3)) }
    }
}

/** Saludo según la hora (PantallaHoy: «Buenos días, Diego»; sin nombre porque la app no tiene cuentas). */
private fun saludo(ahora: LocalTime): String = when (ahora.hour) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

/**
 * Fila del día (C4.4, C6): columna fija de horas con el hilo del día (relleno hasta ahora,
 * punteado después), marcador de tipo, título, salón a la derecha y conflicto inline (C10).
 */
@Composable
private fun FilaDelDia(fila: FilaHoy, antesDeAhora: Boolean, es24h: Boolean, onEditar: () -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula
    // Enmienda 2026-09-23: todo en tinta (AAA, C9.1); lo pasado lo dice la marca «✓» del hilo (C6).
    val tinta = c.tinta
    val conflicto = fila.conflicto?.let { textoConflicto(it, es24h) }
    val inicio = formatearHora(fila.inicio, es24h)
    val fin = formatearHora(fila.fin, es24h)
    val tipoHablado = when (fila.tipo) {
        TipoFila.Clase -> "clase"
        TipoFila.Trabajo -> "trabajo"
        TipoFila.Puntual -> "puntual"
    }
    val frase = buildString {
        append("${inicio.enLinea} a ${fin.enLinea}, ${fila.titulo}, $tipoHablado")
        if (fila.salon.isNotBlank()) append(", salón ${fila.salon}")
        if (fila.lugar.isNotBlank()) append(", ${fila.lugar}")
        if (fila.pasada) append(", terminada")
        conflicto?.let { append(". Conflicto: $it") }
    }
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
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = AgendaSpacing.s64)) {
            ColumnaHoras(antesDeAhora, terminada = fila.pasada) {
                Text(inicio.cifra, style = t.filaHora, color = tinta, modifier = Modifier.padding(top = AgendaSpacing.s12))
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = AgendaSpacing.s12, bottom = AgendaSpacing.s12, end = m.margen),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarcadorTipo(fila.tipo)
                    Spacer(Modifier.width(AgendaSpacing.s8))
                    Text(fila.titulo, style = t.cuerpo, color = tinta, maxLines = 2, modifier = Modifier.weight(1f))
                    if (fila.salon.isNotBlank()) {
                        Spacer(Modifier.width(AgendaSpacing.s12))
                        Text(fila.salon, style = t.filaHora, color = tinta)
                    }
                }
                val detalle = listOf("hasta ${fin.enLinea}", fila.lugar).filter { it.isNotBlank() }.joinToString(" · ")
                // Lleva la hora de fin: en tinta, AAA (C9.1, enmienda 2026-09-23).
                Text(detalle, style = t.meta, color = c.tinta)
                conflicto?.let { texto ->
                    Spacer(Modifier.height(AgendaSpacing.s4))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoAgenda(Icono.Aviso, c.alertaTexto, tamano = AgendaMedidas.iconoEnLinea)
                        Spacer(Modifier.width(AgendaSpacing.s4))
                        Text(texto, style = t.meta, color = c.alertaTexto)
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = AgendaMedidas.filete,
            color = c.filete,
            modifier = Modifier.padding(start = m.columnaHoras),
        )
    }
}

/**
 * Columna fija de 64 dp (C4.2) con el hilo del día (C6): relleno antes de ahora, punteado
 * después. Si la sesión terminó, la marca «✓» se dibuja sobre el hilo a la altura de la hora.
 */
@Composable
private fun ColumnaHoras(antesDeAhora: Boolean, terminada: Boolean, contenido: @Composable () -> Unit) {
    val c = AgendaTheme.colores
    Box(
        Modifier
            .width(AgendaTheme.reticula.columnaHoras)
            .fillMaxHeight()
            .drawBehind {
                hilo(this, if (antesDeAhora) Tramo.Relleno else Tramo.Punteado, c.tinta, c.tinta2)
                if (terminada) marcaTerminada(this, c.tinta, c.papel)
            }
            .padding(start = AgendaSpacing.s16),
    ) { contenido() }
}

/**
 * Marca «✓» de sesión terminada (C6, enmienda 2026-09-23). Dibujada, no es un glifo: dos trazos
 * rectos de 2 dp (el grosor del hilo), extremos rectos como el resto del repertorio de radio 0.
 * Ocupa 12 × 12 dp centrada en el hilo, a la altura de la hora (12 dp de relleno + media línea
 * de fila-hora); un recuadro de papel corta el hilo para que la marca se lea sola.
 */
private fun marcaTerminada(scope: DrawScope, tinta: Color, papel: Color) = with(scope) {
    val lado = AgendaSpacing.s12.toPx()
    val cx = AgendaSpacing.s8.toPx()
    val cy = (AgendaSpacing.s12 + AgendaSpacing.s12).toPx()
    val izq = cx - lado / 2
    val arr = cy - lado / 2
    drawRect(papel, topLeft = Offset(izq, arr), size = androidx.compose.ui.geometry.Size(lado, lado))
    val grosor = AgendaMedidas.hilo.toPx()
    val a = Offset(izq + lado * 0.10f, arr + lado * 0.55f)
    val b = Offset(izq + lado * 0.40f, arr + lado * 0.85f)
    val d = Offset(izq + lado * 0.92f, arr + lado * 0.18f)
    drawLine(tinta, a, b, grosor, cap = StrokeCap.Square)
    drawLine(tinta, b, d, grosor, cap = StrokeCap.Square)
}

private enum class Tramo { Relleno, Punteado, Mitad }

/** El hilo va a 8 dp del borde de la columna de horas. */
private fun hilo(scope: DrawScope, tramo: Tramo, tinta: Color, tinta2: Color) = with(scope) {
    val x = AgendaSpacing.s8.toPx()
    val grosor = AgendaMedidas.hilo.toPx()
    // Punteado de 4 dp (s4 de la escala C4.1).
    val paso = AgendaSpacing.s4.toPx()
    val punteado = PathEffect.dashPathEffect(floatArrayOf(paso, paso))
    when (tramo) {
        Tramo.Relleno -> drawLine(tinta, Offset(x, 0f), Offset(x, size.height), grosor)
        Tramo.Punteado -> drawLine(tinta2, Offset(x, 0f), Offset(x, size.height), grosor, pathEffect = punteado)
        Tramo.Mitad -> {
            drawLine(tinta, Offset(x, 0f), Offset(x, size.height / 2), grosor)
            drawLine(tinta2, Offset(x, size.height / 2), Offset(x, size.height), grosor, pathEffect = punteado)
        }
    }
}

/** Línea de «ahora» (C6): punto de 8 dp sobre el hilo + hora escrita + filete de tinta. */
@Composable
private fun LineaAhora(ahora: LocalTime, es24h: Boolean) {
    val c = AgendaTheme.colores
    val hora = formatearHora(ahora, es24h)
    Row(
        Modifier
            .fillMaxWidth()
            .height(AgendaSpacing.s32)
            .testTag(EtiquetasHoy.AHORA)
            .semantics { contentDescription = "Ahora, ${hora.enLinea}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(AgendaTheme.reticula.columnaHoras)
                .fillMaxHeight()
                .drawBehind { hilo(this, Tramo.Mitad, c.tinta, c.tinta2) },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(start = AgendaSpacing.s4)
                    .size(AgendaMedidas.puntoAhora)
                    .background(c.tinta, FormaPuntoAhora),
            )
        }
        Text("ahora ${hora.enLinea}", style = AgendaTheme.tipo.filaHora, color = c.tinta)
        Spacer(Modifier.width(AgendaSpacing.s8))
        HorizontalDivider(
            thickness = AgendaMedidas.filete,
            color = c.tinta,
            modifier = Modifier.weight(1f).padding(end = AgendaTheme.reticula.margen),
        )
    }
}

/** Marcador de tipo = estación del sistema (círculo universidad, cuadrado trabajo, triángulo entrega), 12 dp, color de su línea. */
@Composable
internal fun MarcadorTipo(tipo: TipoFila) {
    val color = AgendaTheme.ds.lineaDe(tipo)
    androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
        val w = size.width
        when (tipo) {
            TipoFila.Clase -> drawCircle(color, w / 2)
            TipoFila.Trabajo -> drawRect(color)
            TipoFila.Puntual -> drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(w / 2, 0f); lineTo(w, w); lineTo(0f, w); close() }, color)
        }
    }
}

/** C10 «día sin nada». */
@Composable
private fun DiaSinNada(siguiente: SiguienteDia?) {
    val c = AgendaTheme.colores
    val es24h = rememberEs24h()
    Column(Modifier.fillMaxWidth().padding(horizontal = AgendaTheme.reticula.margen, vertical = AgendaSpacing.s24)) {
        Text("Hoy no tienes nada fijo.", style = AgendaTheme.tipo.cuerpo, color = c.tinta)
        siguiente?.let { s ->
            val partes = listOf(FORMATO_DIA_CORTO.format(s.fecha), formatearHora(s.hora, es24h).enLinea, s.titulo, s.salon)
                .filter { it.isNotBlank() }
            Text("Siguiente: ${partes.joinToString(" · ")}", style = AgendaTheme.tipo.cuerpo, color = c.tinta2)
        }
    }
}

/** C10 «cargando»: esqueleto de filas en `papel-2`, sin spinner ni brillo. */
@Composable
internal fun EsqueletoCarga(descripcion: String = "Cargando el día") {
    val c = AgendaTheme.colores
    Column(Modifier.fillMaxWidth().semantics { contentDescription = descripcion }) {
        repeat(4) {
            Row(Modifier.fillMaxWidth().height(AgendaSpacing.s64), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.padding(start = AgendaSpacing.s16).width(AgendaSpacing.s32).height(AgendaSpacing.s16).background(c.papel2))
                Spacer(Modifier.width(AgendaSpacing.s16))
                Box(Modifier.weight(1f).height(AgendaSpacing.s16).padding(end = AgendaTheme.reticula.margen).background(c.papel2))
            }
            HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
        }
    }
}

// ---------- Previews ----------

@Preview(name = "claro", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "oscuro", widthDp = 360, heightDp = 800, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class PreviewHoy

private val filasMuestra = listOf(
    FilaHoy("1", 1, LocalTime.of(8, 0), LocalTime.of(10, 0), "Cálculo diferencial", TipoFila.Clase, "B-204", "Campus", true, null),
    FilaHoy("2", 2, LocalTime.of(10, 30), LocalTime.of(12, 0), "Física mecánica", TipoFila.Clase, "L-3", "Campus", false, null),
    FilaHoy("3", 3, LocalTime.of(12, 25), LocalTime.of(18, 0), "Turno", TipoFila.Trabajo, "Caja 2", "Tienda centro", false, ConflictoFila.Traslado(25, 35)),
    FilaHoy("4", 4, LocalTime.of(21, 0), LocalTime.of(21, 30), "Entrega informe", TipoFila.Puntual, "", "", false, null),
)

@PreviewHoy @Composable
private fun PreviewHoyDia() = AgendaTheme {
    HoyPantalla(
        HoyUiState.Dia(LocalDate.of(2026, 9, 23), LocalTime.of(10, 5), BandaSalidaMuestras.para(EstadoBanda.SinTraslado), filasMuestra, 1, null),
        onAccion = {}, onCambiarModo = {}, onAjustes = {}, onCrear = {},
    )
}

@PreviewHoy @Composable
private fun PreviewHoyCargando() = AgendaTheme {
    HoyPantalla(HoyUiState.Cargando, onAccion = {}, onCambiarModo = {}, onAjustes = {}, onCrear = {})
}

@PreviewHoy @Composable
private fun PreviewHoyPrimerUso() = AgendaTheme {
    HoyPantalla(HoyUiState.PrimerUso, onAccion = {}, onCambiarModo = {}, onAjustes = {}, onCrear = {})
}

