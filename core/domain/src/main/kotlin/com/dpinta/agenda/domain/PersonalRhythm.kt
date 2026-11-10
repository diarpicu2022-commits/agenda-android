package com.dpinta.agenda.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant

/** Una salida real: cuándo se planeaba salir y cuándo se tocó «Voy saliendo» o «Ya voy». */
data class DepartureSample(
    val activityId: Long,
    val placeId: Long?,
    val weekday: DayOfWeek,
    val planned: Instant,
    val actual: Instant,
) {
    /** Minutos de retraso (negativo si salió antes). */
    val lateMinutes: Long get() = Duration.between(planned, actual).toMinutes()
}

/** Consejo de «Tu ritmo»: cuántos minutos suele salir tarde la persona, con cuántas salidas y de qué grupo salió. */
data class RhythmAdvice(val lateMinutes: Int, val samples: Int, val scope: Scope) {
    enum class Scope { LUGAR, DIA, GENERAL }
}

/**
 * IA de Agenda en el teléfono (anexo 2026-11-10-agenda-ia, opción A): aprende el retraso típico de las salidas reales.
 * Estadística robusta, sin Internet: mediana de las últimas salidas, sin los casos de más de una hora (olvidos o no
 * salidas). Prefiere las salidas al mismo lugar, después las del mismo día de la semana y después todas.
 */
object PersonalRhythm {
    /** Salidas mínimas para dar un consejo. */
    const val MIN_SAMPLES = 4
    /** Por debajo de esto no se aconseja: es ruido. */
    const val MIN_LATE = 2
    /** Tope del consejo: más que esto es un caso raro, no un ritmo. */
    const val MAX_LATE = 20
    /** Solo las salidas más recientes: el ritmo cambia con el semestre. */
    const val WINDOW = 30
    private const val OUTLIER_MINUTES = 60L

    fun advice(history: List<DepartureSample>, placeId: Long?, weekday: DayOfWeek): RhythmAdvice? {
        val clean = history.filter { kotlin.math.abs(it.lateMinutes) <= OUTLIER_MINUTES }.sortedByDescending { it.actual }
        val groups = listOf(
            RhythmAdvice.Scope.LUGAR to clean.filter { placeId != null && it.placeId == placeId },
            RhythmAdvice.Scope.DIA to clean.filter { it.weekday == weekday },
            RhythmAdvice.Scope.GENERAL to clean,
        )
        val (scope, samples) = groups.firstOrNull { it.second.size >= MIN_SAMPLES } ?: return null
        val recent = samples.take(WINDOW)
        val late = median(recent.map { it.lateMinutes })
        if (late < MIN_LATE) return null
        return RhythmAdvice(late.coerceAtMost(MAX_LATE.toLong()).toInt(), recent.size, scope)
    }

    private fun median(v: List<Long>): Long {
        val s = v.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }
}
