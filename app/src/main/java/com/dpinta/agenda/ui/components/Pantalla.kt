package com.dpinta.agenda.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Icono

/**
 * Cabecera de pantalla: título de sección (C2.3 `seccion`), volver opcional a la izquierda y
 * una acción opcional a la derecha. Respeta la barra de estado cuando es lo primero de la pantalla.
 */
@Composable
internal fun Cabecera(
    titulo: String,
    modifier: Modifier = Modifier,
    bajoBarraDeEstado: Boolean = true,
    onAtras: (() -> Unit)? = null,
    accion: (@Composable () -> Unit)? = null,
) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (bajoBarraDeEstado) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
                .heightIn(min = AgendaSpacing.s64)
                .padding(start = if (onAtras != null) AgendaSpacing.s8 else m, end = if (accion != null) AgendaSpacing.s8 else m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onAtras?.let {
                BotonIcono(Icono.Atras, "Volver", c.tinta, onClick = it)
                Spacer(Modifier.width(AgendaSpacing.s8))
            }
            Text(
                titulo,
                style = AgendaTheme.tipo.seccion,
                color = c.tinta,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            accion?.invoke()
        }
        HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
    }
}

/** Texto de apoyo bajo una cabecera, en `meta` y `tinta-2`. */
@Composable
internal fun NotaPantalla(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = AgendaTheme.tipo.meta,
        color = AgendaTheme.colores.tinta2,
        modifier = modifier.padding(horizontal = AgendaTheme.reticula.margen, vertical = AgendaSpacing.s16),
    )
}

/**
 * C10 «vacío, primer uso»: sin banda. «Empieza por tu horario de clases» + «Añadir clase»
 * (primaria, a la derecha, C1.2) y «Añadir turno» (secundaria, a la izquierda).
 */
@Composable
internal fun EstadoPrimerUso(onAnadir: () -> Unit, modifier: Modifier = Modifier) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    Column(modifier.fillMaxWidth().padding(horizontal = m, vertical = AgendaSpacing.s32)) {
        Text("Empieza por tu horario de clases", style = AgendaTheme.tipo.seccion, color = c.tinta)
        Spacer(Modifier.height(AgendaSpacing.s8))
        Text(
            "Cada clase y cada turno llevan su lugar y su salón.",
            style = AgendaTheme.tipo.cuerpo,
            color = c.tinta2,
        )
        Spacer(Modifier.height(AgendaSpacing.s24))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BotonSubrayado("Añadir turno", tinta = c.tinta, onClick = onAnadir)
            BotonRelleno("Añadir clase", relleno = c.tinta, tinta = c.papel, onClick = onAnadir)
        }
    }
}
