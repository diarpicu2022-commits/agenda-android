package com.dpinta.agenda.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaMotion
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Icono
import com.dpinta.agenda.ui.theme.IconoAgenda
import com.dpinta.agenda.ui.theme.rememberReducirMovimiento

/*
 * Vocabulario único de botones (C5.3). Mismo botón en la banda y fuera de ella; solo cambian
 * los colores de la superficie. Rectángulo, radio 0, alto mínimo 48 dp (C9.2).
 */

/** Acción primaria: relleno en la tinta de la superficie, texto en su fondo. */
@Composable
internal fun BotonRelleno(
    texto: String,
    relleno: Color,
    tinta: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = AgendaMedidas.toque)
            .anilloFoco(interaccion, relleno)
            .pulsacion(interaccion)
            .background(relleno)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = tinta),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = AgendaSpacing.s20, vertical = AgendaSpacing.s12),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = AgendaTheme.tipo.boton, color = tinta)
    }
}

/** Acción secundaria: solo texto con subrayado de 1,5 dp. */
@Composable
internal fun BotonSubrayado(
    texto: String,
    tinta: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = AgendaMedidas.toque)
            .anilloFoco(interaccion, tinta)
            .pulsacion(interaccion)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = tinta),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(end = AgendaSpacing.s16, top = AgendaSpacing.s12, bottom = AgendaSpacing.s12),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(texto, style = AgendaTheme.tipo.boton, color = tinta, modifier = Modifier.subrayado(tinta))
    }
}

/** Subrayado de 1,5 dp (C5.3): la marca de lo que se toca sin relleno. */
internal fun Modifier.subrayado(color: Color): Modifier = drawBehind {
    val grosor = AgendaMedidas.subrayado.toPx()
    val y = size.height - grosor / 2
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = grosor)
}

/** Pulsación (C8.2): escala a 0,97 y vuelta en 120 ms, solo transform. */
@Composable
internal fun Modifier.pulsacion(interaccion: MutableInteractionSource): Modifier {
    val pulsado by interaccion.collectIsPressedAsState()
    val reducir = rememberReducirMovimiento()
    val escala = remember { Animatable(1f) }
    LaunchedEffect(pulsado, reducir) {
        val destino = if (pulsado) 0.97f else 1f
        if (reducir) {
            escala.snapTo(destino)
        } else {
            escala.animateTo(destino, tween(AgendaMotion.PULSACION_MS, easing = AgendaMotion.salida))
        }
    }
    return graphicsLayer {
        scaleX = escala.value
        scaleY = escala.value
    }
}

/** Botón de solo icono: 48 dp de área táctil (C9.2), icono de 24 dp (C7.2), nombre para TalkBack. */
@Composable
internal fun BotonIcono(
    icono: Icono,
    descripcion: String,
    tinta: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(AgendaMedidas.toque)
            .anilloFoco(interaccion, tinta)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = tinta, bounded = true),
                role = Role.Button,
                onClickLabel = descripcion,
                onClick = onClick,
            )
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        IconoAgenda(icono, tinta)
    }
}
