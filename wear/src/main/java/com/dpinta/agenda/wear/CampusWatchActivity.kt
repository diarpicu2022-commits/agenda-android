package com.dpinta.agenda.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import com.dpinta.agenda.domain.Urgency
import com.dpinta.agenda.domain.WatchSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** CampusWatch: qué, dónde, cuándo, qué tan urgente y si hay que salir — de un vistazo (03-campuswatch.md). */
class CampusWatchActivity : ComponentActivity() {

    private var ambiente by mutableStateOf(false)

    private val observador = AmbientLifecycleObserver(this, object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) { ambiente = true }
        override fun onExitAmbient() { ambiente = false }
        override fun onUpdateAmbient() {}
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(observador)
        lifecycleScope.launch { AlmacenDia.sincronizar(this@CampusWatchActivity) }
        setContent { CampusWatch(ambiente) }
    }
}

/** La hora, redondeada al minuto: el anillo y las cifras se redibujan una vez por minuto, no más. */
@Composable
private fun minutoActual(): LocalDateTime {
    val ahora by produceState(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)) {
        while (true) {
            val t = LocalDateTime.now()
            value = t.truncatedTo(ChronoUnit.MINUTES)
            delay(60_000L - (t.second * 1000L + t.nano / 1_000_000L))
        }
    }
    return ahora
}

private data class Confirmacion(val texto: String, val detalle: String?, val ok: Boolean = true)

@Composable
fun CampusWatch(ambiente: Boolean) {
    val contexto = LocalContext.current
    val dia by AlmacenDia.dia(contexto).collectAsState()
    val ahora = minutoActual()
    val zona = ZoneId.systemDefault()
    var confirmacion by remember { mutableStateOf<Confirmacion?>(null) }
    val alcance = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Reloj.fondo)) {
        val d = dia
        val c = confirmacion
        when {
            c != null -> PantallaConfirmacion(c.texto, c.detalle, c.ok) { confirmacion = null }
            d == null -> PantallaSinDatos()
            else -> {
                // La en curso se queda mientras dura; cambia a la siguiente cuando ya toca prepararse (DayFocus).
                val enfoque = d.focus(ahora)
                val siguiente = enfoque?.session
                val accion: (WatchSession, Accion) -> Unit = { s, a ->
                    alcance.launch {
                        val ok = Telefono.enviar(contexto, a, s)
                        confirmacion = if (!ok) Confirmacion("Sin conexión", "No llegó al teléfono. Hazlo desde allá.", ok = false)
                        else when (a) {
                            Accion.YaVoy -> Confirmacion("En camino", null)
                            Accion.Aplazar -> Confirmacion("Aplazado", "Nueva hora ${Formato.hora(LocalDateTime.now().plusMinutes(5).toLocalTime())}")
                            Accion.Cancelar -> Confirmacion("Cancelada solo hoy", "La serie se mantiene")
                        }
                    }
                }
                val paginas = buildList<@Composable () -> Unit> {
                    if (siguiente == null) {
                        add { PantallaLibre(d, ahora) }
                    } else if (enfoque?.inProgress == true) {
                        add { PantallaEnCurso(siguiente, ahora, ambiente) }
                    } else {
                        val u = siguiente.urgency(ahora, zona)
                        val salir = siguiente.leaveAt != null && (u == Urgency.LEAVE_NOW || u == Urgency.URGENT)
                        add {
                            if (salir) PantallaSalir(siguiente, ahora, zona, ambiente) { accion(siguiente, it) }
                            else PantallaProxima(siguiente, ahora, zona, ambiente)
                        }
                        if (!ambiente && u != Urgency.MISSED) add { PantallaAcciones(siguiente, Reloj.urgencia(u, siguiente.exam)) { accion(siguiente, it) } }
                    }
                    if (!ambiente) {
                        add { PantallaHorario(d, ahora.toLocalDate(), ahora, zona) }
                        add { PantallaHorario(d, ahora.toLocalDate().plusDays(1), ahora, zona) }
                    }
                }
                val estado = rememberPagerState { paginas.size }
                val foco = remember { FocusRequester() }
                var giro by remember { mutableStateOf(0f) }
                // Navegación: desplazamiento vertical con la corona (Próxima · Acciones · Horario de hoy · Mañana).
                VerticalPager(
                    state = estado,
                    modifier = Modifier
                        .fillMaxSize()
                        // Corona: un gesto = una página (se acumula el giro para no saltar varias con un toque leve).
                        .onRotaryScrollEvent { e ->
                            giro += e.verticalScrollPixels
                            if (kotlin.math.abs(giro) > 40f && !estado.isScrollInProgress) {
                                val destino = (estado.currentPage + if (giro > 0) 1 else -1).coerceIn(0, paginas.size - 1)
                                giro = 0f
                                alcance.launch { estado.animateScrollToPage(destino) }
                            }
                            true
                        }
                        .focusRequester(foco)
                        .focusable(),
                ) { i -> paginas.getOrNull(i)?.invoke() }
                LaunchedEffect(Unit) { foco.requestFocus() }
            }
        }
    }
}
