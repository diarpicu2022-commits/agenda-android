package com.dpinta.agenda.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.dpinta.agenda.ui.theme.AgendaMedidas

/**
 * Anillo de foco (C9.4): 2 dp de grosor, separado 2 dp del control, radio 0.
 * El color es la tinta de la superficie donde está el control (tinta sobre papel,
 * tinta de la banda sobre la banda), para que el contraste sea el de su texto.
 * Usar el MISMO [interaccion] que el clickable/selectable del control.
 */
@Composable
internal fun Modifier.anilloFoco(interaccion: MutableInteractionSource, color: Color): Modifier {
    val enfocado by interaccion.collectIsFocusedAsState()
    return drawWithContent {
        drawContent()
        if (enfocado) {
            val grosor = AgendaMedidas.anilloFoco.toPx()
            val separacion = AgendaMedidas.anilloFoco.toPx()
            val desfase = separacion + grosor / 2
            drawRect(
                color = color,
                topLeft = Offset(-desfase, -desfase),
                size = Size(size.width + 2 * desfase, size.height + 2 * desfase),
                style = Stroke(width = grosor),
            )
        }
    }
}
