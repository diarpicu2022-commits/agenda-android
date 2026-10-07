package com.dpinta.agenda.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Niveles de urgencia (sistema de diseño, 02-urgencia.md). Describen cuánto tiempo queda; los comparten
 * Agenda y CampusWatch (vocabulario, color e ícono iguales).
 */
enum class Urgency { CALM, UPCOMING, SOON, LEAVE_NOW, URGENT, MISSED }

object UrgencyRules {
    /** Umbrales por defecto (configurables en Ajustes › Avisos según el sistema). */
    val CALM_ABOVE: Duration = Duration.ofMinutes(60)
    val SOON_AT: Duration = Duration.ofMinutes(30)
    val URGENT_AFTER: Duration = Duration.ofMinutes(5)
    val EXAM_URGENT_WITHIN: Duration = Duration.ofMinutes(60)

    /**
     * @param leaveAt hora de salida si hay trayecto; si no, cuenta hasta [start].
     * @param leftAt cuándo tocó «Ya voy»; con ella ya no sube a Urgente.
     * @param exam examen con prioridad alta: sube un nivel como máximo (Salir ahora → Urgente) y nunca salta de Calma a Urgente.
     */
    fun level(now: Instant, start: Instant, leaveAt: Instant?, exam: Boolean = false, leftAt: Instant? = null): Urgency {
        // Ya empezó: si salió, va en camino (sin alarma); si no, se perdió.
        if (!now.isBefore(start)) return if (leftAt != null) Urgency.UPCOMING else Urgency.MISSED
        val target = leaveAt ?: start
        val left = Duration.between(now, target)
        val base = when {
            leaveAt != null && leftAt == null && !now.isBefore(leaveAt + URGENT_AFTER) -> Urgency.URGENT
            !left.isNegative && left > CALM_ABOVE -> Urgency.CALM
            left > SOON_AT -> Urgency.UPCOMING
            left > Duration.ZERO -> Urgency.SOON
            leftAt != null -> Urgency.UPCOMING
            else -> Urgency.LEAVE_NOW
        }
        // Examen a menos de 60 min: al llegar la hora de salir pasa directo a Urgente (un nivel, nunca antes de tiempo:
        // subir «Pronto» a «Salir ahora» diría «Es hora de salir» media hora antes). Hasta entonces se distingue por el color ciruela.
        val examNear = exam && Duration.between(now, start) <= EXAM_URGENT_WITHIN
        return if (examNear && base == Urgency.LEAVE_NOW) Urgency.URGENT else base
    }

}

/** Una sesión tal como la ve el reloj: lo mínimo para «qué, dónde, cuándo, qué tan urgente y si hay que salir». */
data class WatchSession(
    val activityId: Long,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val title: String,
    /** Aula o salón; vacío si no hay. */
    val room: String,
    val place: String,
    /** Hora de salida si hay trayecto; null si empieza donde ya estás. */
    val leaveAt: LocalDateTime?,
    val kind: Kind,
    val exam: Boolean,
) {
    enum class Kind { CLASE, TRABAJO, PERSONAL, ENTREGA }

    fun urgency(now: LocalDateTime, zone: ZoneId, leftAt: Instant? = null): Urgency =
        UrgencyRules.level(now.atZone(zone).toInstant(), start.atZone(zone).toInstant(), leaveAt?.atZone(zone)?.toInstant(), exam, leftAt)
}

/** Lo que el teléfono deja en el reloj: las sesiones de hoy y mañana, y cuándo se calculó (pie «Actualizado 07:12»). */
data class WatchDay(val computedAt: LocalDateTime, val sessions: List<WatchSession>) {

    /** La siguiente sesión que todavía no ha empezado (igual que [DayPlanner.next] en el teléfono). */
    fun next(now: LocalDateTime): WatchSession? = sessions.sortedBy { it.start }.firstOrNull { it.start.isAfter(now) }

    /** La que está en curso ahora, si hay una. */
    fun current(now: LocalDateTime): WatchSession? = sessions.firstOrNull { !it.start.isAfter(now) && it.end.isAfter(now) }

    fun on(date: java.time.LocalDate): List<WatchSession> = sessions.filter { it.start.toLocalDate() == date }.sortedBy { it.start }

    /**
     * Formato de intercambio teléfono → reloj: una línea por sesión, campos separados por tabulador y escapados.
     * Simple a propósito (sin dependencias en el dominio) y probado ida y vuelta.
     */
    fun encode(): String = buildString {
        append(VERSION).append('\t').append(computedAt).append('\n')
        for (s in sessions) {
            append(listOf(s.activityId.toString(), s.start.toString(), s.end.toString(), s.title, s.room, s.place,
                s.leaveAt?.toString().orEmpty(), s.kind.name, if (s.exam) "1" else "0").joinToString("\t") { esc(it) }).append('\n')
        }
    }

    companion object {
        const val VERSION = "cw1"

        fun decode(text: String): WatchDay? = runCatching {
            val lines = text.split('\n').filter { it.isNotEmpty() }
            val head = lines.first().split('\t')
            require(head[0] == VERSION)
            WatchDay(LocalDateTime.parse(head[1]), lines.drop(1).map { line ->
                val f = line.split('\t').map(::unesc)
                WatchSession(f[0].toLong(), LocalDateTime.parse(f[1]), LocalDateTime.parse(f[2]), f[3], f[4], f[5],
                    f[6].takeIf { it.isNotEmpty() }?.let(LocalDateTime::parse), WatchSession.Kind.valueOf(f[7]), f[8] == "1")
            })
        }.getOrNull()

        private fun esc(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")
        private fun unesc(s: String): String {
            val out = StringBuilder()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    out.append(when (s[i + 1]) { 't' -> '\t'; 'n' -> '\n'; else -> s[i + 1] }); i += 2
                } else { out.append(c); i++ }
            }
            return out.toString()
        }
    }
}
