package com.dpinta.agenda

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dpinta.agenda.data.agenda.AgendaEnMemoria
import com.dpinta.agenda.data.agenda.RelojAjustable
import com.dpinta.agenda.ui.AgendaRoot
import com.dpinta.agenda.ui.navegacion.Pestana
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var reloj: RelojAjustable

    @Inject lateinit var agendaEjemplo: AgendaEnMemoria

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pestana = if (depurable()) aplicarExtrasDeDepuracion() else Pestana.Hoy
        val escenario = if (depurable()) intent.getStringExtra("formulario").orEmpty() else ""
        setContent { AgendaRoot(pestanaInicial = pestana, escenarioFormulario = escenario) }
    }

    private fun depurable() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /**
     * Solo depuración, para capturas reproducibles:
     * - «hora» (HH:mm): la app finge que es esa hora de hoy.
     * - «pestana» (hoy | semana | actividades): destino inicial.
     * - «ejemplo» = «cruce»: datos de ejemplo con un solape (verificar el texto de C10).
     * - «formulario» = nuevo | errores | completo | conflictos | puntual: abre el formulario así.
     */
    private fun aplicarExtrasDeDepuracion(): Pestana {
        intent.getStringExtra("hora")?.let { texto ->
            runCatching { LocalTime.parse(texto) }.getOrNull()?.let { hora ->
                val ahora = java.time.Instant.now()
                val objetivo = LocalDate.now(reloj.zone).atTime(hora).atZone(reloj.zone).toInstant()
                reloj.desfase = Duration.between(ahora, objetivo)
            }
        }
        if (intent.getStringExtra("ejemplo") == "cruce") {
            agendaEjemplo.reemplazar(AgendaEnMemoria.ejemploConCruce(LocalDate.now(reloj)))
        }
        val nombre = intent.getStringExtra("pestana")
        return Pestana.entries.firstOrNull { it.name.equals(nombre, ignoreCase = true) } ?: Pestana.Hoy
    }
}
