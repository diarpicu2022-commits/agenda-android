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
 * Barra inferior (anexo §7): Hoy · Semana · Actividades + Crear.
 * - Fondo `papel-2`: el segundo neutro es de la barra (C3).
 * - Pestaña activa invertida, papel sobre tinta (C6); inactivas en `tinta-2` (C3).
 * - Crear: cuadrado de 56 dp en tinta, abajo a la derecha (C1.2, C5.3).
 * - Toques ≥ 48 dp (C9.2). Sin animación al cambiar de pestaña (C8.3).
 */
@Composable
fun BarraInferior(actual: Pestana?, onPestana: (Pestana) -> Unit, onCrear: () -> Unit, modifier: Modifier = Modifier) {
    val c = AgendaTheme.colores
    Row(
        modifier
            .fillMaxWidth()
            .background(c.papel2)
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
    val c = AgendaTheme.colores
    val fondo = if (activa) c.tinta else c.papel2
    val tinta = if (activa) c.papel else c.tinta2
    val interaccion = remember { MutableInteractionSource() }
    Column(
        modifier
            .heightIn(min = AgendaSpacing.s64)
            .anilloFoco(interaccion, c.tinta)
            .background(fondo)
            .selectable(
                selected = activa,
                interactionSource = interaccion,
                indication = ripple(color = tinta),
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = AgendaSpacing.s8, horizontal = AgendaSpacing.s4)
            .testTag(EtiquetasBarra.pestana(p)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconoAgenda(p.icono, tinta)
        Spacer(Modifier.height(AgendaSpacing.s4))
        EtiquetaAjustable(p.etiqueta, AgendaTheme.tipo.meta, tinta)
    }
}

@Composable
private fun BotonCrear(onCrear: () -> Unit) {
    val c = AgendaTheme.colores
    val interaccion = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(AgendaMedidas.botonCrear)
            .anilloFoco(interaccion, c.tinta)
            .pulsacion(interaccion)
            .background(c.tinta)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = c.papel),
                role = Role.Button,
                onClick = onCrear,
            )
            .semantics { contentDescription = "Crear actividad" }
            .testTag(EtiquetasBarra.CREAR),
        contentAlignment = Alignment.Center,
    ) {
        IconoAgenda(Icono.Anadir, c.papel)
    }
}
