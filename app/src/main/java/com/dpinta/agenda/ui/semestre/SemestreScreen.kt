package com.dpinta.agenda.ui.semestre

import com.dpinta.agenda.ui.components.Mensajes
import com.dpinta.agenda.ui.components.Lengua
import com.dpinta.agenda.ui.components.lengua
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dpinta.agenda.ui.components.BotonRelleno
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.CampoTexto
import com.dpinta.agenda.ui.components.MensajeError
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


private fun LocalDate.corta(l: Lengua) =
    DateTimeFormatter.ofPattern(l("EEE d MMM", "EEE, MMM d"), Locale.forLanguageTag(l("es", "en"))).format(this).replace(".", "")

@Composable
fun SemestreRuta(onCerrar: () -> Unit, viewModel: SemestreViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(e.terminado) { if (e.terminado) onCerrar() }
    SemestrePantalla(e, viewModel, onCerrar)
}

/** Semestre: fechas y días sin clase. Los festivos de Colombia se proponen solos (Ley Emiliani). */
@Composable
fun SemestrePantalla(e: SemestreUiState, vm: SemestreViewModel, onCerrar: () -> Unit, modifier: Modifier = Modifier) {
    val l = lengua()
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    val f = e.formulario
    val fecha = KeyboardOptions(keyboardType = KeyboardType.Number)
    Column(modifier.fillMaxSize().background(c.papel).imePadding()) {
        Cabecera(if (e.editando) l("Editar semestre", "Edit semester") else l("Nuevo semestre", "New semester"), onAtras = onCerrar)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = m, vertical = AgendaSpacing.s16),
            verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s16),
        ) {
            CampoTexto(l("Nombre", "Name"), f.nombre, vm::nombre, e.errores.nombre?.let { Mensajes.de(it, l) }, ayuda = "2026-2")
            Row(horizontalArrangement = Arrangement.spacedBy(AgendaSpacing.s12)) {
                CampoTexto(l("Empieza", "Starts"), f.inicio, vm::inicio, e.errores.inicio?.let { Mensajes.de(it, l) }, Modifier.weight(1f), ayuda = "3/8", teclado = fecha)
                CampoTexto(l("Termina", "Ends"), f.fin, vm::fin, e.errores.fin?.let { Mensajes.de(it, l) }, Modifier.weight(1f), ayuda = "28/11", teclado = fecha)
            }

            Text(l("Días sin clase", "Days without class"), style = t.encabezado, color = c.tinta, modifier = Modifier.padding(top = AgendaSpacing.s16).semantics { heading() })
            if (f.dias.isEmpty()) {
                Text(
                    if (f.inicio.isBlank() || f.fin.isBlank()) l("Escribe las fechas y te propongo los festivos.", "Write the dates and I'll suggest the holidays.") else l("Ningún día sin clase.", "No days without class."),
                    style = t.cuerpo,
                    color = c.tinta2,
                )
            }
            Column {
                for ((dia, motivo) in f.dias.toSortedMap()) {
                    Row(Modifier.fillMaxWidth().heightIn(min = AgendaMedidas.toque), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(dia.corta(l), style = t.cuerpo, color = c.tinta)
                            Text(Mensajes.de(motivo, l), style = t.meta, color = c.tinta2)
                        }
                        BotonSubrayado(l("Quitar", "Remove"), tinta = c.tinta, onClick = { vm.quitar(dia) })
                    }
                    HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AgendaSpacing.s12)) {
                CampoTexto(l("Otro día", "Another day"), f.nuevoDia, vm::nuevoDia, null, Modifier.weight(1f), ayuda = "12/10", teclado = fecha)
                CampoTexto(l("Motivo", "Reason"), f.nuevoMotivo, vm::nuevoMotivo, null, Modifier.weight(1.4f), ayuda = l("Receso", "Break"))
            }
            e.errores.nuevoDia?.let { MensajeError(Mensajes.de(it, l)) }
            BotonSubrayado(l("Añadir día", "Add day"), tinta = c.tinta, onClick = vm::anadir)

            if (e.editando) {
                Spacer(Modifier.height(AgendaSpacing.s16))
                BotonSubrayado(
                    if (e.confirmarEliminar) l("Toca otra vez para eliminar", "Tap again to delete") else l("Eliminar semestre", "Delete semester"),
                    tinta = if (e.confirmarEliminar) c.alertaTexto else c.tinta,
                    onClick = vm::eliminar,
                )
            }
        }
        // C1.2: cancelar a la izquierda, guardar a la derecha, fijos abajo.
        Column(Modifier.fillMaxWidth().background(c.papel).windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = m, vertical = AgendaSpacing.s12),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BotonSubrayado(l("Cancelar", "Cancel"), tinta = c.tinta, onClick = onCerrar)
                BotonRelleno(l("Guardar", "Save"), relleno = c.tinta, tinta = c.papel, onClick = vm::guardar)
            }
        }
    }
}
