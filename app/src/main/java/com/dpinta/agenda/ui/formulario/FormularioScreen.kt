package com.dpinta.agenda.ui.formulario

import com.dpinta.agenda.ui.theme.Formas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.ui.components.BotonRelleno
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.CampoTexto
import com.dpinta.agenda.ui.components.MensajeError
import com.dpinta.agenda.ui.components.NotaPantalla
import com.dpinta.agenda.ui.components.Opcion
import com.dpinta.agenda.ui.components.Segmentado
import com.dpinta.agenda.ui.components.SelectorDias
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Icono
import java.time.DayOfWeek
import java.time.LocalDate

object EtiquetasFormulario {
    const val GUARDAR = "formulario-guardar"
    const val RESUMEN = "formulario-resumen"
    const val CONFLICTOS = "formulario-conflictos"
    const val MAPA = "formulario-mapa"
}

@Composable
fun FormularioRuta(
    onCerrar: () -> Unit,
    onMapa: () -> Unit,
    viewModel: FormularioViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.fase) { if (estado.fase == FaseGuardado.Guardado) onCerrar() }
    FormularioPantalla(
        estado = estado,
        acciones = AccionesFormulario(
            onCambio = viewModel::onCambio,
            onTipo = viewModel::onTipo,
            onSemanal = viewModel::onSemanal,
            onDia = viewModel::onDia,
            onModo = viewModel::onModo,
            onGuardar = viewModel::onGuardar,
            onCancelar = onCerrar,
            onMapa = onMapa,
        ),
    )
}

/** Callbacks de la pantalla (state hoisting). */
data class AccionesFormulario(
    val onCambio: ((Formulario) -> Formulario) -> Unit,
    val onTipo: (ActivityKind) -> Unit,
    val onSemanal: (Boolean) -> Unit,
    val onDia: (DayOfWeek) -> Unit,
    val onModo: (TransportMode) -> Unit,
    val onGuardar: () -> Unit,
    val onCancelar: () -> Unit,
    val onMapa: () -> Unit,
)

private val TIPOS = listOf(
    Opcion(ActivityKind.CLASE, "Clase"),
    Opcion(ActivityKind.TRABAJO, "Trabajo"),
    Opcion(ActivityKind.PUNTUAL, "Puntual"),
    Opcion(ActivityKind.EXAMEN, "Examen"),
    Opcion(ActivityKind.OTRO, "Otro"),
)

private val MODOS = listOf(
    Opcion(TransportMode.TRANSPORTE_PUBLICO, "Bus", Icono.Bus),
    Opcion(TransportMode.A_PIE, "A pie", Icono.APie),
    Opcion(TransportMode.CARRO, "Carro", Icono.Carro),
    Opcion(TransportMode.MOTO, "Moto", Icono.Moto),
)

private val LETRAS = listOf(
    DayOfWeek.MONDAY to "L", DayOfWeek.TUESDAY to "M", DayOfWeek.WEDNESDAY to "X", DayOfWeek.THURSDAY to "J",
    DayOfWeek.FRIDAY to "V", DayOfWeek.SATURDAY to "S", DayOfWeek.SUNDAY to "D",
)

/**
 * Crear o editar una actividad, en el orden del flujo del anexo §7: qué es, nombre, cuándo,
 * dónde (lugar y salón por separado), cómo llegas; conflictos en la misma pantalla y
 * frase-resumen al pie. Guardar a la derecha, cancelar a la izquierda (C1.2), fijos abajo.
 */
