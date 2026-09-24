package com.dpinta.agenda.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.components.banda.AccionBanda
import com.dpinta.agenda.ui.components.banda.BandaSalida
import com.dpinta.agenda.ui.components.banda.BandaSalidaMuestras
import com.dpinta.agenda.ui.components.banda.EstadoBanda
import com.dpinta.agenda.ui.components.banda.ModoTransporte
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme

/**
 * Demostración de la banda con selector de estados. SOLO depuración: se llega desde Ajustes
 * únicamente si la app es depurable (ruta RutaDemoBanda).
 */
@Composable
fun DemoBandaPantalla(onAtras: () -> Unit) {
    var estado by rememberSaveable { mutableStateOf(EstadoBanda.Espera) }
    var viejo by rememberSaveable { mutableStateOf(false) }
    var modo by rememberSaveable { mutableStateOf(ModoTransporte.Bus) }
    var desplazamiento by rememberSaveable { mutableLongStateOf(0L) }

    val muestra = BandaSalidaMuestras.para(estado, viejo)
    val modelo = muestra.copy(modo = modo, horaSalida = muestra.horaSalida.plusMinutes(desplazamiento))

    Column(
        Modifier
            .fillMaxSize()
            .background(AgendaTheme.colores.papel),
    ) {
        Cabecera("Demostración de la banda", onAtras = onAtras)
        BandaSalida(
            bajoBarraDeEstado = false,
            modelo = modelo,
            onAccion = { accion ->
                when (accion) {
                    AccionBanda.VoySaliendo -> estado = EstadoBanda.EnCamino
                    AccionBanda.MasCinco -> desplazamiento += 5
                    AccionBanda.YaLlegue -> estado = EstadoBanda.Espera
                }
            },
            onCambiarModo = { modo = modo.siguiente() },
        )
        SelectorDemo(
            estado = estado,
            viejo = viejo,
            onEstado = { estado = it },
            onViejo = { viejo = !viejo },
            onMasUno = { desplazamiento += 1 },
        )
    }
}

private val nombres = mapOf(
    EstadoBanda.Espera to "Espera",
    EstadoBanda.Preparate to "Prepárate",
    EstadoBanda.SalYa to "Sal ya",
    EstadoBanda.VasTarde to "Vas tarde",
    EstadoBanda.EnCamino to "En camino",
    EstadoBanda.SinTraslado to "Sin traslado",
)

@Composable
private fun SelectorDemo(
    estado: EstadoBanda,
    viejo: Boolean,
    onEstado: (EstadoBanda) -> Unit,
    onViejo: () -> Unit,
    onMasUno: () -> Unit,
) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = m, vertical = AgendaSpacing.s24),
    ) {
        Text("Solo en depuración.", style = AgendaTheme.tipo.meta, color = c.tinta2)
        Spacer(Modifier.height(AgendaSpacing.s12))
        EstadoBanda.entries.chunked(3).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AgendaSpacing.s8)) {
                fila.forEach { e ->
                    Opcion(nombres.getValue(e), e == estado, Modifier.weight(1f)) { onEstado(e) }
                }
            }
            Spacer(Modifier.height(AgendaSpacing.s8))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AgendaSpacing.s8)) {
            Opcion("Dato viejo", viejo, Modifier.weight(1f), onViejo)
            Opcion("+1 min", false, Modifier.weight(1f), onMasUno)
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** Opción del selector: filete de 1,5 dp; la activa, invertida (C6: inversión papel/tinta). */
@Composable
private fun Opcion(texto: String, activa: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = AgendaTheme.colores
    val interaccion = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = AgendaMedidas.toque)
            .anilloFoco(interaccion, c.tinta)
            .border(AgendaMedidas.filete, c.tinta)
            .background(if (activa) c.tinta else c.papel)
            .selectable(
                selected = activa,
                interactionSource = interaccion,
                indication = ripple(color = if (activa) c.papel else c.tinta),
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = AgendaSpacing.s8, vertical = AgendaSpacing.s12),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = AgendaTheme.tipo.boton, color = if (activa) c.papel else c.tinta)
    }
}

