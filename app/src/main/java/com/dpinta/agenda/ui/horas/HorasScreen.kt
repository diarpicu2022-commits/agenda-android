package com.dpinta.agenda.ui.horas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.avisos.ProgramadorAvisos
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.DateRange
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.PayPeriods
import com.dpinta.agenda.domain.WorkedHours
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.Lengua
import com.dpinta.agenda.ui.components.lengua
import com.dpinta.agenda.ui.hoy.TipoFila
import com.dpinta.agenda.ui.hoy.cuenta
import com.dpinta.agenda.ui.hoy.lineaDe
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Lo que muestra `WorkHoursSummary` (04-patrones · Horas trabajadas). Solo cuenta lo ya trabajado. */
data class ResumenHoras(
    val semana: Duration,
    val dosSemanas: Duration,
    val quincena: Duration,
    val turnosCompletados: Int,
    /** Lunes a domingo de esta semana, en minutos trabajados. */
    val porDia: List<Pair<DayOfWeek, Long>>,
    val hayTurnos: Boolean,
)

object CalculoHoras {
    /** Corta cada sesión en [ahora]: lo que todavía no pasa no cuenta como trabajado. */
    private fun hastaAhora(sesiones: List<Occurrence>, ahora: LocalDateTime) =
        sesiones.filter { it.start.isBefore(ahora) }.map { if (it.end.isAfter(ahora)) it.copy(end = ahora) else it }

    fun resumen(sesiones: List<Occurrence>, esTrabajo: (Long) -> Boolean, ahora: LocalDateTime, hayTurnos: Boolean): ResumenHoras {
        val hoy = ahora.toLocalDate()
        val pasadas = hastaAhora(sesiones, ahora)
        val semana = PayPeriods.week(hoy)
        val porDia = (0L..6L).map { i ->
            val d = semana.start.plusDays(i)
            d.dayOfWeek to WorkedHours.total(pasadas, esTrabajo, DateRange(d, d)).toMinutes()
        }
        return ResumenHoras(
            semana = WorkedHours.total(pasadas, esTrabajo, semana),
            dosSemanas = WorkedHours.total(pasadas, esTrabajo, DateRange(hoy.minusDays(13), hoy)),
            quincena = WorkedHours.total(pasadas, esTrabajo, PayPeriods.fortnight(hoy)),
            turnosCompletados = sesiones.count { esTrabajo(it.activityId) && !it.end.isAfter(ahora) && !it.start.toLocalDate().isBefore(semana.start) },
            porDia = porDia,
            hayTurnos = hayTurnos,
        )
    }
}

@HiltViewModel
class HorasViewModel @Inject constructor(
    repositorio: AgendaRepository,
    reloj: Clock,
    programador: ProgramadorAvisos,
) : ViewModel() {
    val estado: StateFlow<ResumenHoras?> = repositorio.agenda().map { agenda: Agenda ->
        val ahora = LocalDateTime.now(reloj)
        val hoy = ahora.toLocalDate()
        // Los cancelados ya no salen de ScheduleExpander (excepciones), así que no cuentan.
        val sesiones = programador.sesiones(agenda, minOf(hoy.minusDays(13), PayPeriods.fortnight(hoy).start, PayPeriods.week(hoy).start), hoy.plusDays(7))
        val esTrabajo = { id: Long -> agenda.actividades[id]?.tipo == ActivityKind.TRABAJO }
        CalculoHoras.resumen(sesiones, esTrabajo, ahora, agenda.actividades.values.any { it.tipo == ActivityKind.TRABAJO })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun HorasRuta(onAtras: () -> Unit, viewModel: HorasViewModel = hiltViewModel()) {
    val r by viewModel.estado.collectAsStateWithLifecycle()
    HorasPantalla(r, onAtras)
}

/** Horas trabajadas (`WorkHoursSummary`): la semana en cifra grande, dos semanas y turnos al lado, barras por día. */
@Composable
fun HorasPantalla(r: ResumenHoras?, onAtras: () -> Unit) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val l = lengua()
    Column(Modifier.fillMaxSize().background(ds.fondo)) {
        Cabecera(l("Horas trabajadas", "Hours worked"), onAtras = onAtras)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = AgendaTheme.reticula.margen, vertical = AgendaSpacing.s8).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s16),
        ) {
            when {
                r == null -> Text(l("Calculando…", "Calculating…"), style = t.apoyo, color = ds.tintaSuave)
                !r.hayTurnos -> Text(
                    l("Todavía no tienes turnos. Crea una actividad de tipo Trabajo y aquí verás tus horas.",
                        "You don't have shifts yet. Create a Work activity and your hours will show up here."),
                    style = t.cuerpo, color = ds.tinta,
                )
                else -> Resumen(r, l)
            }
        }
    }
}

@Composable
private fun Resumen(r: ResumenHoras, l: Lengua) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(l("Esta semana", "This week"), style = t.etiqueta, color = ds.tintaSuave, modifier = Modifier.semantics { heading() })
        Text(cuenta(r.semana.toMinutes()), style = t.horaHero, color = ds.tinta)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AgendaSpacing.s24)) {
        Dato(l("Últimas 2 semanas", "Last 2 weeks"), cuenta(r.dosSemanas.toMinutes()), Modifier.weight(1f))
        Dato(l("Turnos completados", "Shifts completed"), r.turnosCompletados.toString(), Modifier.weight(1f))
    }
    Dato(l("Esta quincena", "This pay period"), cuenta(r.quincena.toMinutes()))
    Barras(r, l)
    Text(l("Los turnos cancelados no cuentan.", "Cancelled shifts don't count."), style = t.apoyo, color = ds.tintaSuave)
}

@Composable
private fun Dato(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(rotulo, style = t.apoyo, color = ds.tintaSuave)
        Text(valor, style = t.horaLg, color = ds.tinta)
    }
}

/** Barras por día en `linea-trabajo`; la escala es el día con más horas (mínimo 8 h). Se lee como una sola frase. */
@Composable
private fun Barras(r: ResumenHoras, l: Lengua) {
    val ds = AgendaTheme.ds
    val t = AgendaTheme.tipo
    val color = ds.lineaDe(TipoFila.Trabajo)
    val vacio = ds.superficieFuerte
    val maximo = maxOf(8 * 60L, r.porDia.maxOf { it.second })
    val letras = if (l == Lengua.EN) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("L", "M", "X", "J", "V", "S", "D")
    val nombre = { d: DayOfWeek -> d.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag(l("es", "en"))) }
    val frase = r.porDia.joinToString(", ") { (d, m) -> "${nombre(d)} ${cuenta(m)}" }
    Column(Modifier.fillMaxWidth().semantics { contentDescription = frase }) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val ancho = size.width / 7f
            val barra = ancho * 0.46f
            r.porDia.forEachIndexed { i, (_, m) ->
                val x = i * ancho + (ancho - barra) / 2
                drawRoundRect(vacio, Offset(x, 0f), Size(barra, size.height), CornerRadius(barra / 2, barra / 2))
                val alto = size.height * (m.toFloat() / maximo)
                if (alto > 0f) drawRoundRect(color, Offset(x, size.height - alto), Size(barra, alto), CornerRadius(barra / 2, barra / 2))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            letras.forEach { Text(it, style = t.apoyo, color = ds.tintaSuave, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
    }
}
