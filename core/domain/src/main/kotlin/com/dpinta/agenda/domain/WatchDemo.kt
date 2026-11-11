package com.dpinta.agenda.domain

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Día de ejemplo para el modo demostración de CampusWatch (anexo 2026-11-10-campuswatch-vivo, §6). Sirve para enseñar
 * la app en un reloj sin teléfono emparejado. Todo es relativo a [now], igual que los escenarios de
 * `tools/verificacion/reloj_dia.py`, para que siempre haya una próxima clase a punto de empezar.
 *
 * Qué se ve: la próxima clase a pie con «Tu ritmo», un turno en bus, una entrega, una clase ya terminada que pregunta
 * «¿Fuiste?», mañana una clase y la asistencia con una materia cerca del límite y otra en el límite.
 */
object WatchDemo {
    fun day(now: LocalDateTime, english: Boolean = false): WatchDay {
        val base = now.truncatedTo(ChronoUnit.MINUTES)
        fun m(min: Long) = base.plusMinutes(min)
        fun t(es: String, en: String) = if (english) en else es
        val calculo = t("Cálculo diferencial", "Calculus I")
        val programacion = t("Programación", "Programming")
        val fisica = t("Física mecánica", "Physics I")
        val campus = "Campus"
        val sesiones = listOf(
            WatchSession(1, m(-170), m(-120), t("Desayuno", "Breakfast"), "", t("Casa", "Home"), null, WatchSession.Kind.PERSONAL, false),
            WatchSession(2, m(-100), m(-10), calculo, "B-204", campus, m(-130), WatchSession.Kind.CLASE, false, TransportMode.A_PIE),
            WatchSession(3, m(18), m(108), programacion, "Lab 3", campus, m(8), WatchSession.Kind.CLASE, false, TransportMode.A_PIE),
            WatchSession(4, m(150), m(270), t("Turno", "Shift"), "", t("Tienda centro", "Downtown store"), m(120), WatchSession.Kind.TRABAJO, false, TransportMode.TRANSPORTE_PUBLICO),
            WatchSession(5, m(600), m(600), t("Informe de Física", "Physics report"), "", "", null, WatchSession.Kind.ENTREGA, false),
            WatchSession(6, m(24 * 60 - 60), m(24 * 60 + 30), fisica, t("Aula 105", "Room 105"), campus, m(24 * 60 - 95), WatchSession.Kind.CLASE, false, TransportMode.A_PIE),
        )
        val asistencia = listOf(
            WatchAttendance(2, calculo, 10, 2, 3, m(-100)),
            WatchAttendance(3, programacion, 12, 0, null, null),
            WatchAttendance(6, fisica, 7, 4, 4, null),
        )
        val ritmo = listOf(WatchRhythm(3, m(18), 6, 9))
        return WatchDay(base, sesiones, asistencia, ritmo)
    }
}
