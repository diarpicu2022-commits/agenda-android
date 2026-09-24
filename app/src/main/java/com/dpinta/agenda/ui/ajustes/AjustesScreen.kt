package com.dpinta.agenda.ui.ajustes

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.NotaPantalla
import com.dpinta.agenda.ui.theme.AgendaTheme

/**
 * Ajustes: ESQUELETO. Se entra desde el icono de la cabecera de Hoy (anexo §7).
 * En compilaciones depurables enlaza la demostración de la banda.
 */
@Composable
fun AjustesPantalla(onAtras: () -> Unit, onDemoBanda: () -> Unit, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val depurable = remember(contexto) { (contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    Column(modifier.fillMaxSize().background(AgendaTheme.colores.papel)) {
        Cabecera("Ajustes", onAtras = onAtras)
        NotaPantalla("Semestre, avisos, permisos, copia de seguridad y apariencia llegan en un paso posterior.")
        if (depurable) {
            BotonSubrayado(
                "Demostración de la banda",
                tinta = AgendaTheme.colores.tinta,
                modifier = Modifier.padding(start = AgendaTheme.reticula.margen),
                onClick = onDemoBanda,
            )
        }
    }
}
