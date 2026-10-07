package com.dpinta.agenda.domain

import java.time.Duration
import java.time.LocalDateTime

/**
 * Qué mostrar ahora en Hoy, el widget y CampusWatch (decisión de Diego, 2026-10-07): la sesión **en curso** se
 * queda mientras dura; se cambia a la siguiente cuando ya toca prepararse para ella — 15 min antes de la hora de
 * salida si hay trayecto, o 15 min antes de que empiece si no lo hay — o cuando la en curso termina.
 */
object DayFocus {
    /** Ventana de «prepárate» (igual que [DepartureCalculator.PREPARE_WINDOW]). */
    val SWITCH_BEFORE: Duration = DepartureCalculator.PREPARE_WINDOW

    /** A partir de cuándo la siguiente pasa a primer plano. */
    fun switchAt(nextStart: LocalDateTime, nextLeave: LocalDateTime?): LocalDateTime =
        (nextLeave ?: nextStart).minus(SWITCH_BEFORE)

    /** La sesión en curso (empezó y no ha terminado), si hay. */
    fun <T> current(sessions: List<T>, now: LocalDateTime, start: (T) -> LocalDateTime, end: (T) -> LocalDateTime): T? =
        sessions.filter { !start(it).isAfter(now) && end(it).isAfter(now) }.maxByOrNull(start)

    /**
     * ¿Se muestra la en curso? Sí, si existe y todavía no llegó el momento de cambiar a la siguiente.
     * @param nextStart inicio de la siguiente (null si no hay ninguna).
     * @param nextLeave hora de salida hacia la siguiente (null si no hay trayecto).
     */
    fun showCurrent(now: LocalDateTime, currentEnd: LocalDateTime?, nextStart: LocalDateTime?, nextLeave: LocalDateTime?): Boolean {
        if (currentEnd == null || !currentEnd.isAfter(now)) return false
        if (nextStart == null) return true
        return now.isBefore(switchAt(nextStart, nextLeave))
    }
}
