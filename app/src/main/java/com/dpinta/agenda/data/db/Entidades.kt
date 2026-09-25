package com.dpinta.agenda.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.TransportMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/*
 * Esquema v1 de la agenda (arquitectura §3). Todo vive cifrado con SQLCipher.
 * A partir de aquí, cada cambio de esquema sube la versión con una Migration no destructiva
 * y su prueba (MigracionesTest, androidTest).
 */

/** Lugar del mapa. [nombreClave] (nombre en minúsculas y sin espacios de más) es único: evita duplicados. */
@Entity(tableName = "lugar", indices = [Index(value = ["nombre_clave"], unique = true)])
data class LugarEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    @ColumnInfo(name = "nombre_clave") val nombreClave: String,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    @ColumnInfo(name = "google_place_id") val googlePlaceId: String? = null,
)

@Entity(
    tableName = "actividad",
    foreignKeys = [ForeignKey(LugarEntidad::class, ["id"], ["lugar_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("lugar_id")],
)
data class ActividadEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val tipo: ActivityKind,
    val salon: String,
    @ColumnInfo(name = "lugar_id") val lugarId: Long?,
    @ColumnInfo(name = "margen_min") val margenMin: Int,
    val modo: TransportMode,
    @ColumnInfo(name = "aviso_min") val avisoMin: Int,
)

/** Serie semanal: mismos días y horas cada semana entre [desde] y [hasta], en la zona [zona]. */
@Entity(
    tableName = "serie_semanal",
    foreignKeys = [ForeignKey(ActividadEntidad::class, ["id"], ["actividad_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("actividad_id")],
)
data class SerieEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "actividad_id") val actividadId: Long,
    val dias: Set<DayOfWeek>,
    val inicio: LocalTime,
    val fin: LocalTime,
    val desde: LocalDate,
    val hasta: LocalDate,
    val zona: String,
)

@Entity(
    tableName = "sesion_puntual",
    foreignKeys = [ForeignKey(ActividadEntidad::class, ["id"], ["actividad_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("actividad_id")],
)
data class SesionPuntualEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "actividad_id") val actividadId: Long,
    val inicio: LocalDateTime,
    val fin: LocalDateTime,
)

/** Cambio sobre una sola sesión de una serie: «hoy no hay Cálculo» (CANCELADA) o «esta semana a las 10» (MOVIDA). */
@Entity(
    tableName = "excepcion",
    foreignKeys = [ForeignKey(ActividadEntidad::class, ["id"], ["actividad_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["actividad_id", "fecha"], unique = true)],
)
data class ExcepcionEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "actividad_id") val actividadId: Long,
    val fecha: LocalDate,
    val tipo: String,
    @ColumnInfo(name = "nuevo_inicio") val nuevoInicio: LocalDateTime? = null,
    @ColumnInfo(name = "nuevo_fin") val nuevoFin: LocalDateTime? = null,
) {
    companion object {
        const val CANCELADA = "CANCELADA"
        const val MOVIDA = "MOVIDA"
    }
}

@Entity(tableName = "semestre")
data class SemestreEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val inicio: LocalDate,
    val fin: LocalDate,
)

/** Festivo o día sin clase de un semestre (los festivos de Colombia se proponen con ColombianHolidays). */
@Entity(
    tableName = "dia_sin_clase",
    primaryKeys = ["semestre_id", "fecha"],
    foreignKeys = [ForeignKey(SemestreEntidad::class, ["id"], ["semestre_id"], onDelete = ForeignKey.CASCADE)],
)
data class DiaSinClaseEntidad(
    @ColumnInfo(name = "semestre_id") val semestreId: Long,
    val fecha: LocalDate,
    val motivo: String,
)

/** Última duración conocida desde la posición de Diego hasta un lugar, por modo (arquitectura §4.5). */
@Entity(
    tableName = "trayecto",
    primaryKeys = ["lugar_id", "modo"],
    foreignKeys = [ForeignKey(LugarEntidad::class, ["id"], ["lugar_id"], onDelete = ForeignKey.CASCADE)],
)
data class TrayectoEntidad(
    @ColumnInfo(name = "lugar_id") val lugarId: Long,
    val modo: TransportMode,
    val minutos: Int,
    @ColumnInfo(name = "calculado_en") val calculadoEn: Instant,
    /** Tiempo escrito por Diego, no calculado (v2). */
    @ColumnInfo(defaultValue = "0") val manual: Boolean = false,
)

/** Minutos de traslado entre dos lugares, para detectar traslados imposibles (conflictos, C10). */
@Entity(
    tableName = "traslado",
    primaryKeys = ["desde_id", "hasta_id"],
    foreignKeys = [
        ForeignKey(LugarEntidad::class, ["id"], ["desde_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(LugarEntidad::class, ["id"], ["hasta_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("hasta_id")],
)
data class TrasladoEntidad(
    @ColumnInfo(name = "desde_id") val desdeId: Long,
    @ColumnInfo(name = "hasta_id") val hastaId: Long,
    val minutos: Int,
    @ColumnInfo(name = "calculado_en") val calculadoEn: Instant,
)