@Composable
fun FormularioPantalla(estado: FormularioUiState, acciones: AccionesFormulario, modifier: Modifier = Modifier) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    val f = estado.formulario
    val e = estado.errores
    val es24h = rememberEs24h()
    val hora = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
    val fecha = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
    val numero = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)

    Column(modifier.fillMaxSize().background(c.papel).imePadding()) {
        Cabecera(if (estado.esNueva) "Nueva actividad" else "Editar actividad", onAtras = acciones.onCancelar)
        when {
            estado.cargando -> {
                NotaPantalla("Cargando la actividad…")
                return@Column
            }
            estado.noEncontrada -> {
                NotaPantalla("Esta actividad ya no existe. Puede que se haya borrado.")
                BotonSubrayado("Volver", tinta = c.tinta, modifier = Modifier.padding(start = m), onClick = acciones.onCancelar)
                return@Column
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = m),
        ) {
            Seccion("¿Qué es?")
            FichasTipo(f.tipo, acciones.onTipo)
            Spacer(Modifier.height(AgendaSpacing.s16))
            CampoTexto(
                "Nombre",
                f.titulo,
                { v -> acciones.onCambio { it.copy(titulo = v) } },
                e[Campo.TITULO],
                ayuda = "Cálculo diferencial",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )

            Seccion("Cuándo")
            Segmentado(
                listOf(Opcion(true, "Cada semana"), Opcion(false, "Una vez")),
                f.semanal,
                acciones.onSemanal,
                etiquetaPrueba = "cuando",
            )
            Spacer(Modifier.height(AgendaSpacing.s16))
            if (f.semanal) {
                Text("Días", style = AgendaTheme.tipo.meta, color = c.tinta)
                Spacer(Modifier.height(AgendaSpacing.s4))
                SelectorDias(LETRAS, f.dias, ::nombreDia, acciones.onDia)
                e[Campo.DIAS]?.let { MensajeError(it) }
            } else {
                CampoTexto("Fecha", f.fecha, { v -> acciones.onCambio { it.copy(fecha = v) } }, e[Campo.FECHA], ayuda = "24/9/2026", teclado = fecha)
            }
            Spacer(Modifier.height(AgendaSpacing.s16))
            Pareja(
                { CampoTexto("Empieza", f.inicio, { v -> acciones.onCambio { it.copy(inicio = v) } }, e[Campo.INICIO], it, ayuda = "8:00", teclado = hora) },
                { CampoTexto("Termina", f.fin, { v -> acciones.onCambio { it.copy(fin = v) } }, e[Campo.FIN], it, ayuda = "10:00", teclado = hora) },
            )
            if (f.semanal) {
                Spacer(Modifier.height(AgendaSpacing.s16))
                Pareja(
                    { CampoTexto("Desde", f.desde, { v -> acciones.onCambio { it.copy(desde = v) } }, e[Campo.DESDE], it, teclado = fecha) },
                    { CampoTexto("Hasta", f.hasta, { v -> acciones.onCambio { it.copy(hasta = v) } }, e[Campo.HASTA], it, teclado = fecha) },
                )
            }

            Seccion("Dónde")
            CampoTexto(
                "Lugar",
                f.lugar,
                { v -> acciones.onCambio { it.copy(lugar = v) } },
                null,
                ayuda = "Campus",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )
            BotonSubrayado("Elegir en el mapa", tinta = c.tinta, modifier = Modifier.testTag(EtiquetasFormulario.MAPA), onClick = acciones.onMapa)
            CampoTexto(
                "Salón",
                f.salon,
                { v -> acciones.onCambio { it.copy(salon = v) } },
                null,
                ayuda = "B-204, piso 3, oficina 12",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
            )

            Seccion("Cómo llegas")
            Segmentado(MODOS, f.modo, acciones.onModo, columnas = 2, etiquetaPrueba = "modo")
            Spacer(Modifier.height(AgendaSpacing.s16))
            Pareja(
                { CampoTexto("Margen (min)", f.margen, { v -> acciones.onCambio { it.copy(margen = v) } }, e[Campo.MARGEN], it, teclado = numero) },
                { CampoTexto("Aviso (min antes)", f.aviso, { v -> acciones.onCambio { it.copy(aviso = v) } }, e[Campo.AVISO], it, teclado = numero) },
            )

            if (estado.conflictos.isNotEmpty()) {
                Seccion("Conflictos")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(Formas.bloque)
                        .background(AgendaTheme.ds.avisoSuave)
                        .padding(AgendaSpacing.s16)
                        .testTag(EtiquetasFormulario.CONFLICTOS)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    estado.conflictos.distinct().forEach { MensajeError(textoAviso(it, es24h)) }
                    Spacer(Modifier.height(AgendaSpacing.s4))
                    Text("Puedes guardar igual.", style = AgendaTheme.tipo.meta, color = c.tinta)
                }
            }

            Seccion("Resumen")
            Text(
                estado.borradorValido?.let { resumen(it, es24h) } ?: "Completa el nombre, los días y las horas para ver el resumen.",
                style = AgendaTheme.tipo.cuerpo,
                color = if (estado.borradorValido != null) AgendaTheme.ds.tinta else AgendaTheme.ds.tintaSuave,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Formas.bloque)
                    .background(if (estado.borradorValido != null) AgendaTheme.ds.rutaSuave else AgendaTheme.ds.superficieFuerte)
                    .padding(AgendaSpacing.s16)
                    .testTag(EtiquetasFormulario.RESUMEN)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(AgendaSpacing.s24))
        }
        PieAcciones(estado, acciones)
    }
}

