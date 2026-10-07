package com.dpinta.agenda.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.theme.Formas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import com.dpinta.agenda.ui.components.pulsacion
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Icono
import com.dpinta.agenda.ui.theme.IconoAgenda

object EtiquetasBarra {
    const val BARRA = "barra-inferior"
    const val CREAR = "boton-crear"
    fun pestana(p: Pestana) = "pestana-${p.name.lowercase()}"
}

/**
 * Barra inferior (AppNav del sistema Agenda, enmienda 2026-10-07): Hoy · Semana · Actividades + Crear.
 * - Fondo `superficie` con `linea` arriba.
 * - Pestaña activa: pastilla `ruta-suave` detrás del ícono y etiqueta en `ruta`; inactivas en `tinta-suave`.
 * - Crear: círculo `ruta` con «+» en `sobre-ruta`.
 * - Toques ≥ 48 dp. Sin animación al cambiar de pestaña.
 */
@Composable
fun BarraInferior(actual: Pestana?, onPestana: (Pestana) -> Unit, onCrear: () -> Unit, modifier: Modifier = Modifier) {
    val ds = AgendaTheme.ds
    Row(
        modifier
            .fillMaxWidth()
            .background(ds.superficie)
            .drawBehind { drawLine(ds.linea, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .heightIn(min = AgendaSpacing.s64)
            .padding(end = AgendaSpacing.s12)
            .testTag(EtiquetasBarra.BARRA),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).selectableGroup()) {
            Pestana.entries.forEach { p ->
                ItemBarra(p, activa = p == actual, onClick = { onPestana(p) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.width(AgendaSpacing.s12))
        BotonCrear(onCrear)
    }
}

@Composable
private fun ItemBarra(p: Pestana, activa: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val ds = AgendaTheme.ds
    val tinta = if (activa) ds.ruta else ds.tintaSuave
    val interaccion = remember { MutableInteractionSource() }
    Column(
        modifier
            .heightIn(min = AgendaSpacing.s64)
            .anilloFoco(interaccion, ds.ruta)
            .selectable(
                selected = activa,
                interactionSource = interaccion,
                indication = ripple(color = ds.ruta),
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = AgendaSpacing.s8, horizontal = AgendaSpacing.s4)
            .testTag(EtiquetasBarra.pestana(p)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .clip(Formas.pastilla)
                .background(if (activa) ds.rutaSuave else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 18.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) { IconoAgenda(p.icono, tinta) }
        Spacer(Modifier.height(AgendaSpacing.s4))
        EtiquetaAjustable(p.etiqueta, AgendaTheme.tipo.apoyo.copy(fontWeight = if (activa) AgendaTheme.tipo.cuerpoFuerte.fontWeight else AgendaTheme.tipo.apoyo.fontWeight), tinta)
    }
}

@Composable
private fun BotonCrear(onCrear: () -> Unit) {
    val ds = AgendaTheme.ds
    val interaccion = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(AgendaMedidas.botonCrear)
            .anilloFoco(interaccion, ds.ruta)
            .pulsacion(interaccion)
            .clip(Formas.estacion)
            .background(ds.ruta)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = ds.sobreRuta),
                role = Role.Button,
                onClick = onCrear,
            )
            .semantics { contentDescription = "Crear actividad" }
            .testTag(EtiquetasBarra.CREAR),
        contentAlignment = Alignment.Center,
    ) {
        IconoAgenda(Icono.Anadir, ds.sobreRuta)
    }
}
