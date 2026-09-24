package com.dpinta.agenda.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class Holiday(val date: LocalDate, val name: String)

/**
 * Festivos de Colombia (Ley 51 de 1983, «Ley Emiliani»). Sirven para llenar [Semester.daysOff]:
 * las clases no se generan en festivo (arquitectura §6 P1.2).
 */
object ColombianHolidays {

    fun of(year: Int): List<Holiday> {
        require(year >= 1984) { "La Ley Emiliani rige desde 1984" }
        val easter = easterSunday(year)
        fun day(month: Int, dayOfMonth: Int) = LocalDate.of(year, month, dayOfMonth)
        return listOf(
            // Fijos: no se trasladan.
            Holiday(day(1, 1), "Año Nuevo"),
            Holiday(easter.minusDays(3), "Jueves Santo"),
            Holiday(easter.minusDays(2), "Viernes Santo"),
            Holiday(day(5, 1), "Día del Trabajo"),
            Holiday(day(7, 20), "Día de la Independencia"),
            Holiday(day(8, 7), "Batalla de Boyacá"),
            Holiday(day(12, 8), "Inmaculada Concepción"),
            Holiday(day(12, 25), "Navidad"),
            // Se trasladan al lunes siguiente si no caen en lunes.
            Holiday(nextMonday(day(1, 6)), "Reyes Magos"),
            Holiday(nextMonday(day(3, 19)), "San José"),
            Holiday(nextMonday(easter.plusDays(39)), "Ascensión del Señor"),
            Holiday(nextMonday(easter.plusDays(60)), "Corpus Christi"),
            Holiday(nextMonday(easter.plusDays(68)), "Sagrado Corazón"),
            Holiday(nextMonday(day(6, 29)), "San Pedro y San Pablo"),
            Holiday(nextMonday(day(8, 15)), "Asunción de la Virgen"),
            Holiday(nextMonday(day(10, 12)), "Día de la Raza"),
            Holiday(nextMonday(day(11, 1)), "Todos los Santos"),
            Holiday(nextMonday(day(11, 11)), "Independencia de Cartagena"),
        ).sortedBy { it.date }
    }

    /** Fechas festivas entre [from] y [to], ambas incluidas. */
    fun between(from: LocalDate, to: LocalDate): Set<LocalDate> =
        (from.year..to.year)
            .flatMap { of(it) }
            .map { it.date }
            .filterTo(sortedSetOf()) { !it.isBefore(from) && !it.isAfter(to) }

    /** Domingo de Pascua (calendario gregoriano, algoritmo de Meeus/Jones/Butcher). */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    private fun nextMonday(date: LocalDate): LocalDate = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
}
