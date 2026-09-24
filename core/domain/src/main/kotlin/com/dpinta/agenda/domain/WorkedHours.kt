package com.dpinta.agenda.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/** Un rango de días, ambos incluidos. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start)) { "El rango termina antes de empezar" }
    }
}

object PayPeriods {

    /** Semana de lunes a domingo. */
    fun week(date: LocalDate): DateRange =
        DateRange(
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
        )

    /** Quincena colombiana: del 1 al 15 y del 16 al último día del mes. */
    fun fortnight(date: LocalDate): DateRange =
        if (date.dayOfMonth <= 15) DateRange(date.withDayOfMonth(1), date.withDayOfMonth(15))
        else DateRange(date.withDayOfMonth(16), date.with(TemporalAdjusters.lastDayOfMonth()))
}

/** Arquitectura §6 P2.10 · Horas trabajadas a partir de los turnos. */
object WorkedHours {

    /**
     * Suma lo que dura cada sesión de trabajo dentro de [range]. Un turno que cruza la medianoche
     * del borde solo cuenta la parte que cae dentro.
     */
    fun total(occurrences: List<Occurrence>, isWork: (activityId: Long) -> Boolean, range: DateRange): Duration {
        val from: LocalDateTime = range.start.atStartOfDay()
        val to: LocalDateTime = range.end.plusDays(1).atStartOfDay()
        return occurrences
            .filter { isWork(it.activityId) }
            .fold(Duration.ZERO) { acc, o ->
                val s = maxOf(o.start, from)
                val e = minOf(o.end, to)
                if (e.isAfter(s)) acc + Duration.between(s, e) else acc
            }
    }
}
