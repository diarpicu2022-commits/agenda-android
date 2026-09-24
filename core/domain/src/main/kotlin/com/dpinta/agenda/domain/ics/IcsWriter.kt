package com.dpinta.agenda.domain.ics

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Escribe un `.ics` (RFC 5545) que abren Google Calendar y el calendario del teléfono. */
object IcsWriter {

    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
    private const val CRLF = "\r\n"

    /** @param stamp instante de la exportación (DTSTAMP), del reloj inyectado. */
    fun write(events: List<IcsEvent>, zone: ZoneId, stamp: Instant): String {
        val sb = StringBuilder()
        fun line(s: String) = sb.append(fold(s)).append(CRLF)
        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//dpinta//Agenda//ES")
        line("CALSCALE:GREGORIAN")
        timezone(zone, stamp)?.forEach(::line)
        val tz = zone.id
        val dtStamp = DATE_TIME.format(stamp.atOffset(ZoneOffset.UTC)) + "Z"
        for (e in events) {
            line("BEGIN:VEVENT")
            line("UID:${e.uid}")
            line("DTSTAMP:$dtStamp")
            if (e.allDay) {
                line("DTSTART;VALUE=DATE:${DATE.format(e.start)}")
                line("DTEND;VALUE=DATE:${DATE.format(e.end)}")
            } else {
                line("DTSTART;TZID=$tz:${DATE_TIME.format(e.start)}")
                line("DTEND;TZID=$tz:${DATE_TIME.format(e.end)}")
            }
            e.recurrenceId?.let { line("RECURRENCE-ID;TZID=$tz:${DATE_TIME.format(it)}") }
            line("SUMMARY:${escape(e.summary)}")
            e.location?.let { line("LOCATION:${escape(it)}") }
            e.description?.let { line("DESCRIPTION:${escape(it)}") }
            e.weekly?.let { w ->
                val days = w.days.sorted().joinToString(",") { d -> ICS_DAYS.entries.first { it.value == d }.key }
                // Con DTSTART en TZID, UNTIL tiene que ir en UTC (RFC 5545 §3.3.10).
                val until = w.until?.let { u ->
                    ";UNTIL=" + DATE_TIME.format(u.atTime(23, 59, 59).atZone(zone).withZoneSameInstant(ZoneOffset.UTC)) + "Z"
                }.orEmpty()
                line("RRULE:FREQ=WEEKLY;BYDAY=$days$until")
            }
            e.excluded.sorted().forEach { d ->
                line("EXDATE;TZID=$tz:${DATE_TIME.format(LocalDateTime.of(d, e.start.toLocalTime()))}")
            }
            line("END:VEVENT")
        }
        line("END:VCALENDAR")
        return sb.toString()
    }

    /** VTIMEZONE solo para zonas sin cambios de horario por delante (Colombia); las demás las resuelve el calendario por su id. */
    private fun timezone(zone: ZoneId, at: Instant): List<String>? {
        val rules = zone.rules
        if (rules.nextTransition(at) != null) return null
        val offset = rules.getOffset(at)
        val o = offset.id.replace(":", "").let { if (it == "Z") "+0000" else it }
        return listOf(
            "BEGIN:VTIMEZONE", "TZID:${zone.id}",
            "BEGIN:STANDARD", "DTSTART:19700101T000000", "TZOFFSETFROM:$o", "TZOFFSETTO:$o", "END:STANDARD",
            "END:VTIMEZONE",
        )
    }

    private fun escape(v: String) =
        v.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n")

    /** Líneas de máximo 75 octetos UTF-8, sin partir un carácter por la mitad. */
    internal fun fold(line: String): String {
        val sb = StringBuilder()
        var octets = 0
        var limit = 75
        var i = 0
        while (i < line.length) {
            val cp = line.codePointAt(i)
            val chars = Character.charCount(cp)
            val size = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
            if (octets + size > limit) {
                sb.append(CRLF).append(' ')
                octets = 0
                limit = 74 // el espacio inicial ya ocupa un octeto
            }
            sb.appendCodePoint(cp)
            octets += size
            i += chars
        }
        return sb.toString()
    }
}
