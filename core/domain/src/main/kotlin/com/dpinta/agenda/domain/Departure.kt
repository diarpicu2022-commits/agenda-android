package com.dpinta.agenda.domain

import java.time.Duration
import java.time.Instant

/** Estados de la banda de salida (contrato C10). */
enum class DepartureState { ESPERA, PREPARATE, SAL_YA, VAS_TARDE }

data class TravelEstimate(
    val duration: Duration,
    val mode: TransportMode,
    val computedAt: Instant,
    /** Sin red o sin permiso: se usó la última duración conocida para este trayecto. */
    val fromCache: Boolean,
    /** Escrita por Diego («Campus: 23 min en bus»): no caduca, así que nunca es «dato viejo». */
    val manual: Boolean = false,
)

data class DeparturePlan(
    val leaveAt: Instant,
    val state: DepartureState,
    /** Minutos hasta la hora de salida; negativos si ya pasó. */
    val minutesToLeave: Long,
    /** Si sales ahora, a qué hora llegas (para «vas tarde · llegas 8:04»). */
    val arriveIfLeavingNow: Instant,
    val stale: Boolean,
)

object DepartureCalculator {

    /** Contrato C10: «prepárate» cuando faltan 15 minutos o menos. */
    val PREPARE_WINDOW: Duration = Duration.ofMinutes(15)

    /** Tras la hora de salida hay 2 minutos de «sal ya» antes de pasar a «vas tarde». */
    val LEAVE_GRACE: Duration = Duration.ofMinutes(2)

    /** Contrato C10: una estimación de hace más de 15 minutos se marca como dato viejo. */
    val STALE_AFTER: Duration = Duration.ofMinutes(15)

    fun plan(
        activityStart: Instant,
        margin: Duration,
        travel: TravelEstimate,
        now: Instant,
    ): DeparturePlan {
        require(!margin.isNegative)
        val leaveAt = activityStart - margin - travel.duration
        val untilLeave = Duration.between(now, leaveAt)
        val state = when {
            untilLeave > PREPARE_WINDOW -> DepartureState.ESPERA
            !untilLeave.isNegative && untilLeave != Duration.ZERO -> DepartureState.PREPARATE
            untilLeave.negated() < LEAVE_GRACE -> DepartureState.SAL_YA
            else -> DepartureState.VAS_TARDE
        }
        return DeparturePlan(
            leaveAt = leaveAt,
            state = state,
            minutesToLeave = Math.floorDiv(untilLeave.seconds, 60L),
            arriveIfLeavingNow = now + travel.duration,
            stale = !travel.manual && (travel.fromCache || Duration.between(travel.computedAt, now) > STALE_AFTER),
        )
    }

    /**
     * Cuándo volver a pedir la ruta: a mitad del tiempo que falta para salir (el tráfico cambia),
     * sin recalcular en los últimos 15 minutos para que la cifra no salte mientras Diego se alista.
     */
    fun nextRecalculation(leaveAt: Instant, now: Instant): Instant? {
        val remaining = Duration.between(now, leaveAt)
        if (remaining <= PREPARE_WINDOW) return null
        return now + remaining.dividedBy(2)
    }
}
