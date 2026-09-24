package com.dpinta.agenda.domain.ics

import com.dpinta.agenda.domain.WeeklyRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** Repetición semanal tal como la entiende la agenda (el resto de reglas de iCalendar no se importan). */
data class IcsWeekly(
    val days: Set<DayOfWeek>,
    /** Último día de la serie; null si el archivo no pone fin. */
    val until: LocalDate?,
)

/** Un evento de un archivo `.ics`, con las horas ya pasadas a la zona del teléfono. */
data class IcsEvent(
    val uid: String,
    val summary: String,
    val location: String?,
    val description: String?,
    val start: LocalDateTime,
    val end: LocalDateTime,
    /** Evento de día completo: la agenda no lo convierte en actividad con hora. */
    val allDay: Boolean = false,
    val weekly: IcsWeekly? = null,
    /** Sesiones quitadas de la serie (EXDATE). */
    val excluded: Set<LocalDate> = emptySet(),
    /** Si no es null, este evento reemplaza la sesión de la serie [uid] que empezaba a esa hora. */
    val recurrenceId: LocalDateTime? = null,
    /** La regla que no se pudo entender (mensual, cada dos semanas…), para decírselo a Diego en vez de perderla. */
    val unsupportedRule: String? = null,
) {
    /** Convierte la serie en una [WeeklyRule]; si el archivo no pone fin se usa [fallbackUntil]. */
    fun toWeeklyRule(activityId: Long, fallbackUntil: LocalDate): WeeklyRule? {
        val w = weekly ?: return null
        if (allDay || end.toLocalDate() != start.toLocalDate()) return null
        return WeeklyRule(activityId, w.days, start.toLocalTime(), end.toLocalTime(), start.toLocalDate(), w.until ?: fallbackUntil)
    }
}

internal val ICS_DAYS = mapOf(
    "MO" to DayOfWeek.MONDAY, "TU" to DayOfWeek.TUESDAY, "WE" to DayOfWeek.WEDNESDAY, "TH" to DayOfWeek.THURSDAY,
    "FR" to DayOfWeek.FRIDAY, "SA" to DayOfWeek.SATURDAY, "SU" to DayOfWeek.SUNDAY,
)
