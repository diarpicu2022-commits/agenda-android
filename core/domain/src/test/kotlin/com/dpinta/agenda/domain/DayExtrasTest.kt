package com.dpinta.agenda.domain

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayExtrasTest {

    private val day = LocalDate.of(2026, 9, 24)
    private val bogota = ZoneId.of("America/Bogota")
    private fun occ(id: Long, place: Long?, h1: Int, m1: Int, h2: Int, m2: Int) =
        Occurrence(id, place, day.atTime(h1, m1), day.atTime(h2, m2))

    @Test fun `modo clase une sesiones seguidas y deja fuera lo que no silencia`() {
        val w = QuietMode.windows(
            listOf(occ(2, 10, 10, 0, 12, 0), occ(1, 10, 8, 0, 10, 0), occ(3, 20, 14, 0, 18, 0), occ(9, null, 12, 0, 13, 0), occ(4, 20, 15, 0, 16, 0)),
        ) { it != 9L }
        assertEquals(
            listOf(QuietWindow(day.atTime(8, 0), day.atTime(12, 0)), QuietWindow(day.atTime(14, 0), day.atTime(18, 0))),
            w,
        )
    }

    private fun task(title: String, dueIn: Long, exam: Boolean = false, done: Boolean = false, notice: Int = 3) =
        AgendaTask(title.hashCode().toLong(), title, 1, day.plusDays(dueIn), done, exam, notice)

    @Test fun `estados de tareas y orden de urgencia`() {
        val list = listOf(
            task("Taller 3", 3), task("Parcial 2", 3, exam = true), task("Informe", 0), task("Lectura", -1),
            task("Quiz", 4), task("Ensayo", 1, done = true), task("Proyecto", 10, notice = 14),
        )
        val t = TaskReminders.today(list, day)
        assertEquals(listOf("Lectura", "Informe", "Parcial 2", "Taller 3", "Proyecto"), t.map { it.task.title })
        assertEquals(listOf(TaskState.VENCIDA, TaskState.HOY, TaskState.AVISO, TaskState.AVISO, TaskState.AVISO), t.map { it.state })
        assertEquals(TaskState.PENDIENTE, TaskReminders.status(list[4], day).state)
        assertEquals(TaskState.HECHA, TaskReminders.status(list[5], day).state)
    }

    @Test fun `resumen matutino con la primera salida y las horas de trabajo`() {
        val sessions = listOf(occ(3, 20, 14, 0, 18, 0), occ(9, null, 7, 0, 7, 30), occ(1, 10, 8, 0, 10, 0), occ(2, 10, 10, 0, 12, 0))
        val tomorrow = Occurrence(1, 10, day.plusDays(1).atTime(8, 0), day.plusDays(1).atTime(10, 0))
        val now = day.atTime(5, 0).atZone(bogota).toInstant()
        val b = MorningBriefing.of(
            date = day, occurrences = sessions + tomorrow, zone = bogota, now = now,
            marginOf = { Duration.ofMinutes(10) },
            travelOf = { TravelEstimate(Duration.ofMinutes(40), TransportMode.TRANSPORTE_PUBLICO, now, false) },
            isWork = { it == 3L },
            tasks = listOf(task("Parcial 2", 2, exam = true)),
        )
        assertEquals(listOf(9L, 1L, 2L, 3L), b.sessions.map { it.activityId })
        assertEquals(1L, b.firstDepartureFor!!.activityId) // la llamada de las 7:00 no tiene lugar
        assertEquals(day.atTime(7, 10).atZone(bogota).toInstant(), b.firstDeparture)
        assertEquals(Duration.ofHours(4), b.workToday)
        assertEquals(1, b.tasks.size)
    }

    @Test fun `sin estimacion no se inventa la hora de salida`() {
        val b = MorningBriefing.of(day, listOf(occ(1, 10, 8, 0, 10, 0)), bogota, day.atTime(5, 0).atZone(bogota).toInstant(),
            { Duration.ZERO }, { null }, { false }, emptyList())
        assertNull(b.firstDeparture)
    }
}
