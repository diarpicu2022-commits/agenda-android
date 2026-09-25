package com.dpinta.agenda.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

/**
 * Qué sesión del día es «la siguiente» y si para llegar a ella hace falta trasladarse.
 * Complementa a [DepartureCalculator]: este decide CUÁNDO salir; aquí se decide HACIA DÓNDE.
 */
object DayPlanner {

    /** La primera sesión que todavía no ha empezado, o null si no queda ninguna. */
    fun next(occurrences: List<Occurrence>, now: LocalDateTime): Occurrence? =
        occurrences.sortedBy { it.start }.firstOrNull { it.start.isAfter(now) }

    /** La sesión inmediatamente anterior a [next] el mismo día, si la hay. */
    fun previous(occurrences: List<Occurrence>, next: Occurrence): Occurrence? =
        occurrences
            .filter { it.start.toLocalDate() == next.start.toLocalDate() && !it.end.isAfter(next.start) && it != next }
            .maxByOrNull { it.end }

    /**
     * Hace falta traslado si la sesión tiene lugar y la anterior del mismo día no fue en ese
     * mismo lugar. Sin lugar no hay nada que calcular: la banda muestra «sin traslado» (C10).
     */
    fun needsTravel(previous: Occurrence?, next: Occurrence): Boolean {
        val destination = next.placeId ?: return false
        return previous?.placeId != destination
    }

    /**
     * Desde qué lugar se sale hacia [next]: el de la sesión anterior del mismo día si hace falta
     * traslado y esa sesión tenía lugar; null = desde casa (primera del día o anterior sin lugar).
     */
    fun origin(occurrences: List<Occurrence>, next: Occurrence): Long? {
        val previous = previous(occurrences, next)
        if (!needsTravel(previous, next)) return null
        return previous?.placeId
    }
}

/** Trayecto ya iniciado («Voy saliendo»). */
data class TripProgress(
    val arriveAt: Instant,
    /** Minutos que faltan para llegar; 0 si ya debería haber llegado. */
    val minutesRemaining: Long,
)

object Trip {

    /** Llegada estimada de un trayecto que empezó en [leftAt] con la estimación [travel]. */
    fun progress(leftAt: Instant, travel: TravelEstimate, now: Instant): TripProgress {
        val arriveAt = leftAt + travel.duration
        val remaining = Duration.between(now, arriveAt)
        return TripProgress(
            arriveAt = arriveAt,
            minutesRemaining = if (remaining.isNegative) 0 else Math.floorDiv(remaining.seconds + 59, 60L),
        )
    }
}
