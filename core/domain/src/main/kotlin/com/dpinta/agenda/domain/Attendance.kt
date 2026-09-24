package com.dpinta.agenda.domain

import java.time.LocalDate

enum class AttendanceMark { ASISTI, FALTE }

data class AttendanceRecord(val activityId: Long, val date: LocalDate, val mark: AttendanceMark)

data class AttendanceSummary(
    val attended: Int,
    val absences: Int,
    /** Sesiones pasadas sin marcar: no cuentan como falta. */
    val unmarked: Int,
    /** Faltas permitidas por la materia; null si no tiene límite. */
    val maxAbsences: Int?,
    /** Faltas que aún se pueden tener sin pasarse del límite; negativo si ya se pasó. */
    val absencesLeft: Int?,
    val overLimit: Boolean,
)

/** Arquitectura §6 P2.9 · Asistencia por materia. */
object Attendance {

    /**
     * Resumen de una materia hasta [today] (incluido).
     *
     * Cuenta solo las sesiones que de verdad hubo según [held] (ya sin festivos ni cancelaciones),
     * así una marca sobre una fecha sin clase no se cuenta. Si una fecha tiene varias marcas, vale la última.
     */
    fun summary(
        activityId: Long,
        held: List<Occurrence>,
        records: List<AttendanceRecord>,
        today: LocalDate,
        maxAbsences: Int?,
    ): AttendanceSummary {
        require(maxAbsences == null || maxAbsences >= 0)
        val sessions = held
            .filter { it.activityId == activityId && !it.start.toLocalDate().isAfter(today) }
            .map { it.start.toLocalDate() }
            .toSet()
        val marks = records
            .filter { it.activityId == activityId && it.date in sessions }
            .associate { it.date to it.mark }
        val absences = marks.values.count { it == AttendanceMark.FALTE }
        val attended = marks.values.count { it == AttendanceMark.ASISTI }
        val left = maxAbsences?.let { it - absences }
        return AttendanceSummary(
            attended = attended,
            absences = absences,
            unmarked = sessions.size - marks.size,
            maxAbsences = maxAbsences,
            absencesLeft = left,
            overLimit = left != null && left < 0,
        )
    }
}
