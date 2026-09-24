package com.dpinta.agenda.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ActivityKind { CLASE, TRABAJO, PUNTUAL, EXAMEN, OTRO }

/** Modos que usa Diego. MOTO se calcula como vehículo de dos ruedas en Routes API. */
enum class TransportMode { TRANSPORTE_PUBLICO, A_PIE, CARRO, MOTO }

/** Serie semanal: los mismos días y horas cada semana entre [from] y [until] (el semestre). */
data class WeeklyRule(
    val activityId: Long,
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
    val from: LocalDate,
    val until: LocalDate,
) {
    init {
        require(days.isNotEmpty()) { "Una serie semanal necesita al menos un día" }
        require(end.isAfter(start)) { "La actividad termina antes de empezar" }
        require(!until.isBefore(from)) { "La serie termina antes de empezar" }
    }
}

/** Cambio sobre una sola sesión de una serie («hoy no hay Cálculo», «esta semana es a las 10»). */
sealed interface SessionException {
    val activityId: Long
    val date: LocalDate

    data class Cancelled(override val activityId: Long, override val date: LocalDate) : SessionException
    data class Moved(
        override val activityId: Long,
        override val date: LocalDate,
        val newStart: LocalDateTime,
        val newEnd: LocalDateTime,
    ) : SessionException
}

data class Occurrence(
    val activityId: Long,
    val placeId: Long?,
    val start: LocalDateTime,
    val end: LocalDateTime,
)

/** Semestre: las series de clase se saltan festivos y semanas sin clase. */
data class Semester(val start: LocalDate, val end: LocalDate, val daysOff: Set<LocalDate>)

object ScheduleExpander {

    /**
     * Genera las sesiones de [rules] entre [from] y [to] (ambos incluidos), más las puntuales,
     * aplicando festivos del [semester] a las series y las excepciones sesión a sesión.
     */
    fun expand(
        rules: List<WeeklyRule>,
        oneOff: List<Occurrence>,
        placeOf: (activityId: Long) -> Long?,
        from: LocalDate,
        to: LocalDate,
        semester: Semester? = null,
        exceptions: List<SessionException> = emptyList(),
    ): List<Occurrence> {
        val byKey = exceptions.associateBy { it.activityId to it.date }
        val result = mutableListOf<Occurrence>()
        var day = from
        while (!day.isAfter(to)) {
            for (rule in rules) {
                if (day.dayOfWeek !in rule.days || day.isBefore(rule.from) || day.isAfter(rule.until)) continue
                if (semester != null && (day in semester.daysOff || day.isBefore(semester.start) || day.isAfter(semester.end))) continue
                when (val ex = byKey[rule.activityId to day]) {
                    is SessionException.Cancelled -> Unit
                    is SessionException.Moved -> result += Occurrence(rule.activityId, placeOf(rule.activityId), ex.newStart, ex.newEnd)
                    null -> result += Occurrence(rule.activityId, placeOf(rule.activityId), day.atTime(rule.start), day.atTime(rule.end))
                }
            }
            day = day.plusDays(1)
        }
        oneOff.filterTo(result) { !it.start.toLocalDate().isBefore(from) && !it.start.toLocalDate().isAfter(to) }
        return result.sortedBy { it.start }
    }
}

sealed interface Conflict {
    data class Overlap(val first: Occurrence, val second: Occurrence) : Conflict

    /** Entre dos actividades en lugares distintos no da el tiempo de traslado. */
    data class NotEnoughTravel(
        val from: Occurrence,
        val to: Occurrence,
        val availableMinutes: Long,
        val neededMinutes: Long,
    ) : Conflict
}

object ConflictDetector {

    /**
     * @param travelMinutes minutos estimados entre dos lugares (el último cálculo conocido),
     *   o null si no hay estimación: en ese caso no se afirma un conflicto que no se sabe.
     */
    fun detect(
        occurrences: List<Occurrence>,
        travelMinutes: (fromPlace: Long, toPlace: Long) -> Long?,
    ): List<Conflict> {
        val sorted = occurrences.sortedBy { it.start }
        val conflicts = mutableListOf<Conflict>()
        for ((a, b) in sorted.zipWithNext()) {
            if (b.start.isBefore(a.end)) {
                conflicts += Conflict.Overlap(a, b)
                continue
            }
            val pa = a.placeId ?: continue
            val pb = b.placeId ?: continue
            if (pa == pb || a.start.toLocalDate() != b.start.toLocalDate()) continue
            val needed = travelMinutes(pa, pb) ?: continue
            val available = java.time.Duration.between(a.end, b.start).toMinutes()
            if (available < needed) conflicts += Conflict.NotEnoughTravel(a, b, available, needed)
        }
        return conflicts
    }
}
