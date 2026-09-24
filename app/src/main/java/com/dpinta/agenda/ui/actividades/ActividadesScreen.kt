package com.dpinta.agenda.ui.actividades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.EstadoPrimerUso
import com.dpinta.agenda.ui.theme.AgendaTheme

/**
 * Actividades: ESQUELETO. Clases, trabajo, puntuales y lugares guardados llegan en
 * «resto de componentes». Hasta entonces, el estado vacío de C10.
 */
@Composable
fun ActividadesPantalla(onCrear: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(AgendaTheme.colores.papel)) {
        Cabecera("Actividades")
        EstadoPrimerUso(onAnadir = onCrear)
    }
}
