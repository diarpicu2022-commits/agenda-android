package com.dpinta.agenda.domain.ics

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Resultado de leer un `.ics`: lo que entró y cuántos eventos se saltaron por estar incompletos. */
data class IcsImport(val events: List<IcsEvent>, val skipped: Int)

/**
 * Lector de iCalendar (RFC 5545) para el horario de la universidad y Google Calendar.
 * Entiende VEVENT con DTSTART, DTEND o DURATION, SUMMARY, LOCATION, DESCRIPTION, RRULE semanal
 * (BYDAY, UNTIL, COUNT), EXDATE y RECURRENCE-ID. Lo demás se ignora sin fallar.
 */
object IcsReader {

    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")

    private data class Prop(val name: String, val params: Map<String, String>, val value: String)

    /** @param zone zona del teléfono: todas las horas se devuelven en ella. */
    fun read(text: String, zone: ZoneId): IcsImport {
        val events = mutableListOf<IcsEvent>()
        var skipped = 0
        var current: MutableList<Prop>? = null
        var depth = 0 // VALARM y otros componentes anidados dentro del VEVENT no se mezclan
        for (line in unfold(text)) {
            val p = parseLine(line) ?: continue
            when {
                p.name == "BEGIN" && p.value.equals("VEVENT", true) -> { current = mutableListOf(); depth = 0 }
                p.name == "END" && p.value.equals("VEVENT", true) -> {
                    val props = current ?: continue
                    val e = runCatching { toEvent(props, zone) }.getOrNull()
                    if (e == null) skipped++ else events += e
                    current = null
                }
                current == null -> Unit
                p.name == "BEGIN" -> depth++
                p.name == "END" -> depth--
                depth == 0 -> current.add(p)
            }
        }
        return IcsImport(events, skipped)
    }

    /** RFC 5545 §3.1: una línea que empieza por espacio o tabulador continúa la anterior. */
    internal fun unfold(text: String): List<String> {
        val out = mutableListOf<StringBuilder>()
        for (raw in text.split("\r\n", "\n", "\r")) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && out.isNotEmpty()) out.last().append(raw, 1, raw.length)
            else if (raw.isNotEmpty()) out += StringBuilder(raw)
        }
        return out.map { it.toString() }
    }

    private fun parseLine(line: String): Prop? {
        var inQuotes = false
        var colon = -1
        for ((i, c) in line.withIndex()) {
            if (c == '"') inQuotes = !inQuotes
            if (c == ':' && !inQuotes) { colon = i; break }
        }
        if (colon <= 0) return null
        val head = line.substring(0, colon).split(';')
        val params = head.drop(1).mapNotNull { param ->
            val eq = param.indexOf('=')
            if (eq <= 0) null else param.substring(0, eq).uppercase() to param.substring(eq + 1).trim('"')
        }.toMap()
        return Prop(head[0].uppercase(), params, line.substring(colon + 1))
    }

    private fun toEvent(props: List<Prop>, zone: ZoneId): IcsEvent? {
        fun one(name: String) = props.firstOrNull { it.name == name }
        val dtStart = one("DTSTART") ?: return null
        val allDay = dtStart.params["VALUE"] == "DATE" || dtStart.value.length == 8
        val start = time(dtStart, zone)
        val end = one("DTEND")?.let { time(it, zone) }
            ?: one("DURATION")?.let { start + duration(it.value) }
            ?: if (allDay) start.plusDays(1) else start
        if (end.isBefore(start)) return null

        var weekly: IcsWeekly? = null
        var unsupported: String? = null
        one("RRULE")?.let { r ->
            weekly = weekly(r.value, start, zone)
            if (weekly == null) unsupported = r.value
        }
        val excluded = props.filter { it.name == "EXDATE" }
            .flatMap { p -> p.value.split(',').filter { it.isNotBlank() }.map { time(p.copy(value = it.trim()), zone).toLocalDate() } }
            .toSet()

        return IcsEvent(
            uid = one("UID")?.value?.trim().orEmpty(),
            summary = one("SUMMARY")?.value?.let(::unescape)?.trim().orEmpty(),
            location = one("LOCATION")?.value?.let(::unescape)?.trim()?.takeIf { it.isNotEmpty() },
            description = one("DESCRIPTION")?.value?.let(::unescape)?.trim()?.takeIf { it.isNotEmpty() },
            start = start,
            end = end,
            allDay = allDay,
            weekly = weekly,
            excluded = excluded,
            recurrenceId = one("RECURRENCE-ID")?.let { time(it, zone) },
            unsupportedRule = unsupported,
        )
    }

    /** Hora local de [zone]. Acepta UTC («Z»), TZID y hora flotante (que se toma como de [zone]). */
    private fun time(p: Prop, zone: ZoneId): LocalDateTime {
        val v = p.value.trim()
        if (v.length == 8) return LocalDate.parse(v, DATE).atStartOfDay()
        if (v.endsWith("Z")) {
            return LocalDateTime.parse(v.dropLast(1), DATE_TIME).atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDateTime()
        }
        val local = LocalDateTime.parse(v, DATE_TIME)
        val source = p.params["TZID"]?.let { id -> runCatching { ZoneId.of(id) }.getOrNull() } ?: return local
        return local.atZone(source).withZoneSameInstant(zone).toLocalDateTime()
    }

    /** DURATION de iCalendar: P1W, P1D, PT1H30M… */
    private fun duration(v: String): Duration {
        val weeks = Regex("^([+-]?)P(\\d+)W$").find(v.trim())
        if (weeks != null) return Duration.ofDays(7L * weeks.groupValues[2].toLong())
        return Duration.parse(v.trim())
    }

    /** Solo FREQ=WEEKLY con intervalo 1; cualquier otra regla devuelve null. */
    private fun weekly(rule: String, start: LocalDateTime, zone: ZoneId): IcsWeekly? {
        val parts = rule.split(';').mapNotNull { kv ->
            val eq = kv.indexOf('=')
            if (eq <= 0) null else kv.substring(0, eq).uppercase() to kv.substring(eq + 1)
        }.toMap()
        if (parts["FREQ"]?.uppercase() != "WEEKLY") return null
        if ((parts["INTERVAL"] ?: "1") != "1") return null
        val days = parts["BYDAY"]?.split(',')?.map { d -> ICS_DAYS[d.trim().uppercase()] ?: return null }?.toSet()
            ?: setOf(start.dayOfWeek)
        if (days.isEmpty()) return null
        val until = parts["UNTIL"]?.let { u ->
            try {
                time(Prop("UNTIL", emptyMap(), u), zone).toLocalDate()
            } catch (_: DateTimeException) {
                return null
            }
        } ?: parts["COUNT"]?.toIntOrNull()?.let { lastOfCount(start.toLocalDate(), days, it) ?: return null }
        return IcsWeekly(days, until)
    }

    /** Fecha de la sesión número [count] (DTSTART cuenta como la primera, como dice el RFC). */
    private fun lastOfCount(first: LocalDate, days: Set<DayOfWeek>, count: Int): LocalDate? {
        if (count < 1) return null
        var n = 1
        var d = first
        while (n < count) {
            d = d.plusDays(1)
            if (d.dayOfWeek in days) n++
        }
        return d
    }

    private fun unescape(v: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < v.length) {
            val c = v[i]
            if (c == '\\' && i + 1 < v.length) {
                sb.append(if (v[i + 1] == 'n' || v[i + 1] == 'N') '\n' else v[i + 1])
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
