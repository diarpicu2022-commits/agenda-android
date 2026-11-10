package com.dpinta.agenda.ui.hoy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dpinta.agenda.domain.WatchAttendance
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.components.lengua
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Formas

/**
 * `MorningSummary` del sistema: lámina en `superficie-fuerte`, la cuenta del día y dos bloques en `superficie`
 * («Primera clase» y «Debes salir a las», esta en `hora`). Desaparece cuando empieza la primera actividad.
 */
@Composable
internal fun TarjetaResumen(r: ResumenManana, modifier: Modifier = Modifier) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val l = lengua()
    val es24h = rememberEs24h()
    Column(
        modifier.fillMaxWidth().clip(Formas.lamina).background(ds.superficieFuerte).padding(20.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            buildAnnotatedString {
                append(l("Hoy tienes ", "Today you have "))
                withStyle(SpanStyle(color = ds.tinta, fontWeight = t.cuerpoFuerte.fontWeight)) {
                    append(if (r.cantidad == 1) l("1 actividad", "1 activity") else l("${r.cantidad} actividades", "${r.cantidad} activities"))
                }
                append(".")
            },
            style = t.cuerpo, color = ds.tintaSuave, modifier = Modifier.semantics { heading() },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Bloque(l("Primero", "First"), formatearHora(r.primeraHora, es24h, l).enLinea, r.primeraTitulo, ds.tinta, Modifier.weight(1f))
            r.salirA?.let { Bloque(l("Debes salir a las", "Leave at"), formatearHora(it, es24h, l).enLinea, null, ds.hora, Modifier.weight(1f)) }
        }
        if (r.demas.isNotEmpty()) Text(
            l("También tienes: ", "You also have: ") + r.demas.joinToString(" · ") { (titulo, hora) -> "$titulo ${formatearHora(hora, es24h, l).enLinea}" },
            style = t.apoyo, color = ds.tintaSuave,
        )
    }
}

@Composable
private fun Bloque(rotulo: String, hora: String, detalle: String?, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    Column(modifier.clip(Formas.bloque).background(ds.superficie).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(rotulo, style = t.apoyo, color = ds.tintaSuave)
        Text(hora, style = t.horaLg, color = color, maxLines = 1)
        if (detalle != null) Text(detalle, style = t.cuerpoFuerte, color = ds.tinta, maxLines = 2)
    }
}

/**
 * Avisos de Hoy (04-patrones · Hoy, punto 6) con la forma de `ConflictNotice`: bloque en `aviso-suave`, título en
 * `aviso` y una ficha en `superficie` por materia. «Cerca del límite» a una falta; «Llegaste al límite» en él.
 */
@Composable
internal fun AvisosAsistencia(lista: List<WatchAttendance>, modifier: Modifier = Modifier) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val l = lengua()
    Column(
        modifier.fillMaxWidth().clip(Formas.bloque).background(ds.avisoSuave).padding(AgendaSpacing.s16),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(l("⚠ Asistencia", "⚠ Attendance"), style = t.cuerpoFuerte, color = ds.aviso, modifier = Modifier.semantics { heading() })
        for (a in lista) {
            val max = a.maxAbsences ?: continue
            val estado = if (a.absences >= max) l("Llegaste al límite", "You reached the limit") else l("Cerca del límite", "Close to the limit")
            Row(
                Modifier.fillMaxWidth().clip(Formas.ficha).background(ds.superficie).padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Column(Modifier.weight(1f)) {
                    Text(a.title, style = t.cuerpoFuerte, color = ds.tinta, maxLines = 1)
                    Text(estado, style = t.apoyo, color = ds.tintaSuave)
                }
                Text(l("${a.absences} de $max", "${a.absences} of $max"), style = t.horaLg, color = ds.tinta)
            }
        }
    }
}
