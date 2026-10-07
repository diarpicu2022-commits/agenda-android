package com.dpinta.agenda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Formas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.dpinta.agenda.ui.theme.Icono
import com.dpinta.agenda.ui.theme.IconoAgenda

/*
 * Controles de formulario (paso 5). Vocabulario de Material 3 con los tokens del contrato:
 * radio 0 (C5.1), sin sombras (C5.2), etiqueta encima y error debajo, en el color de alerta
 * y con icono (C3.3: nunca solo color). Toques ≥ 48 dp (C9.2).
 */

/** Campo de texto: etiqueta arriba (tinta, `meta`), rectángulo con borde, error debajo. */
@Composable
internal fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    etiquetaPrueba: String = etiqueta,
) {
    val c = AgendaTheme.colores
    val ds = AgendaTheme.ds
    Column(modifier) {
        // Etiqueta visible 14 · 600 en tinta (TextField del sistema).
        Text(etiqueta, style = AgendaTheme.tipo.apoyo.copy(fontWeight = AgendaTheme.tipo.cuerpoFuerte.fontWeight), color = c.tinta)
        Spacer(Modifier.height(AgendaSpacing.s4))
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = true,
            isError = error != null,
            textStyle = AgendaTheme.tipo.cuerpo,
            keyboardOptions = teclado,
            placeholder = ayuda?.let { { Text(it, style = AgendaTheme.tipo.cuerpo, color = c.tinta2) } },
            shape = Formas.ficha,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ds.tinta,
                unfocusedTextColor = ds.tinta,
                errorTextColor = ds.tinta,
                focusedContainerColor = ds.superficie,
                unfocusedContainerColor = ds.superficie,
                errorContainerColor = ds.superficie,
                cursorColor = ds.tinta,
                errorCursorColor = ds.tinta,
                focusedBorderColor = ds.tinta,
                unfocusedBorderColor = ds.lineaFuerte,
                errorBorderColor = ds.critico,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AgendaMedidas.toque)
                .testTag("campo-$etiquetaPrueba")
                .semantics {
                    contentDescription = etiqueta
                    if (error != null) error(error)
                },
        )
        error?.let { MensajeError(it) }
    }
}

/** Mensaje de error bajo un control: icono de aviso + texto (C3.3, C10). */
@Composable
internal fun MensajeError(texto: String, modifier: Modifier = Modifier) {
    val c = AgendaTheme.colores
    Row(modifier.padding(top = AgendaSpacing.s4), verticalAlignment = Alignment.CenterVertically) {
        IconoAgenda(Icono.Aviso, c.alertaTexto, tamano = AgendaMedidas.iconoEnLinea)
        Spacer(Modifier.width(AgendaSpacing.s4))
        Text(texto, style = AgendaTheme.tipo.meta, color = c.alertaTexto)
    }
}

/** Una opción de un selector: texto e icono opcional. */
internal data class Opcion<T>(val valor: T, val texto: String, val icono: Icono? = null)

/**
 * Selector de una sola opción. Cada opción es un rectángulo con filete; la elegida va invertida
 * (C6: la inversión papel/tinta marca lo activo). En filas de [columnas].
 */
@Composable
internal fun <T> Segmentado(
    opciones: List<Opcion<T>>,
    seleccion: T,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    columnas: Int = opciones.size,
    etiquetaPrueba: String = "segmentado",
) {
    val c = AgendaTheme.colores
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s8)) {
        opciones.chunked(columnas).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AgendaSpacing.s8)) {
                fila.forEach { o ->
                    val activa = o.valor == seleccion
                    val ds = AgendaTheme.ds
                    // Ficha del sistema: elegida en `ruta-suave` con borde `ruta`; las demás en superficie con `linea`.
                    val tinta = if (activa) ds.ruta else ds.tinta
                    val interaccion = remember { MutableInteractionSource() }
                    Row(
                        Modifier
                            .weight(1f)
                            .heightIn(min = AgendaMedidas.toque)
                            .anilloFoco(interaccion, ds.ruta)
                            .clip(Formas.ficha)
                            .border(if (activa) 1.5.dp else 1.dp, if (activa) ds.ruta else ds.linea, Formas.ficha)
                            .background(if (activa) ds.rutaSuave else ds.superficie)
                            .selectable(
                                selected = activa,
                                interactionSource = interaccion,
                                indication = ripple(color = tinta),
                                role = Role.RadioButton,
                                onClick = { onSeleccion(o.valor) },
                            )
                            .padding(horizontal = AgendaSpacing.s8, vertical = AgendaSpacing.s12)
                            .testTag("$etiquetaPrueba-${o.texto}"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        o.icono?.let {
                            IconoAgenda(it, tinta, tamano = AgendaMedidas.iconoEnLinea)
                            Spacer(Modifier.width(AgendaSpacing.s4))
                        }
                        Text(o.texto, style = AgendaTheme.tipo.boton, color = tinta, textAlign = TextAlign.Center)
                    }
                }
                // Huecos de la última fila: mismo ancho de celda.
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Días de la semana (anexo §7: chips L M X J V S D). Casilla por día, activa invertida.
 * Con 360 dp cada casilla mide ~46 dp de ancho: el área táctil se amplía a 48 dp (C9.2).
 */
@Composable
internal fun SelectorDias(
    dias: List<Pair<java.time.DayOfWeek, String>>,
    elegidos: Set<java.time.DayOfWeek>,
    nombre: (java.time.DayOfWeek) -> String,
    onDia: (java.time.DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AgendaTheme.colores
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AgendaSpacing.s4)) {
        dias.forEach { (dia, letra) ->
            val activo = dia in elegidos
            val ds = AgendaTheme.ds
            val interaccion = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .heightIn(min = AgendaMedidas.toque)
                    .anilloFoco(interaccion, ds.ruta)
                    .clip(Formas.pastilla)
                    .border(if (activo) 1.5.dp else 1.dp, if (activo) ds.ruta else ds.linea, Formas.pastilla)
                    .background(if (activo) ds.ruta else ds.superficie)
                    .toggleable(
                        value = activo,
                        interactionSource = interaccion,
                        indication = ripple(color = if (activo) ds.sobreRuta else ds.ruta),
                        role = Role.Checkbox,
                        onValueChange = { onDia(dia) },
                    )
                    .semantics { contentDescription = nombre(dia) }
                    .testTag("dia-${dia.name.lowercase()}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(letra, style = AgendaTheme.tipo.boton, color = if (activo) AgendaTheme.ds.sobreRuta else AgendaTheme.ds.tinta)
            }
        }
    }
}