/** Acciones fijas abajo, al alcance del pulgar (C1.2): cancelar a la izquierda, guardar a la derecha. */
@Composable
private fun PieAcciones(estado: FormularioUiState, acciones: AccionesFormulario) {
    val c = AgendaTheme.colores
    Column(Modifier.fillMaxWidth().background(AgendaTheme.ds.superficie).windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider(thickness = 1.dp, color = AgendaTheme.ds.linea)
        val errores = estado.errores.size
        val mensaje = when (val fase = estado.fase) {
            is FaseGuardado.Error -> fase.mensaje
            else -> if (errores > 0) (if (errores == 1) "Revisa el campo marcado." else "Revisa los $errores campos marcados.") else null
        }
        mensaje?.let {
            MensajeError(it, Modifier.padding(horizontal = AgendaTheme.reticula.margen).semantics { liveRegion = LiveRegionMode.Assertive })
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AgendaTheme.reticula.margen, vertical = AgendaSpacing.s8),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonSubrayado("Cancelar", tinta = c.tinta, onClick = acciones.onCancelar)
            val texto = when {
                estado.fase == FaseGuardado.Guardando -> "Guardando…"
                estado.fase is FaseGuardado.Error -> "Reintentar"
                estado.conflictos.isNotEmpty() -> "Guardar igual"
                else -> "Guardar"
            }
            BotonRelleno(
                texto,
                relleno = c.tinta,
                tinta = c.papel,
                modifier = Modifier.testTag(EtiquetasFormulario.GUARDAR),
                onClick = acciones.onGuardar,
            )
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Spacer(Modifier.height(AgendaSpacing.s24))
    Text(titulo, style = AgendaTheme.tipo.encabezado, color = AgendaTheme.ds.tinta, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(AgendaSpacing.s12))
}

/**
 * Tipo de actividad en fichas (Chip): cada una con la estación de su línea — ● clase, ■ trabajo, ▲ entrega,
 * ◆ examen, ○ otro. La elegida lleva el `-suave` de su línea y borde en su color.
 */
@Composable
private fun FichasTipo(actual: ActivityKind, onTipo: (ActivityKind) -> Unit) {
    val ds = AgendaTheme.ds
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AgendaSpacing.s8),
        verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s8),
    ) {
        TIPOS.forEach { o ->
            val activa = o.valor == actual
            val (linea, suave) = when (o.valor) {
                ActivityKind.CLASE -> ds.lineaUniversidad to ds.rutaSuave
                ActivityKind.TRABAJO -> ds.lineaTrabajo to ds.lineaTrabajoSuave
                ActivityKind.PUNTUAL -> ds.lineaEntrega to ds.lineaEntregaSuave
                ActivityKind.EXAMEN -> ds.lineaExamen to ds.ciruelaSuave
                ActivityKind.OTRO -> ds.lineaPersonal to ds.lineaPersonalSuave
            }
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(Formas.ficha)
                    .background(if (activa) suave else ds.superficie)
                    .border(if (activa) 1.5.dp else 1.dp, if (activa) linea else ds.linea, Formas.ficha)
                    .selectable(selected = activa, role = Role.RadioButton, onClick = { onTipo(o.valor) })
                    .padding(horizontal = 14.dp)
                    .testTag("tipo-${o.texto}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(11.dp)) {
                    val w = size.width
                    when (o.valor) {
                        ActivityKind.CLASE -> drawCircle(linea, w / 2)
                        ActivityKind.TRABAJO -> drawRect(linea)
                        ActivityKind.PUNTUAL -> drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(w / 2, 0f); lineTo(w, w); lineTo(0f, w); close() }, linea)
                        ActivityKind.EXAMEN -> drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(w / 2, 0f); lineTo(w, w / 2); lineTo(w / 2, w); lineTo(0f, w / 2); close() }, linea)
                        ActivityKind.OTRO -> drawCircle(linea, w / 2 - 1.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
                Spacer(Modifier.width(AgendaSpacing.s8))
                Text(o.texto, style = AgendaTheme.tipo.cuerpoFuerte, color = ds.tinta)
            }
        }
    }
}

/** Dos campos lado a lado, separados por el medianil de la retícula (C4.2). */
@Composable
private fun Pareja(izquierda: @Composable (Modifier) -> Unit, derecha: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        izquierda(Modifier.weight(1f))
        Spacer(Modifier.width(AgendaTheme.reticula.medianil))
        derecha(Modifier.weight(1f))
    }
}

// ---------- Previews ----------

@Preview(name = "claro", widthDp = 360, heightDp = 1800, showBackground = true)
@Preview(name = "oscuro", widthDp = 360, heightDp = 1800, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class PreviewFormulario

private val accionesVacias = AccionesFormulario({}, {}, {}, {}, {}, {}, {}, {})

@PreviewFormulario @Composable
private fun PreviewFormularioConErrores() = AgendaTheme {
    val f = Formulario.nuevo(LocalDate.of(2026, 9, 24)).copy(inicio = "25:00")
    FormularioPantalla(
        FormularioUiState(false, true, false, f, Validador.validar(f, LocalDate.of(2026, 9, 24)).errores, null, emptyList(), FaseGuardado.Editando),
        accionesVacias,
    )
}

