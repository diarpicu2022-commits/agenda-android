package com.dpinta.agenda.ui.semana

import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dpinta.agenda.ui.components.BotonIcono
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.EstadoPrimerUso
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.hoy.EsqueletoCarga
import com.dpinta.agenda.ui.hoy.MarcadorTipo
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.Icono
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ESPANOL: Locale = Locale.forLanguageTag("es")



@Composable
fun SemanaRuta(onCrear: () -> Unit, onEditar: (Long) -> Unit, viewModel: SemanaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    SemanaPantalla(
        estado = estado,
        onCrear = onCrear,
        onEditar = onEditar,
        onAnterior = viewModel::anterior,
        onSiguiente = viewModel::siguiente,
        onEstaSemana = viewModel::estaSemana,
    )
}

@Composable
fun SemanaPantalla(
    estado: SemanaUiState,
    onCrear: () -> Unit,
    onEditar: (Long) -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onEstaSemana: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AgendaTheme.colores
    Column(modifier.fillMaxSize().background(c.papel)) {
        when (estado) {
            SemanaUiState.Cargando -> {
                Cabecera("Semana")
                EsqueletoCarga("Cargando la semana")
            }
            SemanaUiState.PrimerUso -> {
                Cabecera("Semana")
                EstadoPrimerUso(onAnadir = onCrear)
            }
            is SemanaUiState.Semana -> {
                // ScreenHeader del sistema: rótulo de contexto en `apoyo` y título en `titulo`; flechas a la derecha.
                val ds = AgendaTheme.ds
                Row(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(titulo(estado), style = AgendaTheme.tipo.apoyo, color = ds.tintaSuave)
                        Text("Semana", style = AgendaTheme.tipo.titulo, color = ds.tinta, modifier = Modifier.semantics { heading() })
                    }
                    BotonIcono(Icono.Atras, "Semana anterior", ds.tinta, onClick = onAnterior)
                    // Sin glifo propio de «adelante»: la misma flecha, reflejada.
                    BotonIcono(Icono.Atras, "Semana siguiente", ds.tinta, Modifier.graphicsLayer(scaleX = -1f), onClick = onSiguiente)
                }
                SemanaSistema(estado, onEditar, onEstaSemana)
            }
        }
    }
}

/** «Semana 8 · 21–27 sep» dentro de un semestre; «28 sep – 4 oct» fuera de él. */
private fun titulo(s: SemanaUiState.Semana): String {
    val domingo = s.lunes.plusDays(6)
    val mes = { d: java.time.LocalDate -> d.month.getDisplayName(TextStyle.SHORT, ESPANOL).removeSuffix(".") }
    val rango = if (s.lunes.month == domingo.month) {
        "${s.lunes.dayOfMonth}–${domingo.dayOfMonth} ${mes(domingo)}"
    } else {
        "${s.lunes.dayOfMonth} ${mes(s.lunes)} – ${domingo.dayOfMonth} ${mes(domingo)}"
    }
    return s.numero?.let { "Semana $it · $rango" } ?: rango
}

