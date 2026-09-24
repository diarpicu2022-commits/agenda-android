package com.dpinta.agenda.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceTest {

    private val mon = LocalDate.of(2026, 9, 7)
    private val held = (0L until 4L).map { w ->
        val d = mon.plusWeeks(w)
        Occurrence(1, 10, d.atTime(8, 0), d.atTime(10, 0))
    } + Occurrence(2, 10, mon.atTime(10, 0), mon.atTime(12, 0))

    @Test fun `cuenta faltas y avisa al pasarse del limite`() {
        val records = listOf(
            AttendanceRecord(1, mon, AttendanceMark.FALTE),
            AttendanceRecord(1, mon.plusWeeks(1), AttendanceMark.ASISTI),
            AttendanceRecord(1, mon.plusWeeks(2), AttendanceMark.FALTE),
            AttendanceRecord(2, mon, AttendanceMark.FALTE), // otra materia
        )
        val s = Attendance.summary(1, held, records, today = mon.plusWeeks(3), maxAbsences = 2)
        assertEquals(2, s.absences)
        assertEquals(1, s.attended)
        assertEquals(1, s.unmarked)
        assertEquals(0, s.absencesLeft)
        assertTrue(!s.overLimit)

        val more = Attendance.summary(1, held, records + AttendanceRecord(1, mon.plusWeeks(3), AttendanceMark.FALTE), mon.plusWeeks(3), 2)
        assertEquals(-1, more.absencesLeft)
        assertTrue(more.overLimit)
    }

    @Test fun `la ultima marca de una fecha manda y las fechas sin clase no cuentan`() {
        val records = listOf(
            AttendanceRecord(1, mon, AttendanceMark.FALTE),
            AttendanceRecord(1, mon, AttendanceMark.ASISTI), // corrigió
            AttendanceRecord(1, mon.plusDays(1), AttendanceMark.FALTE), // martes: no hubo clase
        )
        val s = Attendance.summary(1, held, records, today = mon, maxAbsences = null)
        assertEquals(0, s.absences)
        assertEquals(1, s.attended)
        assertNull(s.absencesLeft)
    }
}
