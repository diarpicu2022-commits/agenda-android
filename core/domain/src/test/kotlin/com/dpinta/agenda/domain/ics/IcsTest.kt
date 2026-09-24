package com.dpinta.agenda.domain.ics

import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsTest {

    private val bogota = ZoneId.of("America/Bogota")

    private val universidad = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Universidad//Horario//ES
        BEGIN:VEVENT
        UID:calculo-1@u.edu.co
        DTSTART;TZID=America/Bogota:20260803T080000
        DTEND;TZID=America/Bogota:20260803T100000
        RRULE:FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20261128T045959Z
        EXDATE;TZID=America/Bogota:20260817T080000,20261012T080000
        SUMMARY:Cálculo integral\, grupo 3
        LOCATION:Bloque B - Aula 204
        DESCRIPTION:Profesora Ruiz\nTraer calculadora
        BEGIN:VALARM
        TRIGGER:-PT15M
        DESCRIPTION:no es del evento
        END:VALARM
        END:VEVENT
        BEGIN:VEVENT
        UID:fisica@u.edu.co
        DTSTART:20260806T130000Z
        DURATION:PT1H30M
        RRULE:FREQ=WEEKLY;COUNT=3
        SUMMARY:Física
        END:VEVENT
        BEGIN:VEVENT
        UID:mensual@u.edu.co
        DTSTART:20260910T140000
        DTEND:20260910T150000
        RRULE:FREQ=MONTHLY;BYMONTHDAY=10
        SUMMARY:Tutoría
        END:VEVENT
        BEGIN:VEVENT
        UID:roto@u.edu.co
        SUMMARY:Sin fecha
        END:VEVENT
        END:VCALENDAR
    """.trimIndent().replace("\n", "\r\n")

    @Test fun `lee una serie semanal con excepciones, texto escapado y alarma anidada`() {
        val r = IcsReader.read(universidad, bogota)
        assertEquals(1, r.skipped) // el evento sin DTSTART
        val c = r.events.first { it.uid == "calculo-1@u.edu.co" }
        assertEquals("Cálculo integral, grupo 3", c.summary)
        assertEquals("Bloque B - Aula 204", c.location)
        assertEquals("Profesora Ruiz\nTraer calculadora", c.description)
        assertEquals(LocalDate.of(2026, 8, 3).atTime(8, 0), c.start)
        assertEquals(IcsWeekly(setOf(MONDAY, WEDNESDAY), LocalDate.of(2026, 11, 27)), c.weekly)
        assertEquals(setOf(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 10, 12)), c.excluded)

        val rule = c.toWeeklyRule(activityId = 7, fallbackUntil = LocalDate.of(2026, 12, 31))!!
        assertEquals(LocalDate.of(2026, 11, 27), rule.until)
        assertEquals(java.time.LocalTime.of(10, 0), rule.end)
    }

    @Test fun `UTC pasa a hora de Colombia, DURATION y COUNT`() {
        val f = IcsReader.read(universidad, bogota).events.first { it.uid == "fisica@u.edu.co" }
        assertEquals(LocalDate.of(2026, 8, 6).atTime(8, 0), f.start) // 13:00 Z = 8:00 en Bogotá
        assertEquals(LocalDate.of(2026, 8, 6).atTime(9, 30), f.end)
        // Sin BYDAY se repite el día de DTSTART (jueves); la tercera sesión es el 20 de agosto.
        assertEquals(IcsWeekly(setOf(THURSDAY), LocalDate.of(2026, 8, 20)), f.weekly)
    }

    @Test fun `una regla que no es semanal se conserva para avisar y no se inventa la serie`() {
        val t = IcsReader.read(universidad, bogota).events.first { it.uid == "mensual@u.edu.co" }
        assertNull(t.weekly)
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=10", t.unsupportedRule)
        assertNull(t.toWeeklyRule(1, LocalDate.of(2026, 12, 31)))
    }

    @Test fun `lineas plegadas se unen`() {
        val text = "BEGIN:VEVENT\r\nUID:x\r\nDTSTART:20260901T070000\r\nSUMMARY:Laboratorio de \r\n químic\r\n\ta\r\nEND:VEVENT\r\n"
        assertEquals("Laboratorio de química", IcsReader.read(text, bogota).events.single().summary)
    }

    @Test fun `exportar y volver a leer da lo mismo`() {
        val original = IcsReader.read(universidad, bogota).events.filter { it.unsupportedRule == null }
        val ics = IcsWriter.write(original, bogota, Instant.parse("2026-09-24T12:00:00Z"))
        assertTrue(ics.lines().all { it.removeSuffix("\r").toByteArray().size <= 75 })
        assertTrue(ics.contains("BEGIN:VTIMEZONE\r\nTZID:America/Bogota"))
        assertTrue(ics.contains("TZOFFSETTO:-0500"))
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20261128T045959Z"))
        assertEquals(original, IcsReader.read(ics, bogota).events)
    }

    @Test fun `el plegado no parte caracteres de varios octetos`() {
        val folded = IcsWriter.fold("SUMMARY:" + "á".repeat(80))
        val lines = folded.split("\r\n")
        assertTrue(lines.all { it.toByteArray().size <= 75 })
        assertEquals("SUMMARY:" + "á".repeat(80), IcsReader.unfold(folded).single())
    }
}
