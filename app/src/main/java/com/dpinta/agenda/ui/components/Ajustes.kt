package com.dpinta.agenda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Formas

/** Nota de privacidad (PrivacyNote): «Tus datos permanecen en este dispositivo». Va arriba de Ajustes. */
@Composable
internal fun NotaPrivacidad(titulo: String, texto: String, modifier: Modifier = Modifier) {
    val ds = AgendaTheme.ds
    Row(modifier.fillMaxWidth().clip(Formas.bloque).background(ds.exitoSuave).padding(16.dp)) {
        Box(Modifier.padding(top = 7.dp).size(7.dp).background(ds.exito, Formas.estacion))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, style = AgendaTheme.tipo.cuerpoFuerte, color = ds.tinta)
            Text(texto, style = AgendaTheme.tipo.apoyo, color = ds.tintaSuave)
        }
    }
}

/** Grupo de ajustes (SettingsGroup): título en `etiqueta` y filas en una superficie con divisores `linea`. */
@Composable
internal fun GrupoAjustes(titulo: String, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val ds = AgendaTheme.ds
    Column(modifier.fillMaxWidth()) {
        Text(titulo.uppercase(), style = AgendaTheme.tipo.etiqueta, color = ds.tintaSuave,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp).semantics { heading() })
        Column(
            Modifier.fillMaxWidth().clip(Formas.bloque).background(ds.superficie).border(1.dp, ds.linea, Formas.bloque),
            content = contenido,
        )
    }
}

@Composable
internal fun DivisorAjustes() = HorizontalDivider(thickness = 1.dp, color = AgendaTheme.ds.linea, modifier = Modifier.padding(horizontal = 16.dp))

/** Fila que abre algo: etiqueta, valor actual y chevron. `peligro` pinta la etiqueta en `critico`. */
@Composable
internal fun FilaAjuste(etiqueta: String, valor: String? = null, ayuda: String? = null, peligro: Boolean = false, onClick: () -> Unit) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = t.cuerpoFuerte, color = if (peligro) ds.critico else ds.tinta)
            ayuda?.let { Text(it, style = t.apoyo, color = ds.tintaSuave) }
        }
        valor?.let { Text(it, style = t.apoyo, color = ds.tintaSuave, modifier = Modifier.padding(start = 12.dp)) }
        if (!peligro) Text("›", style = t.encabezado, color = ds.tintaSuave, modifier = Modifier.padding(start = 10.dp))
    }
}

/** Fila con interruptor (Switch): encendido = `ruta`. La descripción dice el efecto concreto. */
@Composable
internal fun FilaInterruptor(etiqueta: String, descripcion: String, activo: Boolean, onCambio: (Boolean) -> Unit) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value = activo, role = Role.Switch, onValueChange = onCambio)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = t.cuerpoFuerte, color = ds.tinta)
            Text(descripcion, style = t.apoyo, color = ds.tintaSuave)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = activo,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ds.sobreRuta, checkedTrackColor = ds.ruta, checkedBorderColor = ds.ruta,
                uncheckedThumbColor = ds.lineaFuerte, uncheckedTrackColor = ds.superficieFuerte, uncheckedBorderColor = ds.lineaFuerte,
            ),
        )
    }
}
