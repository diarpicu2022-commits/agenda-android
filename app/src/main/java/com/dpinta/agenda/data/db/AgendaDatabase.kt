package com.dpinta.agenda.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.dpinta.agenda.domain.TransportMode
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Acceso a la agenda. Las escrituras compuestas van en @Transaction: o todo o nada. */
@Dao
abstract class AgendaDao {

    // ---------- lectura (observable) ----------
    @Query("SELECT * FROM actividad ORDER BY id") abstract fun actividades(): Flow<List<ActividadEntidad>>
    @Query("SELECT * FROM lugar ORDER BY nombre") abstract fun lugares(): Flow<List<LugarEntidad>>
    @Query("SELECT * FROM serie_semanal ORDER BY id") abstract fun series(): Flow<List<SerieEntidad>>
    @Query("SELECT * FROM sesion_puntual ORDER BY inicio") abstract fun puntuales(): Flow<List<SesionPuntualEntidad>>
    @Query("SELECT * FROM excepcion ORDER BY fecha") abstract fun excepciones(): Flow<List<ExcepcionEntidad>>
    @Query("SELECT * FROM semestre ORDER BY inicio") abstract fun semestres(): Flow<List<SemestreEntidad>>
    @Query("SELECT * FROM dia_sin_clase ORDER BY fecha") abstract fun diasSinClase(): Flow<List<DiaSinClaseEntidad>>
    @Query("SELECT * FROM traslado") abstract fun traslados(): Flow<List<TrasladoEntidad>>

    // ---------- lugares ----------
    @Query("SELECT * FROM lugar WHERE nombre_clave = :clave LIMIT 1") abstract suspend fun lugarPorClave(clave: String): LugarEntidad?
    @Insert abstract suspend fun insertarLugar(lugar: LugarEntidad): Long

    // ---------- actividades ----------
    @Upsert abstract suspend fun guardarActividadFila(actividad: ActividadEntidad): Long
    @Query("SELECT * FROM actividad WHERE id = :id") abstract suspend fun actividad(id: Long): ActividadEntidad?
    @Query("DELETE FROM actividad WHERE id = :id") abstract suspend fun eliminarActividad(id: Long): Int
    @Query("DELETE FROM serie_semanal WHERE actividad_id = :actividadId") abstract suspend fun borrarSeries(actividadId: Long)
    @Query("DELETE FROM sesion_puntual WHERE actividad_id = :actividadId") abstract suspend fun borrarPuntuales(actividadId: Long)
    @Insert abstract suspend fun insertarSerie(serie: SerieEntidad): Long
    @Insert abstract suspend fun insertarPuntual(sesion: SesionPuntualEntidad): Long

    /**
     * Crea o sustituye una actividad con su serie o su sesión puntual. El lugar se reutiliza si
     * ya existe uno con la misma clave; si no, se crea. Devuelve el id de la actividad.
     */
    @Transaction
    open suspend fun guardarActividad(
        actividad: ActividadEntidad,
        lugarNombre: String,
        serie: SerieEntidad?,
        puntual: SesionPuntualEntidad?,
    ): Long {
        val clave = claveDeLugar(lugarNombre)
        val lugarId = if (clave.isEmpty()) {
            null
        } else {
            lugarPorClave(clave)?.id ?: insertarLugar(LugarEntidad(nombre = lugarNombre.trim().replace(Regex("\\s+"), " "), nombreClave = clave))
        }
        val fila = actividad.copy(lugarId = lugarId)
        val devuelto = guardarActividadFila(fila)
        // @Upsert devuelve -1 cuando actualiza: el id es el que ya traía.
        val id = if (fila.id != 0L) fila.id else devuelto
        borrarSeries(id)
        borrarPuntuales(id)
        serie?.let { insertarSerie(it.copy(id = 0, actividadId = id)) }
        puntual?.let { insertarPuntual(it.copy(id = 0, actividadId = id)) }
        return id
    }

    // ---------- excepciones ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun guardarExcepcion(e: ExcepcionEntidad): Long
    @Query("DELETE FROM excepcion WHERE actividad_id = :actividadId AND fecha = :fecha") abstract suspend fun quitarExcepcion(actividadId: Long, fecha: LocalDate): Int

    // ---------- semestres ----------
    @Upsert abstract suspend fun guardarSemestreFila(s: SemestreEntidad): Long
    @Query("DELETE FROM dia_sin_clase WHERE semestre_id = :semestreId") abstract suspend fun borrarDiasSinClase(semestreId: Long)
    @Insert abstract suspend fun insertarDiasSinClase(dias: List<DiaSinClaseEntidad>)
    @Delete abstract suspend fun eliminarSemestre(s: SemestreEntidad)
    @Query("DELETE FROM semestre WHERE id = :id") abstract suspend fun eliminarSemestre(id: Long)

    @Transaction
    open suspend fun guardarSemestre(s: SemestreEntidad, dias: Map<LocalDate, String>): Long {
        val devuelto = guardarSemestreFila(s)
        val id = if (s.id != 0L) s.id else devuelto
        borrarDiasSinClase(id)
        insertarDiasSinClase(dias.map { (fecha, motivo) -> DiaSinClaseEntidad(id, fecha, motivo) })
        return id
    }

    // ---------- trayectos y traslados ----------
    @Upsert abstract suspend fun guardarTrayecto(t: TrayectoEntidad)
    @Query("SELECT * FROM trayecto WHERE lugar_id = :lugarId AND modo = :modo") abstract suspend fun trayecto(lugarId: Long, modo: TransportMode): TrayectoEntidad?
    @Upsert abstract suspend fun guardarTraslado(t: TrasladoEntidad)

    companion object {
        /** Clave única de un lugar: minúsculas y espacios normalizados («  Campus  Central » → «campus central»). */
        fun claveDeLugar(nombre: String): String = nombre.trim().replace(Regex("\\s+"), " ").lowercase()
    }
}

@Database(
    entities = [
        LugarEntidad::class,
        ActividadEntidad::class,
        SerieEntidad::class,
        SesionPuntualEntidad::class,
        ExcepcionEntidad::class,
        SemestreEntidad::class,
        DiaSinClaseEntidad::class,
        TrayectoEntidad::class,
        TrasladoEntidad::class,
    ],
    version = AgendaDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class AgendaDatabase : RoomDatabase() {
    abstract fun agendaDao(): AgendaDao

    companion object {
        const val VERSION = 2
        const val NOMBRE = "agenda.db"

        /** v2: tiempos de trayecto escritos a mano (sin Routes API). */
        val DE_1_A_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trayecto ADD COLUMN manual INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Migraciones no destructivas, en orden. */
        val MIGRACIONES: Array<androidx.room.migration.Migration> = arrayOf(DE_1_A_2)
    }
}
