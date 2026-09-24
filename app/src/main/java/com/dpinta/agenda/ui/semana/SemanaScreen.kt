package com.dpinta.agenda.ui.semana

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.EstadoPrimerUso
import com.dpinta.agenda.ui.theme.AgendaTheme

/**
 * Semana: ESQUELETO. La rejilla tipo horario llega en «resto de componentes».
 * Hasta entonces muestra el estado vacío de C10, sin inventar contenido.
 */
@Composable
fun SemanaPantalla(onCrear: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(AgendaTheme.colores.papel)) {
        Cabecera("Semana")
        EstadoPrimerUso(onAnadir = onCrear)
    }
}
