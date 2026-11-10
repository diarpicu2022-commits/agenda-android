package com.dpinta.agenda.ui.hoy

import com.dpinta.agenda.ui.components.Lengua
import com.dpinta.agenda.ui.components.lengua
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Formas
import java.time.Duration
import java.time.LocalTime

private fun String.inseparable() = replace(' ', ' ')

internal fun cuenta(min: Long): String = when {
    min < 60 -> "$min min"
    min % 60 == 0L -> "${min / 60} h"
    else -> "${min / 60} h ${min % 60} min"
}

/**
 * Tarjeta AHORA (NextActivityCard «ahora» de PantallaHoy): la sesión en curso se queda en primer plano mientras dura.
 * Lámina `hora-suave`, rótulo AHORA en pastilla `hora`, ficha del tipo, nombre, rango en `hora-xl`, lugar y
 * «Termina en 9 min», barra de avance en `hora` y la línea «Después: …» para saber qué viene.
 */
@Composable
internal fun TarjetaAhora(e: EnCurso, ahora: LocalTime, onEditar: () -> Unit, modifier: Modifier = Modifier) {
    val l = lengua()
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val es24h = rememberEs24h()
    val inicio = formatearHora(e.inicio, es24h, l)
    val fin = formatearHora(e.fin, es24h, l)
    val total = Duration.between(e.inicio, e.fin).toMinutes().coerceAtLeast(1)
    val queda = Duration.between(ahora, e.fin).toMinutes().coerceAtLeast(0)
    val avance = (1f - queda.toFloat() / total).coerceIn(0f, 1f)
    val tipo = when (e.tipo) {
        TipoFila.Clase -> l("Clase", "Class")
        TipoFila.Trabajo -> l("Trabajo", "Work")
        TipoFila.Puntual -> l("Entrega", "Due")
    }
    val despues = e.despues?.let { d ->
        listOf(l("Después: ", "Next: ") + formatearHora(d.hora, es24h, l).enLinea, d.titulo, d.salon).filter { it.isNotBlank() }.joinToString(" · ")
    }
    val frase = buildString {
        append(l("Ahora: ${e.titulo}, ${inicio.enLinea} a ${fin.enLinea}", "Now: ${e.titulo}, ${inicio.enLinea} to ${fin.enLinea}"))
        if (e.salon.isNotBlank()) append(l(", salón ", ", room ") + e.salon)
        if (e.lugar.isNotBlank()) append(", ${e.lugar}")
        append(l(". Termina en ${cuenta(queda)}.", ". Ends in ${cuenta(queda)}."))
        despues?.let { append(" $it.") }
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(Formas.lamina)
            .background(ds.horaSuave)
            .clickable(role = Role.Button, onClickLabel = l("Editar", "Edit"), onClick = onEditar)
            .clearAndSetSemantics {
                contentDescription = frase
                role = Role.Button
                onClick(label = l("Editar", "Edit")) { onEditar(); true }
            }
            .padding(20.dp)
            .testTag("tarjeta-ahora"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(l("AHORA", "NOW"), style = t.etiqueta, color = ds.sobreHora,
                modifier = Modifier.clip(Formas.pastilla).background(ds.hora).padding(horizontal = 10.dp, vertical = 3.dp))
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(Formas.ficha).background(ds.superficie).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarcadorTipo(e.tipo)
                Spacer(Modifier.width(6.dp))
                Text(tipo, style = t.apoyo.copy(fontWeight = t.cuerpoFuerte.fontWeight), color = ds.tinta)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(e.titulo, style = t.titulo.copy(fontSize = t.encabezado.fontSize * 1.25f, lineHeight = t.encabezado.lineHeight * 1.25f), color = ds.tinta)
        // Rango en hora-lg; cada hora con espacios no separables para que «a. m.» nunca quede sola en otra línea.
        Text("${inicio.enLinea.inseparable()} – ${fin.enLinea.inseparable()}", style = t.horaLg, color = ds.tinta)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(listOf(e.salon, e.lugar).filter { it.isNotBlank() }.joinToString(" · "), style = t.apoyo, color = ds.tintaSuave, modifier = Modifier.weight(1f))
            Text(l("Termina en ", "Ends in ") + cuenta(queda), style = t.cuerpoFuerte, color = ds.tinta)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(Formas.pastilla).background(ds.superficie)) {
            Box(Modifier.fillMaxWidth(avance).height(6.dp).clip(Formas.pastilla).background(ds.hora))
        }
        despues?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = t.apoyo, color = ds.tintaSuave)
        }
    }
}
