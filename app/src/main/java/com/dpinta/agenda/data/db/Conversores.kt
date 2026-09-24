package com.dpinta.agenda.data.db

import androidx.room.TypeConverter
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.TransportMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Tipos de java.time y enums a columnas de SQLite.
 * - LocalDate → día de época (INTEGER): ordena y compara bien.
 * - LocalTime → segundo del día (INTEGER).
 * - LocalDateTime → texto ISO («2026-09-24T08:00»): hora local de pared, ordenable.
 * - Instant → milisegundos de época (INTEGER).
 * - Días de la semana → máscara de bits (lunes = 1, domingo = 64).
 * - Enums → su nombre (TEXT): legible y estable aunque cambie el orden de declaración.
 */
class Conversores {
    @TypeConverter fun deFecha(v: LocalDate?): Long? = v?.toEpochDay()
    @TypeConverter fun aFecha(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)

    @TypeConverter fun deHora(v: LocalTime?): Int? = v?.toSecondOfDay()
    @TypeConverter fun aHora(v: Int?): LocalTime? = v?.let { LocalTime.ofSecondOfDay(it.toLong()) }

    @TypeConverter fun deFechaHora(v: LocalDateTime?): String? = v?.toString()
    @TypeConverter fun aFechaHora(v: String?): LocalDateTime? = v?.let(LocalDateTime::parse)

    @TypeConverter fun deInstante(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun aInstante(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun deDias(v: Set<DayOfWeek>?): Int? = v?.fold(0) { m, d -> m or (1 shl (d.value - 1)) }
    @TypeConverter fun aDias(v: Int?): Set<DayOfWeek>? = v?.let { m -> DayOfWeek.entries.filter { m and (1 shl (it.value - 1)) != 0 }.toSet() }

    @TypeConverter fun deTipo(v: ActivityKind?): String? = v?.name
    @TypeConverter fun aTipo(v: String?): ActivityKind? = v?.let(ActivityKind::valueOf)

    @TypeConverter fun deModo(v: TransportMode?): String? = v?.name
    @TypeConverter fun aModo(v: String?): TransportMode? = v?.let(TransportMode::valueOf)
}
