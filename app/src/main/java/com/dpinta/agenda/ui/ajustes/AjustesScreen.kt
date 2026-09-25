package com.dpinta.agenda.ui.ajustes

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.SemestreGuardado
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.NotaPantalla
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AjustesViewModel @Inject constructor(repositorio: AgendaRepository) : ViewModel() {
    val semestres: StateFlow<List<SemestreGuardado>?> =
        repositorio.semestres().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private val FORMATO = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))
private fun LocalDate.corta() = FORMATO.format(this).replace(".", "")

/** «del 3 ago al 28 nov · 5 días sin clase» (sin «→»: Atkinson no lo tiene, C2.2). */
internal fun resumenSemestre(s: SemestreGuardado): String {
    val dias = when (s.diasSinClase.size) {
        0 -> "sin días libres"
        1 -> "1 día sin clase"
        else -> "${s.diasSinClase.size} días sin clase"
    }
    return "del ${s.inicio.corta()} al ${s.fin.corta()} · $dias"
}

@Composable
fun AjustesRuta(
    onAtras: () -> Unit,
    onSemestre: (Long?) -> Unit,
    onDemoBanda: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val semestres by viewModel.semestres.collectAsStateWithLifecycle()
    AjustesPantalla(semestres, onAtras, onSemestre, onDemoBanda)
}

/** Ajustes (anexo §7). Por ahora: Semestre. En compilaciones depurables enlaza la demostración de la banda. */
@Composable
fun AjustesPantalla(
    semestres: List<SemestreGuardado>?,
    onAtras: () -> Unit,
    onSemestre: (Long?) -> Unit,
    onDemoBanda: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    val contexto = LocalContext.current
    val depurable = remember(contexto) { (contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    Column(modifier.fillMaxSize().background(c.papel)) {
        Cabecera("Ajustes", onAtras = onAtras)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                "Semestre",
                style = t.seccion,
                color = c.tinta,
                modifier = Modifier.padding(start = m, end = m, top = AgendaSpacing.s24, bottom = AgendaSpacing.s8).semantics { heading() },
            )
            when {
                semestres == null -> Unit
                semestres.isEmpty() -> NotaPantalla("Sin semestre, cada clase se repite 17 semanas y no se saltan festivos.")
                else -> for (s in semestres) FilaSemestre(s) { onSemestre(s.id) }
            }
            BotonSubrayado("Añadir semestre", tinta = c.tinta, modifier = Modifier.padding(start = m), onClick = { onSemestre(null) })
            if (depurable) {
                BotonSubrayado(
                    "Demostración de la banda",
                    tinta = c.tinta,
                    modifier = Modifier.padding(start = m, top = AgendaSpacing.s32),
                    onClick = onDemoBanda,
                )
            }
        }
    }
}

@Composable
private fun FilaSemestre(s: SemestreGuardado, onEditar: () -> Unit) {
    val c = AgendaTheme.colores
    val interaccion = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .anilloFoco(interaccion, c.tinta)
            .clickable(interactionSource = interaccion, indication = ripple(color = c.tinta), onClickLabel = "Editar", role = Role.Button, onClick = onEditar),
    ) {
        Column(Modifier.heightIn(min = AgendaSpacing.s64).padding(horizontal = AgendaTheme.reticula.margen, vertical = AgendaSpacing.s12)) {
            Text(s.nombre, style = AgendaTheme.tipo.cuerpo, color = c.tinta)
            Text(resumenSemestre(s), style = AgendaTheme.tipo.meta, color = c.tinta)
        }
        HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
    }
}
