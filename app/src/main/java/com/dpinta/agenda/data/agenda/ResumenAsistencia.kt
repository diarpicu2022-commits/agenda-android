package com.dpinta.agenda.data.agenda

import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.Attendance
import com.dpinta.agenda.domain.AttendanceMark
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.SessionException
import com.dpinta.agenda.domain.WatchAttendance
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asistencia por materia con [Attendance.summary] desde el inicio del semestre (o 120 días) hasta hoy. La usan el reloj
 * (W6) y Actividades en el teléfono, así los dos cuentan igual. «Hoy no voy» cancela la sesión; en una clase con marca
 * «falté» esa sesión sí cuenta como sesión que hubo.
 */
@Singleton
class ResumenAsistencia @Inject constructor(private val repositorio: AgendaRepository) {

    suspend fun porMateria(
        agenda: Agenda,
        sesiones: (Agenda, LocalDate, LocalDate) -> List<Occurrence>,
        ahora: LocalDateTime,
    ): List<WatchAttendance> {
        val hoy = ahora.toLocalDate()
        val marcas = repositorio.asistencias().first()
        val faltas = marcas.filter { it.mark == AttendanceMark.FALTE }.map { it.activityId to it.date }.toSet()
        val sinMisFaltas = agenda.copy(excepciones = agenda.excepciones.filterNot { it is SessionException.Cancelled && (it.activityId to it.date) in faltas })
        val desde = agenda.semestreEn(hoy)?.start ?: hoy.minusDays(120)
        val hubo = sesiones(sinMisFaltas, desde, hoy)
        return agenda.actividades.values.filter { it.tipo == ActivityKind.CLASE || it.tipo == ActivityKind.EXAMEN }.mapNotNull { act ->
            val propias = hubo.filter { it.activityId == act.id }
            if (propias.none { !it.start.isAfter(ahora) }) return@mapNotNull null
            val r = Attendance.summary(act.id, propias, marcas, hoy, act.faltasMax)
            val pendiente = propias.firstOrNull { it.start.toLocalDate() == hoy && !it.end.isAfter(ahora) &&
                marcas.none { m -> m.activityId == act.id && m.date == hoy } }
            WatchAttendance(act.id, act.titulo, r.attended, r.absences, r.maxAbsences, pendiente?.start)
        }.sortedBy { it.title }
    }
}
