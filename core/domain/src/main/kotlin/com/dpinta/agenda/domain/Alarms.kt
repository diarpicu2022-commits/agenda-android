package com.dpinta.agenda.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Qué dispara cada alarma exacta (arquitectura §4). */
enum class AlarmKind {
    /** Pedir posición y ruta con tiempo: `inicio − 120 min`. Solo si hay traslado. */
    PRECALCULO,

    /** Recordatorio propio de la actividad (`avisoAntesMin`). */
    AVISO,

    /** La hora de salida, cuando ya hay una estimación de trayecto. */
    SALIDA,
}

data class PlannedAlarm(
    val kind: AlarmKind,
    val activityId: Long,
    /** Identifica la sesión; junto con [kind] da una clave estable para reemplazar la alarma. */
    val occurrenceStart: LocalDateTime,
    val at: Instant,
)

/** Ajustes de aviso de una actividad. */
data class AlarmSettings(
    /** Colchón al llegar. */
    val margin: Duration,
    /** Null: la actividad no tiene recordatorio propio. */
    val remindBefore: Duration?,
)

object AlarmPlanner {

    /** Arquitectura §4.1. */
    val PRECALCULATION_LEAD: Duration = Duration.ofMinutes(120)

    /**
     * Todas las alarmas que deben quedar programadas a partir de [now] para [occurrences].
     * Se recalcula entera tras reiniciar el teléfono o cambiar la hora o la zona: el resultado solo
     * depende de los datos, así que programarla dos veces no duplica nada.
     *
     * @param travelOf última estimación de trayecto hacia esa sesión, o null si aún no hay.
     */
    fun plan(
        occurrences: List<Occurrence>,
        zone: ZoneId,
        now: Instant,
        settingsOf: (activityId: Long) -> AlarmSettings,
        travelOf: (Occurrence) -> TravelEstimate?,
    ): List<PlannedAlarm> {
        val result = mutableListOf<PlannedAlarm>()
        for (occ in occurrences.sortedBy { it.start }) {
            val start = occ.start.atZone(zone).toInstant()
            if (!start.isAfter(now)) continue
            val settings = settingsOf(occ.activityId)
            fun add(kind: AlarmKind, at: Instant) {
                if (at.isAfter(now)) result += PlannedAlarm(kind, occ.activityId, occ.start, at)
            }
            settings.remindBefore?.let { add(AlarmKind.AVISO, start - it) }
            if (DayPlanner.needsTravel(DayPlanner.previous(occurrences, occ), occ)) {
                add(AlarmKind.PRECALCULO, start - PRECALCULATION_LEAD)
                travelOf(occ)?.let { add(AlarmKind.SALIDA, DepartureCalculator.plan(start, settings.margin, it, now).leaveAt) }
            }
        }
        return result.sortedWith(compareBy({ it.at }, { it.kind }))
    }
}
