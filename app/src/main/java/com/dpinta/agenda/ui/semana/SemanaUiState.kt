package com.dpinta.agenda.ui.semana

import androidx.compose.runtime.Immutable
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.domain.ColombianHolidays
import com.dpinta.agenda.domain.ScheduleExpander
import com.dpinta.agenda.ui.hoy.TipoFila
import com.dpinta.agenda.ui.hoy.aTipoFila
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

sealed interface SemanaUiState {
    data object Cargando : SemanaUiState
    data object PrimerUso : SemanaUiState

    @Immutable
    data class Semana(
        val lunes: LocalDate,
        /** Semana del semestre (1, 2…), o nula si esta semana no cae en uno. */
        val numero: Int?,
        val dias: List<DiaSemana>,
        val bloques: List<Bloque>,
        /** Primera y última hora que se dibujan (franjas de 48 dp, C4.6). */
        val desde: Int,
        val hasta: Int,
        /** Hora de «ahora», solo si hoy cae en esta semana. */
        val ahora: LocalTime?,
    ) : SemanaUiState
}

@Immutable
data class DiaSemana(
    val fecha: LocalDate,
    val esHoy: Boolean,
    /** «Festivo: Día de la Raza» o «Sin clase» si el semestre lo marca; nulo si es día normal. */
    val sinClase: String?,
)

@Immutable
data class Bloque(
    val actividadId: Long,
    /** 0 = lunes … 6 = domingo. */
    val dia: Int,
    val inicio: LocalTime,
    val fin: LocalTime,
    val titulo: String,
    /** Contenido abreviado de la celda: «Cálc» (C2.3: se abrevia, no se reduce el cuerpo). */
    val abreviado: String,
    val salon: String,
    val tipo: TipoFila,
    /** Si dos sesiones se cruzan, cada una ocupa su carril dentro de la columna. */
    val carril: Int,
    val carriles: Int,
)

object SemanaMapeador {

    /** Horas por defecto si la semana no tiene nada: un día universitario típico. */
    private const val DESDE = 7
    private const val HASTA = 18

    fun lunesDe(fecha: LocalDate): LocalDate = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun estado(agenda: Agenda, lunes: LocalDate, ahora: LocalDateTime): SemanaUiState {
        if (agenda.vacia) return SemanaUiState.PrimerUso
        val domingo = lunes.plusDays(6)
        val semestre = agenda.semestres.firstOrNull { !domingo.isBefore(it.start) && !lunes.isAfter(it.end) }
        val sesiones = ScheduleExpander.expand(
            rules = agenda.reglas,
            oneOff = agenda.puntuales,
            placeOf = { agenda.actividades[it]?.lugarId },
            from = lunes,
            to = domingo,
            semester = semestre,
            exceptions = agenda.excepciones,
        ).filter { it.activityId in agenda.actividades }

        val bloques = sesiones.groupBy { it.start.toLocalDate() }.flatMap { (fecha, delDia) ->
            val ordenadas = delDia.sortedWith(compareBy({ it.start }, { it.end }))
            // Carriles: cada sesión va al primer carril libre; un grupo de sesiones que se cruzan comparte el total.
            val carrilDe = IntArray(ordenadas.size)
            val finDeCarril = mutableListOf<LocalDateTime>()
            val grupos = mutableListOf<MutableList<Int>>()
            var finGrupo: LocalDateTime? = null
            ordenadas.forEachIndexed { i, s ->
                if (finGrupo == null || !s.start.isBefore(finGrupo)) {
                    grupos += mutableListOf<Int>()
                    finDeCarril.clear()
                }
                val libre = finDeCarril.indexOfFirst { !s.start.isBefore(it) }
                carrilDe[i] = if (libre >= 0) libre else finDeCarril.size
                if (libre >= 0) finDeCarril[libre] = s.end else finDeCarril += s.end
                grupos.last() += i
                finGrupo = if (finGrupo == null || s.end.isAfter(finGrupo)) s.end else finGrupo
            }
            val carrilesDe = IntArray(ordenadas.size)
            for (g in grupos) {
                val total = g.maxOf { carrilDe[it] } + 1
                for (i in g) carrilesDe[i] = total
            }
            ordenadas.mapIndexed { i, s ->
                val act = agenda.actividades.getValue(s.activityId)
                Bloque(
                    actividadId = act.id,
                    dia = fecha.dayOfWeek.value - 1,
                    inicio = s.start.toLocalTime(),
                    fin = s.end.toLocalTime(),
                    titulo = act.titulo,
                    abreviado = abreviar(act.titulo),
                    salon = act.salon,
                    tipo = act.tipo.aTipoFila(),
                    carril = carrilDe[i],
                    carriles = carrilesDe[i],
                )
            }
        }

        val festivos = (lunes.year..domingo.year).flatMap { ColombianHolidays.of(it) }.associate { it.date to it.name }
        val dias = (0L..6L).map { lunes.plusDays(it) }.map { fecha ->
            val libre = semestre?.daysOff?.contains(fecha) == true
            DiaSemana(
                fecha = fecha,
                esHoy = fecha == ahora.toLocalDate(),
                sinClase = if (!libre) null else festivos[fecha]?.let { "Festivo: $it" } ?: "Sin clase",
            )
        }
        val desde = bloques.minOfOrNull { it.inicio.hour } ?: DESDE
        val hasta = bloques.maxOfOrNull { if (it.fin.minute > 0) it.fin.hour + 1 else it.fin.hour }?.coerceAtLeast(desde + 1) ?: HASTA
        val numero = semestre?.let { ChronoUnit.WEEKS.between(lunesDe(it.start), lunes).toInt() + 1 }?.takeIf { it >= 1 }
        val hoyEnSemana = !ahora.toLocalDate().isBefore(lunes) && !ahora.toLocalDate().isAfter(domingo)
        return SemanaUiState.Semana(lunes, numero, dias, bloques, desde, hasta, if (hoyEnSemana) ahora.toLocalTime() else null)
    }

    /** Primera palabra, hasta 4 letras: «Cálculo diferencial» → «Cálc», «Turno» → «Turn». */
    fun abreviar(titulo: String): String = titulo.trim().split(' ').first().take(4)
}
