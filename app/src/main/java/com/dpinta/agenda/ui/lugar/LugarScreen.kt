package com.dpinta.agenda.ui.lugar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.avisos.ProgramadorAvisos
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.ui.components.BotonRelleno
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.CampoTexto
import com.dpinta.agenda.ui.formulario.Interprete
import com.dpinta.agenda.ui.hoy.BandaMapeador
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Un trayecto de más de 5 h no es un «cuánto tardas»: es un error de tecleo. */
private val MINUTOS = 1..300

private const val ERROR = "Escribe los minutos, entre 1 y 300"

data class LugarUiState(
    val nombre: String = "",
    /** Minutos escritos por modo, tal cual. */
    val minutos: Map<TransportMode, String> = emptyMap(),
    val errores: Map<TransportMode, String> = emptyMap(),
    /** Los demás lugares guardados (id, nombre), para el traslado desde cada uno hasta este. */
    val otros: List<Pair<Long, String>> = emptyList(),
    /** Minutos escritos desde cada otro lugar, tal cual. */
    val traslados: Map<Long, String> = emptyMap(),
    val erroresTraslado: Map<Long, String> = emptyMap(),
    val terminado: Boolean = false,
)

@HiltViewModel
class LugarViewModel @Inject constructor(
    private val repositorio: AgendaRepository,
    private val programador: ProgramadorAvisos,
    private val reloj: Clock,
    estadoGuardado: SavedStateHandle,
) : ViewModel() {

    private val id: Long = estadoGuardado.get<Long>("id") ?: -1
    private val _estado = MutableStateFlow(LugarUiState())
    val estado: StateFlow<LugarUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val agenda = repositorio.agenda().first()
            val conocidos = repositorio.estimaciones(id).mapValues { it.value.duration.toMinutes().toString() }
            val otros = agenda.lugares.values.filter { it.id != id }.sortedBy { it.nombre.lowercase() }.map { it.id to it.nombre }
            val traslados = otros.mapNotNull { (otro, _) -> agenda.minutosEntre(otro, id)?.let { otro to it.toString() } }.toMap()
            _estado.update { it.copy(nombre = agenda.lugares[id]?.nombre.orEmpty(), minutos = conocidos, otros = otros, traslados = traslados) }
        }
    }

    fun minutos(modo: TransportMode, texto: String) = _estado.update {
        it.copy(minutos = it.minutos + (modo to texto.filter(Char::isDigit).take(3)), errores = it.errores - modo)
    }

    fun traslado(otro: Long, texto: String) = _estado.update {
        it.copy(traslados = it.traslados + (otro to texto.filter(Char::isDigit).take(3)), erroresTraslado = it.erroresTraslado - otro)
    }

    fun guardar() {
        val e = _estado.value
        val invalido = { t: String -> t.isNotBlank() && Interprete.minutos(t, MINUTOS) == null }
        val errores = e.minutos.filterValues(invalido).mapValues { ERROR }
        val erroresTraslado = e.traslados.filterValues(invalido).mapValues { ERROR }
        if (errores.isNotEmpty() || erroresTraslado.isNotEmpty()) {
            _estado.update { it.copy(errores = errores, erroresTraslado = erroresTraslado) }
            return
        }
        viewModelScope.launch {
            for ((modo, texto) in e.minutos) {
                val m = Interprete.minutos(texto, MINUTOS) ?: continue
                repositorio.guardarEstimacion(id, TravelEstimate(Duration.ofMinutes(m.toLong()), modo, reloj.instant(), fromCache = false, manual = true))
            }
            for ((otro, texto) in e.traslados) {
                val m = Interprete.minutos(texto, MINUTOS) ?: continue
                repositorio.guardarTraslado(otro, id, m.toLong())
            }
            // La agenda no cambia al guardar un trayecto: las alarmas de salida se rehacen aquí.
            programador.reprogramar()
            _estado.update { it.copy(terminado = true) }
        }
    }
}

@Composable
fun LugarRuta(onCerrar: () -> Unit, viewModel: LugarViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(e.terminado) { if (e.terminado) onCerrar() }
    LugarPantalla(e, onMinutos = viewModel::minutos, onTraslado = viewModel::traslado, onGuardar = viewModel::guardar, onCerrar = onCerrar)
}

/** Cuánto tardas desde casa a un lugar, por modo (C1.3: los cuatro modos, y solo cuatro). */
@Composable
fun LugarPantalla(
    e: LugarUiState,
    onMinutos: (TransportMode, String) -> Unit,
    onTraslado: (Long, String) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    Column(modifier.fillMaxSize().background(c.papel).imePadding()) {
        Cabecera(e.nombre.ifBlank { "Lugar" }, onAtras = onCerrar)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = m, vertical = AgendaSpacing.s16),
            verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s16),
        ) {
            Text(
                "Cuánto tardas desde casa. Con esto calculo a qué hora salir; sin tráfico en vivo.",
                style = AgendaTheme.tipo.cuerpo,
                color = c.tinta,
            )
            for (modo in TransportMode.entries) {
                val etiqueta = BandaMapeador.aModoUi(modo).etiqueta.replaceFirstChar { it.uppercase() }
                CampoTexto(
                    etiqueta = "$etiqueta, en minutos",
                    valor = e.minutos[modo].orEmpty(),
                    onCambio = { onMinutos(modo, it) },
                    error = e.errores[modo],
                    ayuda = "23",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            if (e.otros.isNotEmpty()) {
                Text(
                    "Desde otros lugares",
                    style = AgendaTheme.tipo.encabezado,
                    color = c.tinta,
                    modifier = Modifier.padding(top = AgendaSpacing.s16).semantics { heading() },
                )
                Text(
                    "Si vienes de otra actividad, la salida se calcula con este tiempo.",
                    style = AgendaTheme.tipo.cuerpo,
                    color = c.tinta,
                )
                for ((otro, nombre) in e.otros) {
                    CampoTexto(
                        etiqueta = "Desde $nombre, en minutos",
                        valor = e.traslados[otro].orEmpty(),
                        onCambio = { onTraslado(otro, it) },
                        error = e.erroresTraslado[otro],
                        ayuda = "35",
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
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
                BotonSubrayado("Cancelar", tinta = c.tinta, onClick = onCerrar)
                BotonRelleno("Guardar", relleno = c.tinta, tinta = c.papel, onClick = onGuardar)
            }
        }
    }
}
