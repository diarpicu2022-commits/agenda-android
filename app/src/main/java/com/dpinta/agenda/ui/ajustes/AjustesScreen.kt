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
import com.dpinta.agenda.avisos.ProgramadorAvisos
import com.dpinta.agenda.data.ajustes.AjusteResumen
import com.dpinta.agenda.data.ajustes.AjustesAvisos
import com.dpinta.agenda.ui.components.CampoTexto
import com.dpinta.agenda.ui.components.Opcion
import com.dpinta.agenda.ui.components.Segmentado
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.formulario.Interprete
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
class AjustesViewModel @Inject constructor(
    repositorio: AgendaRepository,
    private val ajustes: AjustesAvisos,
    private val programador: ProgramadorAvisos,
    private val perfilLocal: com.dpinta.agenda.data.perfil.PerfilLocal,
) : ViewModel() {
    val perfil: StateFlow<com.dpinta.agenda.data.perfil.Perfil> = perfilLocal.perfil

    fun perfil(p: com.dpinta.agenda.data.perfil.Perfil) = perfilLocal.guardar(p)

    val semestres: StateFlow<List<SemestreGuardado>?> =
        repositorio.semestres().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val resumen: StateFlow<AjusteResumen> = ajustes.resumen

    fun resumen(ajuste: AjusteResumen) {
        if (ajuste == ajustes.resumen.value) return
        ajustes.guardar(ajuste)
        viewModelScope.launch { programador.reprogramar() }
    }
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
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    val perfil by viewModel.perfil.collectAsStateWithLifecycle()
    AjustesPantalla(semestres, onAtras, onSemestre, onDemoBanda, resumen = resumen, onResumen = viewModel::resumen,
        perfil = perfil, onPerfil = viewModel::perfil)
}

/** Ajustes (anexo §7). Por ahora: Semestre. En compilaciones depurables enlaza la demostración de la banda. */
@Composable
fun AjustesPantalla(
    semestres: List<SemestreGuardado>?,
    onAtras: () -> Unit,
    onSemestre: (Long?) -> Unit,
    onDemoBanda: () -> Unit,
    modifier: Modifier = Modifier,
    resumen: AjusteResumen = AjusteResumen(),
    onResumen: (AjusteResumen) -> Unit = {},
    perfil: com.dpinta.agenda.data.perfil.Perfil = com.dpinta.agenda.data.perfil.Perfil(),
    onPerfil: (com.dpinta.agenda.data.perfil.Perfil) -> Unit = {},
) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    val contexto = LocalContext.current
    val depurable = remember(contexto) { (contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    Column(modifier.fillMaxSize().background(c.papel)) {
        Cabecera("Ajustes", onAtras = onAtras)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            TuPerfil(perfil, onPerfil)
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
            ResumenMatutino(resumen, onResumen)
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

/**
 * Tu perfil (cuenta local): nombre para el saludo de Hoy y, si quieres, carrera y universidad. Se guarda solo en este
 * teléfono; se guarda al escribir.
 */
@Composable
private fun TuPerfil(perfil: com.dpinta.agenda.data.perfil.Perfil, onCambio: (com.dpinta.agenda.data.perfil.Perfil) -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    var nombre by rememberSaveable { mutableStateOf(perfil.nombre) }
    var carrera by rememberSaveable { mutableStateOf(perfil.carrera) }
    var universidad by rememberSaveable { mutableStateOf(perfil.universidad) }
    fun guardar() = onCambio(com.dpinta.agenda.data.perfil.Perfil(nombre, carrera, universidad))
    Column(Modifier.padding(horizontal = m), verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s12)) {
        Text("Tu perfil", style = t.seccion, color = c.tinta, modifier = Modifier.padding(top = AgendaSpacing.s24).semantics { heading() })
        Text("Cuenta local: estos datos se guardan solo en este teléfono, sin servidor ni copia en la nube.", style = t.meta, color = AgendaTheme.ds.tintaSuave)
        CampoTexto("Tu nombre", nombre, { nombre = it; guardar() }, error = null, ayuda = "Diego")
        CampoTexto("Carrera (opcional)", carrera, { carrera = it; guardar() }, error = null, ayuda = "Ingeniería de Software")
        CampoTexto("Universidad (opcional)", universidad, { universidad = it; guardar() }, error = null)
    }
}

/** Resumen matutino (arquitectura P2.8): encendido o apagado y a qué hora llega. */
@Composable
private fun ResumenMatutino(ajuste: AjusteResumen, onCambio: (AjusteResumen) -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    val es24h = rememberEs24h()
    // Sin clave de la hora: guardar mientras se escribe no debe reescribir el campo.
    var texto by rememberSaveable { mutableStateOf(formatearHora(ajuste.hora, es24h).enLinea) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.padding(horizontal = m), verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s12)) {
        Text(
            "Resumen matutino",
            style = t.seccion,
            color = c.tinta,
            modifier = Modifier.padding(top = AgendaSpacing.s32).semantics { heading() },
        )
        Text("Una notificación en silencio con lo del día y la primera salida.", style = t.cuerpo, color = c.tinta)
        Segmentado(
            opciones = listOf(Opcion(true, "Activado"), Opcion(false, "Apagado")),
            seleccion = ajuste.activo,
            onSeleccion = { onCambio(ajuste.copy(activo = it)) },
            etiquetaPrueba = "resumen",
        )
        if (ajuste.activo) {
            CampoTexto(
                etiqueta = "Hora",
                valor = texto,
                onCambio = { nuevo ->
                    texto = nuevo
                    val hora = Interprete.hora(nuevo)
                    error = if (hora == null && nuevo.isNotBlank()) "Escribe la hora como 6 o 6:30" else null
                    if (hora != null) onCambio(ajuste.copy(hora = hora))
                },
                error = error,
                ayuda = "6:00",
            )
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
