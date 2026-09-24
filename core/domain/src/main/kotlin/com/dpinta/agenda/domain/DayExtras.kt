package com.dpinta.agenda.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Un tramo en el que el teléfono va en silencio. */
data class QuietWindow(val start: LocalDateTime, val end: LocalDateTime)

/** Arquitectura §6 P2.6 · Modo clase: No Molestar durante clases y trabajo. */
object QuietMode {

    /**
     * Tramos de silencio para las sesiones que [isQuiet] marca. Dos sesiones que se tocan o se
     * cruzan dan un solo tramo, para no apagar y encender el silencio entre una y otra.
     */
    fun windows(occurrences: List<Occurrence>, isQuiet: (activityId: Long) -> Boolean): List<QuietWindow> {
        val out = mutableListOf<QuietWindow>()
        for (o in occurrences.filter { isQuiet(it.activityId) }.sortedBy { it.start }) {
            val last = out.lastOrNull()
            if (last != null && !o.start.isAfter(last.end)) {
                if (o.end.isAfter(last.end)) out[out.lastIndex] = last.copy(end = o.end)
            } else {
                out += QuietWindow(o.start, o.end)
            }
        }
        return out
    }
}

data class AgendaTask(
    val id: Long,
    val title: String,
    /** Materia a la que pertenece, si tiene. */
    val activityId: Long?,
    val due: LocalDate,
    val done: Boolean,
    val isExam: Boolean,
    /** Con cuántos días de anticipación avisar (además del mismo día). */
    val noticeDays: Int,
) {
    init {
        require(noticeDays >= 0)
    }
}

enum class TaskState { PENDIENTE, AVISO, HOY, VENCIDA, HECHA }

data class TaskStatus(val task: AgendaTask, val state: TaskState, val daysLeft: Long)

/** Arquitectura §6 P2.7 · Tareas y exámenes ligados a una materia, con aviso días antes. */
object TaskReminders {

    fun status(task: AgendaTask, today: LocalDate): TaskStatus {
        val days = ChronoUnit.DAYS.between(today, task.due)
        val state = when {
            task.done -> TaskState.HECHA
            days < 0 -> TaskState.VENCIDA
            days == 0L -> TaskState.HOY
            days <= task.noticeDays -> TaskState.AVISO
            else -> TaskState.PENDIENTE
        }
        return TaskStatus(task, state, days)
    }

    /** Lo que hay que mostrar o avisar hoy: vencidas, de hoy y en aviso; primero lo más urgente y los exámenes. */
    fun today(tasks: List<AgendaTask>, today: LocalDate): List<TaskStatus> =
        tasks.map { status(it, today) }
            .filter { it.state == TaskState.VENCIDA || it.state == TaskState.HOY || it.state == TaskState.AVISO }
            .sortedWith(compareBy<TaskStatus> { it.daysLeft }.thenByDescending { it.task.isExam }.thenBy { it.task.title })
}

data class MorningBrief(
    val date: LocalDate,
    /** Sesiones del día en orden. */
    val sessions: List<Occurrence>,
    /** Hora de la primera salida que exige traslado, si ya hay estimación. */
    val firstDeparture: Instant?,
    /** La sesión a la que corresponde [firstDeparture]. */
    val firstDepartureFor: Occurrence?,
    val tasks: List<TaskStatus>,
    val workToday: Duration,
)

/** Arquitectura §6 P2.8 · Resumen matutino: el día completo y la primera salida. */
object MorningBriefing {

    fun of(
        date: LocalDate,
        occurrences: List<Occurrence>,
        zone: ZoneId,
        now: Instant,
        marginOf: (activityId: Long) -> Duration,
        travelOf: (Occurrence) -> TravelEstimate?,
        isWork: (activityId: Long) -> Boolean,
        tasks: List<AgendaTask>,
    ): MorningBrief {
        val day = occurrences.filter { it.start.toLocalDate() == date }.sortedBy { it.start }
        var departure: Instant? = null
        var departureFor: Occurrence? = null
        for (o in day) {
            if (!DayPlanner.needsTravel(DayPlanner.previous(day, o), o)) continue
            val travel = travelOf(o) ?: continue
            val start = o.start.atZone(zone).toInstant()
            departure = DepartureCalculator.plan(start, marginOf(o.activityId), travel, now).leaveAt
            departureFor = o
            break
        }
        return MorningBrief(
            date = date,
            sessions = day,
            firstDeparture = departure,
            firstDepartureFor = departureFor,
            tasks = TaskReminders.today(tasks, date),
            workToday = WorkedHours.total(day, isWork, DateRange(date, date)),
        )
    }
}
